package com.example.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuotationLineItem
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.util.InvoicePrintManager
import kotlinx.coroutines.launch

@Composable
fun ThermalPrintDialog(
    title: String,
    refNo: String,
    date: String,
    customerName: String,
    items: List<QuotationLineItem>,
    subtotal: Double,
    discount: Double,
    grandTotal: Double,
    storeName: String = "HARDWARE & TOOLS STORE",
    storePhone: String = "",
    onDismiss: () -> Unit,
    onPrintSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var is80mm by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            pairedDevices = InvoicePrintManager.getPairedBluetoothPrinters()
            selectedDevice = pairedDevices.firstOrNull()
            if (pairedDevices.isEmpty()) {
                statusMessage = "No paired Bluetooth printers found. Please pair your printer in Android Bluetooth Settings first."
            }
        } catch (e: Exception) {
            statusMessage = "Bluetooth error: ${e.message}"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isPrinting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Print,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Bluetooth Thermal Print", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select your thermal printer and paper size:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Paper width toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !is80mm,
                        onClick = { is80mm = false },
                        label = { Text("58mm (Compact)") }
                    )
                    FilterChip(
                        selected = is80mm,
                        onClick = { is80mm = true },
                        label = { Text("80mm (Standard)") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PAIRED PRINTERS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (pairedDevices.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "No paired Bluetooth devices detected.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Turn ON your thermal printer\n2. Open Phone Settings > Bluetooth > Pair printer\n3. Re-open this dialog",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        items(pairedDevices) { device ->
                            @SuppressLint("MissingPermission")
                            val name = try { device.name ?: "Unknown Device" } catch (_: Exception) { "Unknown Device" }
                            val isSelected = selectedDevice == device

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) BrandBlue.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { selectedDevice = device }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isSelected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = device.address,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = statusMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val device = selectedDevice ?: return@Button
                    isPrinting = true
                    statusMessage = null
                    coroutineScope.launch {
                        try {
                            val data = InvoicePrintManager.generateEscPosReceipt(
                                storeName = storeName,
                                storePhone = storePhone,
                                title = title,
                                refNo = refNo,
                                date = date,
                                customerName = customerName,
                                items = items,
                                subtotal = subtotal,
                                discount = discount,
                                grandTotal = grandTotal,
                                is80mm = is80mm
                            )
                            val res = InvoicePrintManager.printViaBluetooth(device, data)
                            if (res.isSuccess) {
                                onPrintSuccess()
                                onDismiss()
                            } else {
                                statusMessage = "Print failed: ${res.exceptionOrNull()?.message}"
                            }
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.message}"
                        } finally {
                            isPrinting = false
                        }
                    }
                },
                enabled = selectedDevice != null && !isPrinting,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Printing…")
                } else {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Slip")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPrinting) {
                Text("Cancel")
            }
        }
    )
}
