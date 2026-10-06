package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Item
import com.example.data.model.QuotationLineItem
import com.example.data.model.QuotationRecord
import com.example.ui.StockViewModel
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.util.InvoicePrintManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun QuotationScreen(viewModel: StockViewModel) {
    val context = LocalContext.current
    val quotations by viewModel.allQuotations.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    var quotationToEdit by remember { mutableStateOf<QuotationRecord?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var thermalPrintTarget by remember { mutableStateOf<QuotationRecord?>(null) }
    var quotationToDelete by remember { mutableStateOf<QuotationRecord?>(null) }
    var quotationToConvert by remember { mutableStateOf<QuotationRecord?>(null) }

    val filtered = remember(quotations, searchQuery, selectedStatusFilter) {
        quotations.filter { q ->
            val matchQuery = searchQuery.isEmpty() ||
                    q.customerName.contains(searchQuery, ignoreCase = true) ||
                    q.quotationNo.contains(searchQuery, ignoreCase = true) ||
                    q.customerPhone.contains(searchQuery, ignoreCase = true)

            val matchStatus = if (selectedStatusFilter == "ALL") true else q.status.equals(selectedStatusFilter, ignoreCase = true)
            matchQuery && matchStatus
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    quotationToEdit = null
                    showFormDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Estimate") },
                containerColor = BrandBlue,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar & Search
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimates & Quotations",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${quotations.size} total quotes created",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by customer, quote #, phone…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("ALL", "Draft", "Sent", "Accepted", "Converted")
                    items(filters) { f ->
                        FilterChip(
                            selected = selectedStatusFilter == f,
                            onClick = { selectedStatusFilter = f },
                            label = { Text(f) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (quotations.isEmpty()) "No estimates created yet.\nTap '+ New Estimate' to create one." else "No matching estimates found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { quote ->
                        QuotationCard(
                            quotation = quote,
                            onEdit = {
                                quotationToEdit = quote
                                showFormDialog = true
                            },
                            onDelete = { quotationToDelete = quote },
                            onConvertToSale = { quotationToConvert = quote },
                            onPrintPdf = {
                                val pdf = InvoicePrintManager.createQuotationPdf(context, quote)
                                InvoicePrintManager.printPdf(context, pdf, "Quote_${quote.quotationNo}")
                            },
                            onSharePdf = {
                                val pdf = InvoicePrintManager.createQuotationPdf(context, quote)
                                InvoicePrintManager.sharePdf(context, pdf, "Estimate #${quote.quotationNo}")
                            },
                            onThermalPrint = { thermalPrintTarget = quote }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Form Dialog
    if (showFormDialog) {
        QuotationFormDialog(
            existingQuotation = quotationToEdit,
            availableItems = allItems,
            onDismiss = { showFormDialog = false },
            onSave = { record ->
                viewModel.saveQuotation(record)
                showFormDialog = false
            }
        )
    }

    // Thermal Print Dialog
    if (thermalPrintTarget != null) {
        val q = thermalPrintTarget!!
        val lineItems = InvoicePrintManager.parseLineItems(q.itemsJson)
        ThermalPrintDialog(
            title = "ESTIMATE / QUOTE",
            refNo = q.quotationNo,
            date = q.date,
            customerName = q.customerName,
            items = lineItems,
            subtotal = q.subtotal,
            discount = q.discount,
            grandTotal = q.grandTotal,
            onDismiss = { thermalPrintTarget = null },
            onPrintSuccess = {
                viewModel.showToast("Receipt printed successfully!")
                thermalPrintTarget = null
            }
        )
    }

    // Delete Confirmation
    if (quotationToDelete != null) {
        AlertDialog(
            onDismissRequest = { quotationToDelete = null },
            title = { Text("Delete Estimate") },
            text = { Text("Are you sure you want to delete Estimate #${quotationToDelete!!.quotationNo} for ${quotationToDelete!!.customerName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQuotation(quotationToDelete!!.id)
                        quotationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { quotationToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Convert to Sale Confirmation
    if (quotationToConvert != null) {
        val q = quotationToConvert!!
        AlertDialog(
            onDismissRequest = { quotationToConvert = null },
            title = { Text("Convert to Confirmed Sale?") },
            text = {
                Text("This will deduct all ${InvoicePrintManager.parseLineItems(q.itemsJson).size} item(s) from current stock and log Stock OUT transactions automatically.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.convertQuotationToSale(q)
                        quotationToConvert = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Confirm & Deduct Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { quotationToConvert = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun QuotationCard(
    quotation: QuotationRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConvertToSale: () -> Unit,
    onPrintPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onThermalPrint: () -> Unit
) {
    val items = remember(quotation.itemsJson) { InvoicePrintManager.parseLineItems(quotation.itemsJson) }

    val statusColor = when (quotation.status) {
        "Converted" -> Color(0xFF9333EA)
        "Accepted" -> Color(0xFF16A34A)
        "Sent" -> BrandBlue
        else -> BrandAmber
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Quote # & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#${quotation.quotationNo}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = quotation.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = quotation.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Info & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quotation.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (quotation.customerPhone.isNotEmpty()) {
                        Text(
                            text = "📞 ${quotation.customerPhone}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${items.size} items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Grand Total",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "₹%.2f", quotation.grandTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onPrintPdf, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "Print A4 PDF", tint = BrandBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onThermalPrint, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Thermal Print", tint = BrandAmber, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onSharePdf, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }

                if (quotation.status != "Converted") {
                    Button(
                        onClick = onConvertToSale,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Convert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationFormDialog(
    existingQuotation: QuotationRecord?,
    availableItems: List<Item>,
    onDismiss: () -> Unit,
    onSave: (QuotationRecord) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val nextQuoteNo = remember { "EST-" + (1000..9999).random().toString() }

    var customerName by remember { mutableStateOf(existingQuotation?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(existingQuotation?.customerPhone ?: "") }
    var quoteNo by remember { mutableStateOf(existingQuotation?.quotationNo ?: nextQuoteNo) }
    var date by remember { mutableStateOf(existingQuotation?.date ?: todayStr) }
    var validUntil by remember { mutableStateOf(existingQuotation?.validUntil ?: "") }
    var status by remember { mutableStateOf(existingQuotation?.status ?: "Draft") }
    var discountText by remember { mutableStateOf(existingQuotation?.discount?.toString() ?: "0") }
    var taxPercentText by remember { mutableStateOf(existingQuotation?.taxPercent?.toString() ?: "0") }
    var notes by remember { mutableStateOf(existingQuotation?.notes ?: "") }
    var lastSelectedType by remember { mutableStateOf("") }
    var voiceTargetIdx by remember { mutableStateOf<Int?>(null) }

    val lineItems = remember {
        mutableStateListOf<QuotationLineItem>().apply {
            if (existingQuotation != null) {
                val parsed = InvoicePrintManager.parseLineItems(existingQuotation.itemsJson)
                addAll(parsed)
                if (parsed.isNotEmpty()) {
                    lastSelectedType = parsed.last().type
                }
            } else {
                add(QuotationLineItem(name = "", qty = 1.0, unitPrice = 0.0, total = 0.0, type = ""))
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val normalized = com.example.util.ItemSearchMatcher.normalizeVoiceInput(spoken)
                val target = voiceTargetIdx
                if (target != null && target in lineItems.indices) {
                    lineItems[target] = lineItems[target].copy(name = normalized)
                }
            }
        }
    }

    val distinctTypes = remember(availableItems) {
        val defaults = listOf("Plumbing", "Electrical", "Hardware", "Sanitary", "Paints", "Tools")
        val fromDb = availableItems.map { it.type.trim() }.filter { it.isNotEmpty() }
        listOf("All") + (defaults + fromDb).distinct().sorted()
    }

    val subtotal = lineItems.sumOf { it.total }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val taxPercent = taxPercentText.toDoubleOrNull() ?: 0.0
    val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount = (afterDiscount * taxPercent) / 100.0
    val grandTotal = afterDiscount + taxAmount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingQuotation == null) "New Estimate / Quote" else "Edit Estimate #${existingQuotation.quotationNo}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Customer info
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Name *") },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quoteNo,
                                onValueChange = { quoteNo = it },
                                label = { Text("Quote No.") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LINE ITEMS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            TextButton(
                                onClick = {
                                    val rowType = if (lineItems.isNotEmpty() && lineItems.last().type.isNotBlank()) lineItems.last().type else lastSelectedType
                                    lineItems.add(
                                        QuotationLineItem(
                                            name = "",
                                            qty = 1.0,
                                            unitPrice = 0.0,
                                            total = 0.0,
                                            type = rowType
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Item")
                            }
                        }
                    }

                    // Line items list
                    items(lineItems.size) { idx ->
                        val item = lineItems[idx]
                        var isSuggestionsOpen by remember { mutableStateOf(false) }

                        val matchingItems = remember(item.name, item.type, availableItems) {
                            val q = item.name.trim()
                            val curType = item.type.trim()
                            if (q.isBlank() && curType.isBlank()) {
                                emptyList()
                            } else {
                                availableItems.mapNotNull { dbItem ->
                                    val typeMatch = curType.isBlank() || curType.equals("All", ignoreCase = true) ||
                                            dbItem.type.trim().equals(curType, ignoreCase = true)
                                    if (!typeMatch) return@mapNotNull null

                                    val score = if (q.isBlank()) 1 else com.example.util.ItemSearchMatcher.matchScore(dbItem, q)
                                    if (score > 0) Pair(dbItem, score) else null
                                }.sortedByDescending { it.second }
                                .map { it.first }
                                .take(25)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Item Name Field with instant autocomplete & Voice Search
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = item.name,
                                        onValueChange = { name ->
                                            lineItems[idx] = item.copy(name = name)
                                            isSuggestionsOpen = true
                                        },
                                        label = { Text("Item Name (type or speak to search)") },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        voiceTargetIdx = idx
                                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak item name (e.g. Socket, Pipe, Elbow)...")
                                                        }
                                                        try {
                                                            speechLauncher.launch(intent)
                                                        } catch (e: Exception) {
                                                            // voice not supported
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = BrandBlue)
                                                }
                                                if (lineItems.size > 1) {
                                                    IconButton(
                                                        onClick = { lineItems.removeAt(idx) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                // Suggestions popup list
                                if (isSuggestionsOpen && matchingItems.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        shadowElevation = 4.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    ) {
                                        Column {
                                            matchingItems.forEach { matched ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val curQty = if (item.qty <= 0.0) 1.0 else item.qty
                                                            val displayName = if (matched.size.isNotBlank() && !matched.name.contains(matched.size, ignoreCase = true)) {
                                                                "${matched.name} ${matched.size}"
                                                            } else {
                                                                matched.name
                                                            }
                                                            val updated = item.copy(
                                                                itemId = matched.id,
                                                                name = displayName,
                                                                code = matched.code,
                                                                type = if (matched.type.isNotBlank()) matched.type else item.type,
                                                                unit = matched.unit.ifBlank { "pcs" },
                                                                unitPrice = matched.price,
                                                                total = curQty * matched.price
                                                            )
                                                            lineItems[idx] = updated
                                                            if (matched.type.isNotBlank()) {
                                                                lastSelectedType = matched.type
                                                            }
                                                            isSuggestionsOpen = false
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(matched.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                            if (matched.size.isNotBlank()) {
                                                                Surface(
                                                                    color = BrandBlue.copy(alpha = 0.14f),
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f))
                                                                ) {
                                                                    Text(
                                                                        text = matched.size,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        color = BrandBlue,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            if (matched.type.isNotBlank()) {
                                                                Surface(
                                                                    color = BrandBlue.copy(alpha = 0.12f),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = matched.type,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = BrandBlue,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                            Text("📦 Stock: ${matched.qty} ${matched.unit}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Text(
                                                        String.format(Locale.US, "₹%.2f", matched.price),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = BrandBlue
                                                    )
                                                }
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Type Selection Row beneath Item Name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Type: ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(distinctTypes) { tOption ->
                                            val isSelected = if (tOption.equals("All", ignoreCase = true)) {
                                                item.type.trim().isEmpty() || item.type.equals("All", ignoreCase = true)
                                            } else {
                                                item.type.trim().equals(tOption.trim(), ignoreCase = true)
                                            }
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    val newType = if (tOption.equals("All", ignoreCase = true)) "" else tOption
                                                    lineItems[idx] = item.copy(type = newType)
                                                    lastSelectedType = newType
                                                    isSuggestionsOpen = true
                                                },
                                                label = {
                                                    Text(
                                                        text = tOption,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                                    selectedContainerColor = BrandBlue,
                                                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Qty, Unit, Unit Price (Rate), and Total Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = if (item.qty == 0.0) "" else item.qty.toString(),
                                        onValueChange = { qStr ->
                                            val qVal = qStr.toDoubleOrNull() ?: 0.0
                                            val tot = qVal * item.unitPrice
                                            lineItems[idx] = item.copy(qty = qVal, total = tot)
                                        },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    // Unit with Quick Multi-Unit Toggle
                                    Column(modifier = Modifier.weight(1.1f)) {
                                        OutlinedTextField(
                                            value = item.unit,
                                            onValueChange = { u -> lineItems[idx] = item.copy(unit = u) },
                                            label = { Text("Unit") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        // Unit Conversion Quick Pill
                                        val paired = com.example.util.UnitConversionHelper.getPairedUnit(item.unit)
                                        if (paired.first != item.unit) {
                                            Text(
                                                text = "⇄ ${paired.first.uppercase()}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandBlue,
                                                modifier = Modifier
                                                    .clickable {
                                                        val newUnit = paired.first
                                                        val convertedQty = com.example.util.UnitConversionHelper.convertQuantity(item.unit, newUnit, item.qty, paired.second)
                                                        lineItems[idx] = item.copy(unit = newUnit, qty = convertedQty, total = convertedQty * item.unitPrice)
                                                    }
                                                    .padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = if (item.unitPrice == 0.0) "" else item.unitPrice.toString(),
                                        onValueChange = { rStr ->
                                            val rVal = rStr.toDoubleOrNull() ?: 0.0
                                            val tot = item.qty * rVal
                                            lineItems[idx] = item.copy(unitPrice = rVal, total = tot)
                                        },
                                        label = { Text("Rate (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1.1f),
                                        singleLine = true
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Total", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(String.format(Locale.US, "₹%.2f", item.total), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { discountText = it },
                                label = { Text("Discount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = taxPercentText,
                                onValueChange = { taxPercentText = it },
                                label = { Text("GST/Tax (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Payment Terms") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                }

                // Summary & Save Footer with proper generous padding
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Subtotal: ₹${String.format(Locale.US, "%.2f", subtotal)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (taxAmount > 0) Text("Tax ($taxPercent%): +₹${String.format(Locale.US, "%.2f", taxAmount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Grand Total: ₹${String.format(Locale.US, "%.2f", grandTotal)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Button(
                            onClick = {
                                if (customerName.isBlank()) return@Button
                                val validItems = lineItems.filter { it.name.isNotBlank() }
                                if (validItems.isEmpty()) return@Button

                                val record = QuotationRecord(
                                    id = existingQuotation?.id ?: UUID.randomUUID().toString(),
                                    quotationNo = quoteNo.ifEmpty { nextQuoteNo },
                                    customerName = customerName.trim(),
                                    customerPhone = customerPhone.trim(),
                                    date = date,
                                    validUntil = validUntil,
                                    itemsJson = InvoicePrintManager.toJson(validItems),
                                    subtotal = subtotal,
                                    discount = discount,
                                    taxPercent = taxPercent,
                                    taxAmount = taxAmount,
                                    grandTotal = grandTotal,
                                    status = status,
                                    notes = notes.trim(),
                                    createdAt = existingQuotation?.createdAt ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                                )
                                onSave(record)
                            },
                            enabled = customerName.isNotBlank() && lineItems.any { it.name.isNotBlank() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Estimate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
