package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerEntry
import com.example.ui.StockViewModel
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandBlue
import com.example.util.InvoicePrintManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun LedgerScreen(viewModel: StockViewModel) {
    val context = LocalContext.current
    val accounts by viewModel.allLedgerAccounts.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Customers/Contractors, 1 = Suppliers
    var searchQuery by remember { mutableStateOf("") }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedAccountForDetail by remember { mutableStateOf<LedgerAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<LedgerAccount?>(null) }

    // Quick transaction dialog state
    var quickEntryAccount by remember { mutableStateOf<LedgerAccount?>(null) }
    var quickEntryType by remember { mutableStateOf("GAVE") } // "GAVE" or "GOT"

    val currentType = if (selectedTab == 0) "CUSTOMER" else "SUPPLIER"
    val filteredAccounts = remember(accounts, selectedTab, searchQuery) {
        accounts.filter { acc ->
            val matchType = if (selectedTab == 0) acc.type != "SUPPLIER" else acc.type == "SUPPLIER"
            val matchQuery = searchQuery.isEmpty() || acc.name.contains(searchQuery, ignoreCase = true) || acc.phone.contains(searchQuery)
            matchType && matchQuery
        }
    }

    // Totals
    val totalReceivable = remember(accounts) {
        accounts.filter { it.type != "SUPPLIER" && it.netBalance > 0 }.sumOf { it.netBalance }
    }
    val totalPayable = remember(accounts) {
        accounts.filter { it.type == "SUPPLIER" && it.netBalance > 0 }.sumOf { it.netBalance }
    }

    val detailAccount = selectedAccountForDetail
    if (detailAccount != null) {
        // Account Detail View
        LedgerAccountDetailScreen(
            account = detailAccount,
            viewModel = viewModel,
            onBack = { selectedAccountForDetail = null },
            onAccountDeleted = { selectedAccountForDetail = null }
        )
        return
    }

    if (accountToDelete != null) {
        val target = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Khata Account?") },
            text = {
                Text(
                    "Are you sure you want to delete ${target.name} along with its entire transaction history from local storage and DB?\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLedgerAccount(target.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddAccountDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (selectedTab == 0) "Add Customer / Contractor" else "Add Supplier") },
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
            // Header Summary Cards
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Khata & Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track customer credit & supplier balances",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // You will get Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16A34A).copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("YOU WILL GET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "₹%.2f", totalReceivable),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }

                    // You will pay Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("YOU WILL PAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "₹%.2f", totalPayable),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Customers / Contractors (${accounts.count { it.type != "SUPPLIER" }})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Suppliers (${accounts.count { it.type == "SUPPLIER" }})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search account by name or phone…") },
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
            }

            if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (accounts.isEmpty()) "No accounts in Khata yet.\nTap below to add your first customer or supplier." else "No matching accounts found.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAccounts, key = { it.id }) { acc ->
                        LedgerAccountCard(
                            account = acc,
                            onClick = { selectedAccountForDetail = acc },
                            onGive = {
                                quickEntryAccount = acc
                                quickEntryType = "GAVE"
                            },
                            onGot = {
                                quickEntryAccount = acc
                                quickEntryType = "GOT"
                            },
                            onDelete = { accountToDelete = acc }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        AddAccountDialog(
            defaultType = if (selectedTab == 0) "CUSTOMER" else "SUPPLIER",
            onDismiss = { showAddAccountDialog = false },
            onSave = { acc ->
                viewModel.saveLedgerAccount(acc)
                showAddAccountDialog = false
            }
        )
    }

    // Quick Entry Dialog (Give / Got)
    if (quickEntryAccount != null) {
        val target = quickEntryAccount!!
        AddLedgerEntryDialog(
            account = target,
            initialType = quickEntryType,
            onDismiss = { quickEntryAccount = null },
            onSave = { type, amount, date, desc, ref ->
                viewModel.addLedgerEntry(target.id, type, amount, date, desc, ref)
                quickEntryAccount = null
            }
        )
    }

    // Delete Account Confirmation
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete ${accountToDelete!!.name}? All transaction history for this account will be erased.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLedgerAccount(accountToDelete!!.id)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LedgerAccountCard(
    account: LedgerAccount,
    onClick: () -> Unit,
    onGive: () -> Unit,
    onGot: () -> Unit,
    onDelete: () -> Unit
) {
    val isSupplier = account.type == "SUPPLIER"
    val isDue = account.netBalance > 0
    val balanceColor = if (isSupplier) {
        if (isDue) Color(0xFFDC2626) else Color(0xFF16A34A)
    } else {
        if (isDue) Color(0xFF16A34A) else Color(0xFFDC2626)
    }

    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .background(BrandBlue.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Text(
                            text = account.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = account.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (account.phone.isNotEmpty()) {
                            Text(
                                text = "📞 ${account.phone}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (account.notes.isNotEmpty()) {
                            Text(
                                text = "📝 ${account.notes}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        val statusLabel = if (isSupplier) {
                            if (account.netBalance > 0) "You Pay" else if (account.netBalance < 0) "Advance" else "Settled"
                        } else {
                            if (account.netBalance > 0) "You Get" else if (account.netBalance < 0) "Advance" else "Settled"
                        }
                        Text(text = statusLabel, fontSize = 10.sp, color = balanceColor, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format(Locale.US, "₹%.2f", Math.abs(account.netBalance)),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = balanceColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: GAVE / GOT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGive,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("- Gave (Debit)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onGot,
                    modifier = Modifier.weight(1f).height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Got (Credit)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LedgerAccountDetailScreen(
    account: LedgerAccount,
    viewModel: StockViewModel,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = onBack
) {
    val context = LocalContext.current
    val entriesFlow = remember(account.id) { viewModel.getEntriesForAccount(account.id) }
    val entries by entriesFlow.collectAsState(initial = emptyList())

    var showEntryDialog by remember { mutableStateOf(false) }
    var entryType by remember { mutableStateOf("GAVE") }
    var entryToEdit by remember { mutableStateOf<LedgerEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<LedgerEntry?>(null) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    val isSupplier = account.type == "SUPPLIER"
    val balanceColor = if (account.netBalance >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Delete Khata Account?") },
            text = {
                Text("Delete ${account.name} along with all ${entries.size} transaction records from local storage & DB? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLedgerAccount(account.id)
                        showDeleteAccountConfirm = false
                        onAccountDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (entryToDelete != null) {
        val target = entryToDelete!!
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Transaction Entry?") },
            text = {
                Text("Delete entry of ₹${target.amount} (${if (target.type == "GAVE") "Gave" else "Got"})? The account balance will be automatically recalculated.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLedgerEntry(target.id, account.id)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (entryToEdit != null) {
        EditLedgerEntryDialog(
            entry = entryToEdit!!,
            account = account,
            onDismiss = { entryToEdit = null },
            onSave = { updated ->
                viewModel.updateLedgerEntry(updated)
                entryToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(account.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (account.phone.isNotEmpty()) "📞 ${account.phone} • ${account.type}" else account.type,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val pdf = InvoicePrintManager.createLedgerStatementPdf(context, account, entries)
                            InvoicePrintManager.sharePdf(context, pdf, "Statement - ${account.name}")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Statement", tint = BrandBlue)
                    }
                    IconButton(
                        onClick = {
                            val pdf = InvoicePrintManager.createLedgerStatementPdf(context, account, entries)
                            InvoicePrintManager.printPdf(context, pdf, "Statement_${account.name}")
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showDeleteAccountConfirm = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Balance Banner & Notes
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = String.format(Locale.US, "₹%.2f", Math.abs(account.netBalance)),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = balanceColor
                            )
                        }

                        if (account.phone.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val msg = "Hello ${account.name}, your current ledger balance with Hardware Store is Rs. ${String.format(Locale.US, "%.2f", Math.abs(account.netBalance))}."
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?phone=${account.phone.replace("+", "").replace(" ", "")}&text=${Uri.encode(msg)}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp)
                            }
                        }
                    }

                    if (account.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📝 Note: ${account.notes}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Entries List
            Text(
                text = "TRANSACTION HISTORY (${entries.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (entry.type == "GAVE") Color(0xFFDC2626).copy(alpha = 0.15f) else Color(0xFF16A34A).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (entry.type == "GAVE") "Gave (-)" else "Got (+)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (entry.type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(entry.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (entry.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(entry.description, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    if (entry.billRef.isNotEmpty()) {
                                        Text("Bill #${entry.billRef}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "%s₹%.2f", if (entry.type == "GAVE") "-" else "+", entry.amount),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (entry.type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "Bal: ₹%.2f", entry.balanceAfter),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { entryToEdit = entry },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Entry", tint = BrandBlue, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { entryToDelete = entry },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Entry", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Buttons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            entryType = "GAVE"
                            showEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("- YOU GAVE (₹)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            entryType = "GOT"
                            showEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ YOU GOT (₹)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showEntryDialog) {
        AddLedgerEntryDialog(
            account = account,
            initialType = entryType,
            onDismiss = { showEntryDialog = false },
            onSave = { type, amount, date, desc, ref ->
                viewModel.addLedgerEntry(account.id, type, amount, date, desc, ref)
                showEntryDialog = false
            }
        )
    }
}

@Composable
fun EditLedgerEntryDialog(
    entry: LedgerEntry,
    account: LedgerAccount,
    onDismiss: () -> Unit,
    onSave: (LedgerEntry) -> Unit
) {
    var type by remember { mutableStateOf(entry.type) }
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var description by remember { mutableStateOf(entry.description) }
    var billRef by remember { mutableStateOf(entry.billRef) }
    var date by remember { mutableStateOf(entry.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Transaction Entry", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Account: ${account.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "GAVE",
                        onClick = { type = "GAVE" },
                        label = { Text("You Gave (-)") }
                    )
                    FilterChip(
                        selected = type == "GOT",
                        onClick = { type = "GOT" },
                        label = { Text("You Got (+)") }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billRef,
                        onValueChange = { billRef = it },
                        label = { Text("Bill # (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) return@Button
                    onSave(
                        entry.copy(
                            type = type,
                            amount = amt,
                            date = date,
                            description = description.trim(),
                            billRef = billRef.trim()
                        )
                    )
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddAccountDialog(
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (LedgerAccount) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var notes by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "SUPPLIER") "Add Supplier" else "Add Customer / Contractor") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / City") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "CUSTOMER",
                        onClick = { type = "CUSTOMER" },
                        label = { Text("Customer / Contractor") }
                    )
                    FilterChip(
                        selected = type == "SUPPLIER",
                        onClick = { type = "SUPPLIER" },
                        label = { Text("Supplier") }
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Description / Notes (e.g. GSTIN, site, remarks)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Opening Balance (₹) [Optional]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val initBal = initialBalanceText.toDoubleOrNull() ?: 0.0
                    val acc = LedgerAccount(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        type = type,
                        notes = notes.trim(),
                        netBalance = initBal,
                        createdAt = todayStr,
                        updatedAt = todayStr
                    )
                    onSave(acc)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddLedgerEntryDialog(
    account: LedgerAccount,
    initialType: String,
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, date: String, description: String, billRef: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) } // "GAVE" or "GOT"
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var billRef by remember { mutableStateOf("") }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(todayDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (type == "GAVE") "Record You Gave (Debit)" else "Record You Got (Credit)",
                color = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Account: ${account.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "GAVE",
                        onClick = { type = "GAVE" },
                        label = { Text("You Gave (-)") }
                    )
                    FilterChip(
                        selected = type == "GOT",
                        onClick = { type = "GOT" },
                        label = { Text("You Got (+)") }
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billRef,
                        onValueChange = { billRef = it },
                        label = { Text("Bill # (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) return@Button
                    onSave(type, amt, date, description.trim(), billRef.trim())
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "GAVE") Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
