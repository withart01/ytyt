package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printer.EscPosHelper
import com.example.printer.PrinterStatus
import com.example.ui.components.CalculatorKeypad
import com.example.ui.viewmodel.PosViewModel

@Composable
fun PosMainScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) } // 0: Kasir, 1: Riwayat, 2: Printer
    var posMode by remember { mutableIntStateOf(0) } // 0: Kalkulator, 1: Katalog Menu
    var showCartSheet by remember { mutableStateOf(false) }
    var showSessionDialogFromTopBar by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val printerStatus by viewModel.printerStatus.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val cashierSession by viewModel.cashierSession.collectAsState()

    // Cart states
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val discountNominal by viewModel.discountNominal.collectAsState()
    val isTaxApplied by viewModel.isTaxApplied.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()
    val customerName by viewModel.customerName.collectAsState()

    // Products
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Transactions
    val transactions by viewModel.transactions.collectAsState()

    // Calculator states
    val calcDisplay by viewModel.calcDisplay.collectAsState()
    val calcExpression by viewModel.calcExpression.collectAsState()

    // Dialog states
    val isPaymentDialogOpen by viewModel.paymentDialogOpen.collectAsState()
    val cashReceived by viewModel.cashReceived.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()

    val completedTransaction by viewModel.completedTransaction.collectAsState()
    val completedItems by viewModel.completedItems.collectAsState()
    val isReceiptDialogOpen by viewModel.receiptDialogOpen.collectAsState()

    // Auth & Session Role
    val currentAuthUser by viewModel.currentAuthUser.collectAsState()
    val isAdmin = currentAuthUser?.isAdmin == true

    // Show Login / Session startup gate if not authenticated
    if (currentAuthUser == null) {
        LoginSessionScreen(
            storeName = storeSettings.storeName,
            onLoginCashier = { name, initialCash ->
                viewModel.loginCashier(name, initialCash)
            },
            onLoginAdmin = { user, pass ->
                viewModel.loginAdmin(user, pass)
            },
            modifier = modifier
        )
        return
    }

    // Role-based navigation safety: Kasir can only access tab 0 (Kasir) & 1 (Riwayat)
    if (!isAdmin && selectedNavIndex > 1) {
        selectedNavIndex = 0
    }

    androidx.activity.compose.BackHandler(enabled = showCartSheet || selectedNavIndex != 0 || posMode != 0) {
        when {
            showCartSheet -> showCartSheet = false
            posMode != 0 -> posMode = 0
            selectedNavIndex != 0 -> selectedNavIndex = 0
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Kasir") },
                    label = { Text("Kasir") },
                    modifier = Modifier.testTag("nav_kasir")
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                    label = { Text("Riwayat") },
                    modifier = Modifier.testTag("nav_history")
                )
                if (isAdmin) {
                    NavigationBarItem(
                        selected = selectedNavIndex == 2,
                        onClick = { selectedNavIndex = 2 },
                        icon = {
                            Box {
                                Icon(Icons.Default.Print, contentDescription = "Koneksi BT")
                                if (printerStatus is PrinterStatus.Connected) {
                                    Surface(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .align(Alignment.TopEnd),
                                        shape = CircleShape,
                                        color = Color(0xFF16A34A)
                                    ) {}
                                }
                            }
                        },
                        label = { Text("Koneksi BT") },
                        modifier = Modifier.testTag("nav_printer")
                    )
                    NavigationBarItem(
                        selected = selectedNavIndex == 3,
                        onClick = { selectedNavIndex = 3 },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = "Format Struk") },
                        label = { Text("Format Struk") },
                        modifier = Modifier.testTag("nav_receipt_format")
                    )
                    NavigationBarItem(
                        selected = selectedNavIndex == 4,
                        onClick = { selectedNavIndex = 4 },
                        icon = { Icon(Icons.Default.Storage, contentDescription = "Kelola DB") },
                        label = { Text("Kelola DB") },
                        modifier = Modifier.testTag("nav_admin_db")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavIndex) {
                0 -> {
                    // Kasir Screen (Calculator or Catalog)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Branding Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showSessionDialogFromTopBar = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isAdmin) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = if (isAdmin) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = storeSettings.storeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isAdmin) Color(0xFFD97706) else Color(0xFF16A34A),
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAdmin) "Super User (Admin)" else "Sesi: ${currentAuthUser?.displayName ?: cashierSession.cashierName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isAdmin) Color(0xFFB45309) else Color(0xFF15803D)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Dynamic Printer Status Pill
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            if (isAdmin) {
                                                selectedNavIndex = 2
                                            } else {
                                                val statusStr = if (printerStatus is PrinterStatus.Connected) "Terhubung" else "Belum Terhubung"
                                                viewModel.showMessage("Status Printer: $statusStr (Khusus Admin)")
                                            }
                                        }
                                        .testTag("printer_status_pill"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (printerStatus is PrinterStatus.Connected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (printerStatus is PrinterStatus.Connected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.5f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(
                                                    color = if (printerStatus is PrinterStatus.Connected) Color(0xFF16A34A) else Color(0xFF94A3B8),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (printerStatus is PrinterStatus.Connected) "BT On" else "BT Off",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (printerStatus is PrinterStatus.Connected) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Logout / Ganti Sesi Icon Button
                                IconButton(
                                    onClick = { viewModel.logout() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("btn_logout_topbar")
                                ) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = "Keluar / Ganti Sesi",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Mode Segment Pill: [ Kalkulator Kasir ] | [ Katalog Menu ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModeTabButton(
                                        text = "Kalkulator Cepat",
                                        icon = Icons.Default.Calculate,
                                        selected = posMode == 0,
                                        modifier = Modifier.weight(1f),
                                        onClick = { posMode = 0 }
                                    )
                                    ModeTabButton(
                                        text = "Katalog Menu",
                                        icon = Icons.Default.RestaurantMenu,
                                        selected = posMode == 1,
                                        modifier = Modifier.weight(1f),
                                        onClick = { posMode = 1 }
                                    )
                                }
                            }
                        }

                        // Content according to posMode
                        if (posMode == 0) {
                            CalculatorKeypad(
                                displayValue = calcDisplay,
                                expression = calcExpression,
                                onDigit = { viewModel.onCalcDigit(it) },
                                onOperator = { viewModel.onCalcOperator(it) },
                                onEquals = { viewModel.onCalcEquals() },
                                onClear = { viewModel.onCalcClear() },
                                onBackspace = { viewModel.onCalcBackspace() },
                                onDenomination = { viewModel.onCalcDenomination(it) },
                                onAddToCart = { viewModel.addCurrentCalcToCart(it) },
                                onQuickPay = { viewModel.openPaymentDialog() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            CatalogScreen(
                                products = filteredProducts,
                                cartItems = cartItems,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                onCategorySelect = { viewModel.selectCategory(it) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onAddToCart = { viewModel.addToCart(it) },
                                onAddNewProduct = { viewModel.addNewProduct(it) },
                                canAddProduct = isAdmin,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bottom Floating Cart Bar if cart has items
                        if (cartItems.isNotEmpty()) {
                            FloatingCartBar(
                                itemCount = cartItems.sumOf { it.quantity },
                                total = grandTotal,
                                onClick = { showCartSheet = true }
                            )
                        }
                    }
                }

                1 -> {
                    // Riwayat Transaksi Screen
                    HistoryScreen(
                        transactions = transactions,
                        storeSettings = storeSettings,
                        cashierSession = cashierSession,
                        printerStatus = printerStatus,
                        onPrintDailyReport = { dateText, total, count, cash, nonCash ->
                            viewModel.printDailyReport(dateText, total, count, cash, nonCash)
                        },
                        onRePrintReceipt = { tx, items ->
                            viewModel.printReceipt(tx, items)
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onStartNewSession = { cashier, initialCash ->
                            viewModel.startNewCashierSession(cashier, initialCash)
                        },
                        onCloseSession = {
                            viewModel.closeCashierSession(autoPrintSummary = true)
                        },
                        onPrintSessionReport = {
                            viewModel.printCurrentSessionReport()
                        },
                        onExportData = {
                            viewModel.exportTransactionsText()
                        },
                        onShowMessage = { viewModel.showMessage(it) }
                    )
                }

                2 -> {
                    // Dedicated Bluetooth Printer Connection Screen (Status & Find only - Admin)
                    if (isAdmin) {
                        PrinterConnectionScreen(
                            printerManager = viewModel.printerManager,
                            printerStatus = printerStatus,
                            onTestPrint = { viewModel.printTestReceipt() },
                            onShowMessage = { viewModel.showMessage(it) }
                        )
                    }
                }

                3 -> {
                    // Dedicated Receipt Format & Paper Settings Screen (Admin)
                    if (isAdmin) {
                        ReceiptFormatScreen(
                            storeSettings = storeSettings,
                            printerStatus = printerStatus,
                            onUpdateStoreSettings = { viewModel.updateStoreSettings(it) },
                            onTestPrint = { viewModel.printTestReceipt() },
                            onShowMessage = { viewModel.showMessage(it) }
                        )
                    }
                }

                4 -> {
                    // Dedicated Admin Database Screen (Admin Super User)
                    if (isAdmin) {
                        val allProducts by viewModel.products.collectAsState()
                        AdminDatabaseScreen(
                            products = allProducts,
                            transactionCount = transactions.size,
                            onAddNewProduct = { viewModel.addNewProduct(it) },
                            onUpdateProduct = { viewModel.updateProduct(it) },
                            onDeleteProduct = { viewModel.deleteProduct(it) },
                            onResetDefaultProducts = { viewModel.resetProductsToDefault() },
                            onClearAllTransactions = { viewModel.clearAllTransactionsData() },
                            onExportTransactions = { viewModel.exportTransactionsText() },
                            onShowMessage = { viewModel.showMessage(it) }
                        )
                    }
                }
            }
        }
    }

    // Cart Bottom Sheet
    if (showCartSheet) {
        CartSheet(
            cartItems = cartItems,
            subtotal = subtotal,
            discountNominal = discountNominal,
            isTaxApplied = isTaxApplied,
            grandTotal = grandTotal,
            customerName = customerName,
            onUpdateQuantity = { item, delta -> viewModel.updateQuantity(item, delta) },
            onRemoveItem = { viewModel.removeFromCart(it) },
            onClearCart = { viewModel.clearCart() },
            onSetDiscount = { viewModel.setDiscountNominal(it) },
            onToggleTax = { viewModel.toggleTax(it) },
            onCustomerNameChange = { viewModel.updateCustomerName(it) },
            onProceedToPayment = {
                showCartSheet = false
                viewModel.openPaymentDialog()
            },
            onDismiss = { showCartSheet = false }
        )
    }

    // Payment Dialog
    if (isPaymentDialogOpen) {
        PaymentDialog(
            grandTotal = grandTotal,
            cashReceived = cashReceived,
            paymentMethod = paymentMethod,
            onPaymentMethodChange = { viewModel.setPaymentMethod(it) },
            onCashReceivedChange = { viewModel.setCashReceived(it) },
            onConfirmPayment = { autoPrint ->
                viewModel.completeCheckout(autoPrint = autoPrint)
            },
            onDismiss = { viewModel.closePaymentDialog() }
        )
    }

    // Completed Receipt Dialog
    if (isReceiptDialogOpen && completedTransaction != null) {
        ReceiptDialog(
            transaction = completedTransaction!!,
            items = completedItems,
            settings = storeSettings,
            printerStatus = printerStatus,
            onPrint = {
                viewModel.printReceipt(completedTransaction!!, completedItems)
            },
            onDismiss = { viewModel.closeReceiptDialog() }
        )
    }

    // Cashier Session Dialog from Top Bar
    if (showSessionDialogFromTopBar) {
        CashierSessionDialog(
            session = cashierSession,
            transactions = transactions,
            onStartNewSession = { cashier, initialCash ->
                viewModel.startNewCashierSession(cashier, initialCash)
            },
            onCloseSession = {
                viewModel.closeCashierSession(autoPrintSummary = true)
            },
            onPrintSessionReport = {
                viewModel.printCurrentSessionReport()
            },
            onExportData = {
                viewModel.exportTransactionsText()
            },
            onShowMessage = { viewModel.showMessage(it) },
            onDismiss = { showSessionDialogFromTopBar = false }
        )
    }
}

@Composable
private fun ModeTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = if (selected) 3.dp else 0.dp,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FloatingCartBar(
    itemCount: Int,
    total: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("floating_cart_bar"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F766E))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF134E4A),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$itemCount",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Keranjang Pesanan",
                            fontSize = 10.sp,
                            color = Color(0xFF99F6E4),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = EscPosHelper.formatRupiah(total),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF134E4A),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bayar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
