package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Item
import com.example.data.repository.BillRowData
import com.example.ui.StockViewModel
import com.example.ui.components.CalculatorDialog
import com.example.ui.components.ValuePickerDialog
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillScreen(viewModel: StockViewModel) {
    val allItems by viewModel.allItems.collectAsState()

    var stage by remember { mutableStateOf("idle") } // "idle" or "review"
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showFullImage by remember { mutableStateOf(false) }

    var supplier by remember { mutableStateOf("") }
    var billNo by remember { mutableStateOf("") }
    var billDate by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
    }

    val rows = remember {
        mutableStateListOf(
            BillRowData(
                id = "row_0",
                name = "",
                rate = 0.0,
                qty = 1.0,
                unit = "pcs",
                include = true
            )
        )
    }

    var showCalculator by remember { mutableStateOf(false) }
    var showSequentialScanDialog by remember { mutableStateOf(false) }

    // Pick image from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            photoBitmap = null
        }
    }

    // Take photo with camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            photoBitmap = bitmap
            imageUri = null
        }
    }

    if (showCalculator) {
        CalculatorDialog(onDismiss = { showCalculator = false })
    }

    if (showFullImage && (imageUri != null || photoBitmap != null)) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap!!.asImageBitmap(),
                            contentDescription = "Full bill",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Full bill",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { showFullImage = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (stage == "review") {
                    stage = "idle"
                } else {
                    viewModel.navigateBack()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = if (stage == "review") "Enter items" else "Bill",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (stage == "review") {
                IconButton(onClick = { showCalculator = true }) {
                    Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = BrandBlue)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (stage == "idle") {
            // Idle Screen
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Quick Sequential Scan Banner for Receiving Shipments
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = BrandBlue, shape = CircleShape, modifier = Modifier.size(32.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("⚡", fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Quick Bill Entry (Sequential Scan)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue
                                    )
                                    Text(
                                        text = "Scan items sequentially when receiving shipments to auto-populate the bill.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showSequentialScanDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⚡ Start Sequential Shipment Scan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📷 Bill image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Take a photo or pick from gallery, then tap Continue. You'll enter items while viewing the photo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera")
                                }

                                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery")
                                }

                                if (imageUri != null || photoBitmap != null) {
                                    OutlinedButton(onClick = {
                                        imageUri = null
                                        photoBitmap = null
                                    }) {
                                        Text("Remove")
                                    }
                                }
                            }

                            if (photoBitmap != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Image(
                                    bitmap = photoBitmap!!.asImageBitmap(),
                                    contentDescription = "Bill photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (imageUri != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Bill photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            if (imageUri != null || photoBitmap != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { stage = "review" },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                ) {
                                    Text("Continue →")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Manual Entry without Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Skip the photo and enter rows or scan items directly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { stage = "review" }) {
                                Text("Enter items directly →")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        } else {
            // Review Stage
            val includedRows = rows.filter { it.include }
            val billTotal = includedRows.sumOf { it.rate * it.qty }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Image preview strip (if image chosen)
                if (photoBitmap != null || imageUri != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showFullImage = true }
                        ) {
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap!!.asImageBitmap(),
                                    contentDescription = "Bill thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Bill thumbnail",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text(
                                text = "Tap image to enlarge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Header Fields Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = supplier,
                                onValueChange = { supplier = it },
                                label = { Text("Supplier") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = billNo,
                                onValueChange = { billNo = it },
                                label = { Text("Bill no") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = billDate,
                                onValueChange = { billDate = it },
                                label = { Text("Bill date (dd/mm/yyyy)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set Type / Brand / Size first if you want to narrow the item list. Then tap item name to pick.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Row Items
                itemsIndexed(rows) { index, row ->
                    BillRowEditor(
                        index = index,
                        row = row,
                        allItems = allItems,
                        onUpdate = { updated -> rows[index] = updated },
                        onDelete = {
                            if (rows.size > 1) {
                                rows.removeAt(index)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Summary & Actions
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bill total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = StockViewModel.formatRupees(billTotal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showSequentialScanDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚡ Quick Scan")
                        }

                        OutlinedButton(
                            onClick = {
                                rows.add(
                                    BillRowData(
                                        id = "row_${System.currentTimeMillis()}",
                                        name = "",
                                        rate = 0.0,
                                        qty = 1.0,
                                        unit = "pcs",
                                        include = true
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Add row")
                        }

                        Button(
                            onClick = {
                                val namedRows = rows.filter { it.include && it.name.trim().isNotEmpty() }
                                if (namedRows.isEmpty()) {
                                    viewModel.showToast("Add at least one named row")
                                    return@Button
                                }
                                viewModel.confirmBill(supplier, billNo, billDate, rows)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("Confirm (${includedRows.size})")
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showSequentialScanDialog) {
        SequentialScanBillDialog(
            allItems = allItems,
            onDismiss = { showSequentialScanDialog = false },
            onItemsScanned = { scannedRows ->
                if (scannedRows.isNotEmpty()) {
                    // Replace empty initial row if it's untouched
                    if (rows.size == 1 && rows[0].name.isBlank()) {
                        rows.clear()
                    }
                    rows.addAll(scannedRows)
                    stage = "review"
                    viewModel.showToast("Added ${scannedRows.size} shipment items to bill ✓")
                }
                showSequentialScanDialog = false
            }
        )
    }
}

@Composable
fun SequentialScanBillDialog(
    allItems: List<Item>,
    onDismiss: () -> Unit,
    onItemsScanned: (List<BillRowData>) -> Unit
) {
    var scannedInput by remember { mutableStateOf("") }
    val scannedList = remember { mutableStateListOf<BillRowData>() }
    var showCameraScannerDialog by remember { mutableStateOf(false) }

    val barcodeSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                scannedInput = spoken
            }
        }
    }

    val cameraScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            // Simulated / visual barcode scan feedback
            val itemWithBarcode = allItems.firstOrNull { it.barcode.isNotBlank() } ?: allItems.firstOrNull()
            if (itemWithBarcode != null) {
                val codeToUse = if (itemWithBarcode.barcode.isNotBlank()) itemWithBarcode.barcode else itemWithBarcode.name
                val existingIdx = scannedList.indexOfFirst { it.name.equals(itemWithBarcode.name, ignoreCase = true) }
                if (existingIdx >= 0) {
                    val cur = scannedList[existingIdx]
                    scannedList[existingIdx] = cur.copy(qty = cur.qty + 1.0)
                } else {
                    scannedList.add(
                        BillRowData(
                            id = "scan_${System.currentTimeMillis()}_${scannedList.size}",
                            name = itemWithBarcode.name,
                            matchedItem = itemWithBarcode,
                            rate = itemWithBarcode.cost,
                            qty = 1.0,
                            type = itemWithBarcode.type,
                            brand = itemWithBarcode.brand,
                            size = itemWithBarcode.size,
                            unit = itemWithBarcode.unit.ifBlank { "pcs" },
                            include = true
                        )
                    )
                }
            }
        }
    }

    fun processBarcode(code: String) {
        val clean = code.trim()
        if (clean.isEmpty()) return

        val matched = allItems.firstOrNull {
            it.barcode.equals(clean, ignoreCase = true) ||
            it.code.equals(clean, ignoreCase = true) ||
            it.name.equals(clean, ignoreCase = true) ||
            it.name.contains(clean, ignoreCase = true) ||
            it.aliases.split(",").any { a -> a.trim().equals(clean, ignoreCase = true) }
        }

        if (matched != null) {
            val existingIdx = scannedList.indexOfFirst { it.name.equals(matched.name, ignoreCase = true) }
            if (existingIdx >= 0) {
                val current = scannedList[existingIdx]
                scannedList[existingIdx] = current.copy(qty = current.qty + 1.0)
            } else {
                scannedList.add(
                    BillRowData(
                        id = "scan_${System.currentTimeMillis()}_${scannedList.size}",
                        name = matched.name,
                        matchedItem = matched,
                        rate = matched.cost,
                        qty = 1.0,
                        type = matched.type,
                        brand = matched.brand,
                        size = matched.size,
                        unit = matched.unit.ifBlank { "pcs" },
                        include = true
                    )
                )
            }
        } else {
            // New uncatalogued item
            val existingIdx = scannedList.indexOfFirst { it.name.equals(clean, ignoreCase = true) }
            if (existingIdx >= 0) {
                val current = scannedList[existingIdx]
                scannedList[existingIdx] = current.copy(qty = current.qty + 1.0)
            } else {
                scannedList.add(
                    BillRowData(
                        id = "scan_${System.currentTimeMillis()}_${scannedList.size}",
                        name = clean,
                        matchedItem = null,
                        rate = 0.0,
                        qty = 1.0,
                        unit = "pcs",
                        include = true
                    )
                )
            }
        }
        scannedInput = ""
    }

    val itemsWithBarcodes = remember(allItems) {
        allItems.filter { it.barcode.isNotBlank() || it.code.isNotBlank() }.take(15)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = BrandBlue.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Shipment Barcode Scan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandBlue)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scan barcodes with camera or enter codes sequentially as you unpack shipments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Camera Scan & Voice Scan Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { cameraScanLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📷 Camera Scanner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak barcode, SKU, or item name...")
                            }
                            try {
                                barcodeSpeechLauncher.launch(intent)
                            } catch (e: Exception) {
                                // speech not supported
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🎤 Speak", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Barcode Catalog Tap Bar
                if (itemsWithBarcodes.isNotEmpty()) {
                    Text(
                        "QUICK-TAP BARCODES (FROM INVENTORY)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(itemsWithBarcodes) { itm ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    val codeToProcess = if (itm.barcode.isNotBlank()) itm.barcode else itm.code
                                    processBarcode(codeToProcess)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡ ", fontSize = 10.sp)
                                    Column {
                                        Text(itm.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(
                                            text = if (itm.barcode.isNotBlank()) "🏷️ ${itm.barcode}" else "SKU: ${itm.code}",
                                            fontSize = 9.sp,
                                            color = BrandBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Barcode / SKU Text Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = scannedInput,
                        onValueChange = { scannedInput = it },
                        label = { Text("Scan / Type Barcode or SKU") },
                        placeholder = { Text("e.g. 890123456789") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = { processBarcode(scannedInput) },
                        enabled = scannedInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("+ Add")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Scanned Items List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SCANNED ITEMS (${scannedList.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Total Qty: ${scannedList.sumOf { it.qty }.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (scannedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No items scanned yet.\nScan barcodes with camera or quick-tap items to populate the bill.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(scannedList) { sIdx, sRow ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sRow.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Rate: ₹${sRow.rate} • Unit: ${sRow.unit}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = BrandBlue.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Qty: ${sRow.qty.toInt()}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = BrandBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (sRow.qty > 1) {
                                                    scannedList[sIdx] = sRow.copy(qty = sRow.qty - 1)
                                                } else {
                                                    scannedList.removeAt(sIdx)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = DangerRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onItemsScanned(scannedList.toList()) },
                        enabled = scannedList.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("✓ Apply to Bill (${scannedList.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BillRowEditor(
    index: Int,
    row: BillRowData,
    allItems: List<Item>,
    onUpdate: (BillRowData) -> Unit,
    onDelete: () -> Unit
) {
    var showItemPicker by remember { mutableStateOf(false) }
    var pickerField by remember { mutableStateOf<String?>(null) } // "type", "brand", "size", "unit"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = row.include,
                    onCheckedChange = { onUpdate(row.copy(include = it)) },
                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Item #${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (row.name.isNotBlank()) row.name + (if (row.matchedItem == null) " (new)" else "") else "— tap to pick item —",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (row.name.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showItemPicker = true }
                            .padding(vertical = 4.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = DangerRed)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rate and Quantity + Multi-Unit Conversion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (row.rate > 0) row.rate.toString() else "",
                    onValueChange = {
                        val r = it.toDoubleOrNull() ?: 0.0
                        onUpdate(row.copy(rate = r))
                    },
                    label = { Text("Rate (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = if (row.qty > 0) row.qty.toString() else "",
                        onValueChange = {
                            val q = it.toDoubleOrNull() ?: 0.0
                            onUpdate(row.copy(qty = q))
                        },
                        label = { Text("Qty (${row.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Multi-Unit Conversion Pill
                    val paired = com.example.util.UnitConversionHelper.getPairedUnit(row.unit)
                    if (paired.first != row.unit) {
                        Text(
                            text = "⇄ Convert to ${paired.first.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            modifier = Modifier
                                .clickable {
                                    val newUnit = paired.first
                                    val convertedQty = com.example.util.UnitConversionHelper.convertQuantity(row.unit, newUnit, row.qty, paired.second)
                                    onUpdate(row.copy(unit = newUnit, qty = convertedQty))
                                }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Type, Brand, Size, Unit chips/buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { pickerField = "type" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.type.ifEmpty { "Type" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { pickerField = "brand" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.brand.ifEmpty { "Brand" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { pickerField = "size" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.size.ifEmpty { "Size" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { pickerField = "unit" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = row.unit.ifEmpty { "pcs" },
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Item Picker Dialog
    if (showItemPicker) {
        val filteredScope = remember(row, allItems) {
            allItems.filter { it ->
                if (row.type.isNotBlank() && it.type != row.type) false
                else if (row.brand.isNotBlank() && it.brand != row.brand) false
                else if (row.size.isNotBlank() && it.size != row.size) false
                else true
            }
        }
        val options = filteredScope.map { it.name to it.cost.toInt() }
        ValuePickerDialog(
            title = "Pick Item",
            optionsWithCount = options,
            currentValue = row.name,
            onSelect = { selectedName ->
                val matched = filteredScope.firstOrNull { it.name.equals(selectedName, ignoreCase = true) }
                if (matched != null) {
                    onUpdate(
                        row.copy(
                            name = matched.name,
                            matchedItem = matched,
                            rate = if (row.rate == 0.0) matched.cost else row.rate,
                            type = if (row.type.isEmpty()) matched.type else row.type,
                            brand = if (row.brand.isEmpty()) matched.brand else row.brand,
                            size = if (row.size.isEmpty()) matched.size else row.size,
                            unit = if (row.unit.isEmpty() || row.unit == "pcs") matched.unit else row.unit
                        )
                    )
                } else {
                    onUpdate(
                        row.copy(
                            name = selectedName,
                            matchedItem = null
                        )
                    )
                }
                showItemPicker = false
            },
            onDismiss = { showItemPicker = false }
        )
    }

    // Type / Brand / Size / Unit picker
    pickerField?.let { field ->
        val counts = remember(field, allItems) {
            when (field) {
                "type" -> allItems.map { it.type }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList()
                "brand" -> allItems.map { it.brand }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList()
                "size" -> allItems.map { it.size }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList()
                "unit" -> listOf("pcs", "kg", "g", "L", "ml", "box", "pack", "bag", "m", "ft", "cm", "set", "pair", "dozen", "roll", "sheet")
                    .map { Pair(it, allItems.count { item -> item.unit == it }) }
                else -> emptyList()
            }
        }

        ValuePickerDialog(
            title = "Select ${field.replaceFirstChar { it.uppercase() }}",
            optionsWithCount = counts,
            currentValue = when (field) {
                "type" -> row.type
                "brand" -> row.brand
                "size" -> row.size
                "unit" -> row.unit
                else -> ""
            },
            onSelect = { selectedVal ->
                when (field) {
                    "type" -> onUpdate(row.copy(type = selectedVal))
                    "brand" -> onUpdate(row.copy(brand = selectedVal))
                    "size" -> onUpdate(row.copy(size = selectedVal))
                    "unit" -> onUpdate(row.copy(unit = selectedVal.ifBlank { "pcs" }))
                }
                pickerField = null
            },
            onDismiss = { pickerField = null }
        )
    }
}
