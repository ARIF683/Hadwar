package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerEntry
import com.example.data.model.QuotationLineItem
import com.example.data.model.QuotationRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object InvoicePrintManager {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val lineItemListAdapter = moshi.adapter<List<QuotationLineItem>>(
        Types.newParameterizedType(List::class.java, QuotationLineItem::class.java)
    )

    fun parseLineItems(json: String): List<QuotationLineItem> {
        return try {
            lineItemListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toJson(items: List<QuotationLineItem>): String {
        return lineItemListAdapter.toJson(items)
    }

    /**
     * Generates a clean A4 PDF document for Quotations / Estimates
     */
    fun createQuotationPdf(
        context: Context,
        quotation: QuotationRecord,
        storeName: String = "HARDWARE & TOOLS STORE",
        storePhone: String = "",
        storeAddress: String = "Main Market"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(20, 24, 33)
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(100, 116, 139)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(255, 255, 255)
        }

        // Header Background Banner
        val bgPaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Deep Navy
        }
        canvas.drawRect(0f, 0f, 595f, 90f, bgPaint)

        // Store Title
        val storeTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        canvas.drawText(storeName.uppercase(), 36f, 42f, storeTitlePaint)
        val headerSubPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(203, 213, 225)
        }
        canvas.drawText("Phone: ${storePhone.ifEmpty { "N/A" }} | $storeAddress", 36f, 62f, headerSubPaint)

        // Document Type Badge
        val docTypePaint = Paint().apply {
            isAntiAlias = true
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(245, 158, 11) // Amber
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("ESTIMATE / QUOTE", 559f, 45f, docTypePaint)
        val docNumPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.WHITE
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("No: #${quotation.quotationNo}", 559f, 65f, docNumPaint)

        var y = 120f

        // Customer & Meta Info Card
        val cardPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
        }
        val strokePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(36f, y, 559f, y + 70f, 8f, 8f, cardPaint)
        canvas.drawRoundRect(36f, y, 559f, y + 70f, 8f, 8f, strokePaint)

        canvas.drawText("CUSTOMER DETAILS", 50f, y + 20f, boldPaint)
        canvas.drawText("Name: ${quotation.customerName}", 50f, y + 38f, textPaint)
        if (quotation.customerPhone.isNotEmpty()) {
            canvas.drawText("Phone: ${quotation.customerPhone}", 50f, y + 54f, textPaint)
        }

        canvas.drawText("ESTIMATE DETAILS", 340f, y + 20f, boldPaint)
        canvas.drawText("Date: ${quotation.date}", 340f, y + 38f, textPaint)
        if (quotation.validUntil.isNotEmpty()) {
            canvas.drawText("Valid Until: ${quotation.validUntil}", 340f, y + 54f, textPaint)
        }

        y += 90f

        // Table Header
        val tableHeadBg = Paint().apply { color = Color.rgb(51, 65, 85) }
        canvas.drawRoundRect(36f, y, 559f, y + 24f, 4f, 4f, tableHeadBg)

        canvas.drawText("#", 46f, y + 16f, headerPaint)
        canvas.drawText("ITEM DESCRIPTION", 75f, y + 16f, headerPaint)
        canvas.drawText("QTY", 320f, y + 16f, headerPaint)
        canvas.drawText("RATE", 380f, y + 16f, headerPaint)
        canvas.drawText("DISC %", 450f, y + 16f, headerPaint)
        val rightHeaderPaint = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL", 545f, y + 16f, rightHeaderPaint)

        y += 32f

        val items = parseLineItems(quotation.itemsJson)
        val linePaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            strokeWidth = 1f
        }
        val rightTextPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }

        items.forEachIndexed { index, item ->
            if (index % 2 == 1) {
                canvas.drawRect(36f, y - 10f, 559f, y + 12f, Paint().apply { color = Color.rgb(248, 250, 252) })
            }
            canvas.drawText("${index + 1}", 46f, y + 2f, subPaint)
            val nameDisplay = if (item.name.length > 35) item.name.take(32) + "…" else item.name
            canvas.drawText(nameDisplay, 75f, y + 2f, boldPaint)
            canvas.drawText(String.format(Locale.US, "%.1f %s", item.qty, item.unit), 320f, y + 2f, textPaint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", item.unitPrice), 380f, y + 2f, textPaint)
            canvas.drawText(if (item.discountPercent > 0) "${item.discountPercent}%" else "-", 450f, y + 2f, textPaint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", item.total), 545f, y + 2f, rightBoldPaint)

            canvas.drawLine(36f, y + 14f, 559f, y + 14f, linePaint)
            y += 24f
        }

        y += 15f

        // Totals Card on right
        val totalBoxX = 320f
        canvas.drawRoundRect(totalBoxX, y, 559f, y + 95f, 6f, 6f, cardPaint)
        canvas.drawRoundRect(totalBoxX, y, 559f, y + 95f, 6f, 6f, strokePaint)

        var ty = y + 20f
        canvas.drawText("Subtotal:", totalBoxX + 16f, ty, textPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", quotation.subtotal), 545f, ty, rightTextPaint)

        if (quotation.discount > 0) {
            ty += 18f
            canvas.drawText("Discount:", totalBoxX + 16f, ty, textPaint)
            canvas.drawText(String.format(Locale.US, "-₹%.2f", quotation.discount), 545f, ty, rightTextPaint)
        }

        if (quotation.taxAmount > 0) {
            ty += 18f
            canvas.drawText("GST/Tax (${quotation.taxPercent}%):", totalBoxX + 16f, ty, textPaint)
            canvas.drawText(String.format(Locale.US, "+₹%.2f", quotation.taxAmount), 545f, ty, rightTextPaint)
        }

        ty += 22f
        canvas.drawLine(totalBoxX + 10f, ty - 8f, 545f, ty - 8f, strokePaint)
        val grandTotalPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val grandTotalRight = Paint(grandTotalPaint).apply {
            textAlign = Paint.Align.RIGHT
            color = Color.rgb(2, 132, 199)
        }
        canvas.drawText("GRAND TOTAL:", totalBoxX + 16f, ty + 2f, grandTotalPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", quotation.grandTotal), 545f, ty + 2f, grandTotalRight)

        // Notes & Terms on left
        if (quotation.notes.isNotEmpty()) {
            canvas.drawText("NOTES / TERMS:", 36f, y + 20f, boldPaint)
            canvas.drawText(quotation.notes.take(80), 36f, y + 38f, subPaint)
        }

        // Footer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            color = Color.rgb(148, 163, 184)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated via Hardware Stock Manager • Thank you for your business!", 595f / 2f, 810f, footerPaint)

        pdfDoc.finishPage(page)

        val outputDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val outputFile = File(outputDir, "Quotation_${quotation.quotationNo}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return outputFile
    }

    /**
     * Generates a clean A4 PDF for Ledger Statement
     */
    fun createLedgerStatementPdf(
        context: Context,
        account: LedgerAccount,
        entries: List<LedgerEntry>,
        storeName: String = "HARDWARE & TOOLS STORE"
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(30, 41, 59)
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = Color.rgb(100, 116, 139)
        }

        // Header Banner
        canvas.drawRect(0f, 0f, 595f, 85f, Paint().apply { color = Color.rgb(15, 23, 42) })
        canvas.drawText(storeName.uppercase(), 36f, 38f, Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        })
        canvas.drawText("ACCOUNT STATEMENT / KHATA LEDGER", 36f, 58f, Paint().apply {
            isAntiAlias = true
            textSize = 10f
            color = Color.rgb(245, 158, 11)
        })

        var y = 110f

        // Account Details Box
        canvas.drawRoundRect(36f, y, 559f, y + 60f, 6f, 6f, Paint().apply { color = Color.rgb(248, 250, 252) })
        canvas.drawText("ACCOUNT: ${account.name} (${account.type})", 50f, y + 22f, boldPaint)
        canvas.drawText("Phone: ${account.phone.ifEmpty { "N/A" }} | Address: ${account.address.ifEmpty { "N/A" }}", 50f, y + 42f, subPaint)

        val balanceColor = if (account.netBalance >= 0) Color.rgb(22, 163, 74) else Color.rgb(220, 38, 38)
        val balText = String.format(Locale.US, "NET BALANCE: ₹%.2f", Math.abs(account.netBalance))
        canvas.drawText(balText, 545f, y + 32f, Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = balanceColor
            textAlign = Paint.Align.RIGHT
        })

        y += 80f

        // Table Header
        canvas.drawRoundRect(36f, y, 559f, y + 24f, 4f, 4f, Paint().apply { color = Color.rgb(51, 65, 85) })
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        canvas.drawText("DATE", 46f, y + 16f, headerPaint)
        canvas.drawText("DETAILS / BILL REF", 130f, y + 16f, headerPaint)
        canvas.drawText("GAVE (DEBIT)", 330f, y + 16f, headerPaint)
        canvas.drawText("GOT (CREDIT)", 420f, y + 16f, headerPaint)
        canvas.drawText("BALANCE", 545f, y + 16f, Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT })

        y += 34f
        val linePaint = Paint().apply { color = Color.rgb(241, 245, 249); strokeWidth = 1f }
        val rightTextPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }

        entries.forEachIndexed { index, entry ->
            canvas.drawText(entry.date, 46f, y + 2f, subPaint)
            val desc = entry.description.ifEmpty { if (entry.billRef.isNotEmpty()) "Bill #${entry.billRef}" else "Payment" }
            canvas.drawText(desc.take(28), 130f, y + 2f, textPaint)

            if (entry.type == "GAVE") {
                canvas.drawText(String.format(Locale.US, "₹%.2f", entry.amount), 380f, y + 2f, Paint(rightTextPaint).apply { color = Color.rgb(220, 38, 38) })
                canvas.drawText("-", 450f, y + 2f, textPaint)
            } else {
                canvas.drawText("-", 350f, y + 2f, textPaint)
                canvas.drawText(String.format(Locale.US, "₹%.2f", entry.amount), 470f, y + 2f, Paint(rightTextPaint).apply { color = Color.rgb(22, 163, 74) })
            }

            canvas.drawText(String.format(Locale.US, "₹%.2f", entry.balanceAfter), 545f, y + 2f, rightTextPaint)

            canvas.drawLine(36f, y + 12f, 559f, y + 12f, linePaint)
            y += 22f
        }

        pdfDoc.finishPage(page)
        val outputDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val outputFile = File(outputDir, "Ledger_${account.name.replace(" ", "_")}.pdf")
        FileOutputStream(outputFile).use { out -> pdfDoc.writeTo(out) }
        pdfDoc.close()
        return outputFile
    }

    /**
     * Share PDF file via WhatsApp / System Share Sheet
     */
    fun sharePdf(context: Context, file: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Print PDF using Android System Print Manager
     */
    fun printPdf(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Generates ESC/POS thermal receipt raw bytes for 58mm (32 chars) or 80mm (48 chars) thermal printers
     */
    fun generateEscPosReceipt(
        storeName: String,
        storePhone: String,
        title: String,
        refNo: String,
        date: String,
        customerName: String,
        items: List<QuotationLineItem>,
        subtotal: Double,
        discount: Double,
        grandTotal: Double,
        is80mm: Boolean = false
    ): ByteArray {
        val maxChars = if (is80mm) 48 else 32
        val out = mutableListOf<Byte>()

        fun addBytes(vararg b: Int) {
            b.forEach { out.add(it.toByte()) }
        }

        fun addText(text: String) {
            text.toByteArray(charset("ISO-8859-1")).forEach { out.add(it) }
        }

        fun addLine(text: String = "") {
            addText(text + "\n")
        }

        fun alignCenter() = addBytes(0x1B, 0x61, 0x01)
        fun alignLeft() = addBytes(0x1B, 0x61, 0x00)
        fun alignRight() = addBytes(0x1B, 0x61, 0x02)
        fun boldOn() = addBytes(0x1B, 0x45, 0x01)
        fun boldOff() = addBytes(0x1B, 0x45, 0x00)
        fun doubleSize() = addBytes(0x1D, 0x21, 0x11)
        fun normalSize() = addBytes(0x1D, 0x21, 0x00)

        // Initialize printer
        addBytes(0x1B, 0x40)

        // Header
        alignCenter()
        boldOn()
        doubleSize()
        addLine(storeName)
        normalSize()
        if (storePhone.isNotEmpty()) addLine("Tel: $storePhone")
        addLine("--------------------------------".take(maxChars))
        boldOn()
        addLine(title.uppercase())
        boldOff()
        addLine("Ref: #$refNo  Date: $date")
        if (customerName.isNotEmpty()) addLine("Customer: $customerName")
        addLine("--------------------------------".take(maxChars))

        // Items Table
        alignLeft()
        val headerCol = if (is80mm) {
            String.format(Locale.US, "%-20s %6s %9s %9s", "Item", "Qty", "Rate", "Total")
        } else {
            String.format(Locale.US, "%-14s %4s %6s %6s", "Item", "Qty", "Rate", "Total")
        }
        addLine(headerCol)
        addLine("-".repeat(maxChars))

        items.forEach { item ->
            val nameTrunc = if (is80mm) item.name.take(20) else item.name.take(14)
            val line = if (is80mm) {
                String.format(Locale.US, "%-20s %6.1f %9.2f %9.2f", nameTrunc, item.qty, item.unitPrice, item.total)
            } else {
                String.format(Locale.US, "%-14s %4.1f %6.1f %6.1f", nameTrunc, item.qty, item.unitPrice, item.total)
            }
            addLine(line)
        }
        addLine("-".repeat(maxChars))

        // Totals
        alignRight()
        addLine(String.format(Locale.US, "Subtotal: Rs. %.2f", subtotal))
        if (discount > 0) {
            addLine(String.format(Locale.US, "Discount: -Rs. %.2f", discount))
        }
        boldOn()
        addLine(String.format(Locale.US, "GRAND TOTAL: Rs. %.2f", grandTotal))
        boldOff()
        addLine("--------------------------------".take(maxChars))

        // Footer
        alignCenter()
        addLine("Thank you! Visit Again.")
        addLine("\n\n\n")

        // Cut paper command
        addBytes(0x1D, 0x56, 0x41, 0x10)

        return out.toByteArray()
    }

    /**
     * Get paired Bluetooth devices list
     */
    @SuppressLint("MissingPermission")
    fun getPairedBluetoothPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Sends raw bytes to Bluetooth Thermal Printer over RFCOMM socket (SPP UUID)
     */
    @SuppressLint("MissingPermission")
    suspend fun printViaBluetooth(device: BluetoothDevice, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard Serial Port Profile UUID
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            val outputStream: OutputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()
            socket.close()
            Result.success(Unit)
        } catch (e: Exception) {
            try { socket?.close() } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
