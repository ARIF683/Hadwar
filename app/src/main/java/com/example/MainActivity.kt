package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.ui.CurrentScreen
import com.example.ui.NavigationTab
import com.example.ui.StockViewModel
import com.example.ui.components.InputPromptDialog
import com.example.ui.screens.AuthGateScreen
import com.example.ui.screens.BillScreen
import com.example.ui.screens.HomeScreen
import androidx.compose.material.icons.filled.ReceiptLong
import com.example.ui.screens.BillingHubScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.ItemFormDialog
import com.example.ui.screens.ItemsScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.QuotationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmoothSplashScreen
import com.example.ui.screens.StockTransactionDialog
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.StockManagerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: StockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockManagerTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppRoot(viewModel: StockViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    val selectedLogo by viewModel.selectedLogo.collectAsState()

    if (showSplash) {
        SmoothSplashScreen(
            selectedLogo = selectedLogo,
            onAnimationFinished = { showSplash = false }
        )
        return
    }

    val isAuthed by viewModel.isAuthed.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var itemForForm by remember { mutableStateOf<Item?>(null) }
    var showItemForm by remember { mutableStateOf(false) }
    var itemForTransaction by remember { mutableStateOf<Item?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showAdminPinDialog by remember { mutableStateOf(false) }

    // Gear triple tap tracking
    var gearTapCount by remember { mutableIntStateOf(0) }
    var lastGearTapTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(isAuthed) {
        if (isAuthed) {
            viewModel.reconnectRealtime()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, isAuthed) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && isAuthed) {
                viewModel.reconnectRealtime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = isAuthed) {
        val handled = viewModel.navigateBack()
        if (!handled && currentTab != NavigationTab.ITEMS) {
            viewModel.navigateTo(NavigationTab.ITEMS)
        }
    }

    if (!isAuthed) {
        AuthGateScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen is CurrentScreen.Main) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Triple(NavigationTab.HOME, "Home", Icons.Default.Home),
                        Triple(NavigationTab.ITEMS, "Stock", Icons.Default.Inventory2),
                        Triple(NavigationTab.BILLING, "Billing", Icons.Default.ReceiptLong),
                        Triple(NavigationTab.TRANSACTIONS, "History", Icons.AutoMirrored.Filled.CompareArrows),
                        Triple(NavigationTab.SETTINGS, "Settings", Icons.Default.Settings)
                    )

                    tabs.forEach { (tab, label, icon) ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (tab == NavigationTab.SETTINGS) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastGearTapTime < 600) {
                                        gearTapCount++
                                        if (gearTapCount >= 3) {
                                            if (isAdmin) {
                                                viewModel.authManager.exitAdmin()
                                                viewModel.showToast("Admin mode off")
                                            } else {
                                                showAdminPinDialog = true
                                            }
                                            gearTapCount = 0
                                        }
                                    } else {
                                        gearTapCount = 1
                                    }
                                    lastGearTapTime = now
                                }
                                viewModel.navigateTo(tab)
                            },
                            icon = {
                                if (tab == NavigationTab.SETTINGS && isAdmin) {
                                    BadgedBox(
                                        badge = {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(BrandBlue)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "ADMIN",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(icon, contentDescription = label)
                                    }
                                } else {
                                    Icon(icon, contentDescription = label)
                                }
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandBlue,
                                selectedTextColor = BrandBlue,
                                indicatorColor = BrandBlue.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is CurrentScreen.ItemDetail -> {
                    ItemDetailScreen(
                        itemId = screen.itemId,
                        viewModel = viewModel,
                        onEditItem = {
                            itemForForm = it
                            showItemForm = true
                        },
                        onOpenTransactionDialog = {
                            itemForTransaction = it
                            showTransactionDialog = true
                        }
                    )
                }

                is CurrentScreen.BillFlow -> {
                    BillScreen(viewModel = viewModel)
                }

                is CurrentScreen.LedgerAccountDetail -> {
                    val accounts by viewModel.allLedgerAccounts.collectAsState()
                    val targetAcc = accounts.find { it.id == screen.accountId }
                    if (targetAcc != null) {
                        com.example.ui.screens.LedgerAccountDetailScreen(
                            account = targetAcc,
                            viewModel = viewModel,
                            onBack = { viewModel.navigateToKhata() }
                        )
                    } else {
                        BillingHubScreen(viewModel = viewModel)
                    }
                }

                is CurrentScreen.Main -> {
                    when (currentTab) {
                        NavigationTab.HOME -> HomeScreen(viewModel = viewModel)
                        NavigationTab.ITEMS -> ItemsScreen(
                            viewModel = viewModel,
                            onOpenItemForm = {
                                itemForForm = it
                                showItemForm = true
                            }
                        )
                        NavigationTab.BILLING -> BillingHubScreen(viewModel = viewModel)
                        NavigationTab.TRANSACTIONS -> TransactionsScreen(viewModel = viewModel)
                        NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Add / Edit Item Dialog
    if (showItemForm) {
        ItemFormDialog(
            itemToEdit = itemForForm,
            viewModel = viewModel,
            onDismiss = {
                showItemForm = false
                itemForForm = null
            }
        )
    }

    // Quick Transaction Dialog
    if (showTransactionDialog && itemForTransaction != null) {
        StockTransactionDialog(
            item = itemForTransaction!!,
            viewModel = viewModel,
            onDismiss = {
                showTransactionDialog = false
                itemForTransaction = null
            }
        )
    }

    // Admin PIN shortcut dialog
    if (showAdminPinDialog) {
        InputPromptDialog(
            title = "Admin access",
            message = "Enter the admin PIN to reveal restricted actions.",
            placeholder = "PIN",
            confirmText = "Unlock",
            isPassword = true,
            onConfirm = { pin ->
                val ok = viewModel.authManager.unlockAdmin(pin)
                if (ok) {
                    viewModel.showToast("Admin mode on")
                    showAdminPinDialog = false
                } else {
                    viewModel.showToast("Wrong PIN")
                }
            },
            onDismiss = { showAdminPinDialog = false }
        )
    }
}
