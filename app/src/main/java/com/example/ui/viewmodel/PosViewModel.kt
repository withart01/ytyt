package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.ProductEntity
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.printer.BluetoothPrinterManager
import com.example.printer.EscPosHelper
import com.example.printer.PrinterStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val productDao = db.productDao()
    private val transactionDao = db.transactionDao()

    val printerManager = BluetoothPrinterManager(application)
    val printerStatus: StateFlow<PrinterStatus> = printerManager.printerStatus

    // --- Products & Catalog ---
    val products: StateFlow<List<ProductEntity>> = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        products,
        _selectedCategory,
        _searchQuery
    ) { prods, category, query ->
        prods.filter { prod ->
            val matchCategory = category == "Semua" || prod.category.equals(category, ignoreCase = true)
            val matchQuery = query.isBlank() || prod.name.contains(query, ignoreCase = true) || prod.barcode.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Cart State ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _discountNominal = MutableStateFlow(0L)
    val discountNominal: StateFlow<Long> = _discountNominal.asStateFlow()

    private val _isTaxApplied = MutableStateFlow(false)
    val isTaxApplied: StateFlow<Boolean> = _isTaxApplied.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    val subtotal: StateFlow<Long> = _cartItems.combine(_discountNominal) { items, _ ->
        items.sumOf { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val grandTotal: StateFlow<Long> = combine(
        subtotal,
        _discountNominal,
        _isTaxApplied
    ) { sub, disc, tax ->
        val afterDisc = (sub - disc).coerceAtLeast(0L)
        val taxAmount = if (tax) (afterDisc * 0.11).toLong() else 0L
        afterDisc + taxAmount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // --- Calculator State ---
    private val _calcDisplay = MutableStateFlow("0")
    val calcDisplay: StateFlow<String> = _calcDisplay.asStateFlow()

    private val _calcExpression = MutableStateFlow("")
    val calcExpression: StateFlow<String> = _calcExpression.asStateFlow()

    private var currentOperand: Long = 0L
    private var pendingOperator: String? = null
    private var isNewNumber = true

    // --- Store Settings ---
    private val _storeSettings = MutableStateFlow(StoreSettings())
    val storeSettings: StateFlow<StoreSettings> = _storeSettings.asStateFlow()

    // --- Active Payment & Receipt State ---
    private val _paymentDialogOpen = MutableStateFlow(false)
    val paymentDialogOpen: StateFlow<Boolean> = _paymentDialogOpen.asStateFlow()

    private val _cashReceived = MutableStateFlow(0L)
    val cashReceived: StateFlow<Long> = _cashReceived.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Tunai")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _completedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val completedTransaction: StateFlow<TransactionEntity?> = _completedTransaction.asStateFlow()

    private val _completedItems = MutableStateFlow<List<CartItem>>(emptyList())
    val completedItems: StateFlow<List<CartItem>> = _completedItems.asStateFlow()

    private val _receiptDialogOpen = MutableStateFlow(false)
    val receiptDialogOpen: StateFlow<Boolean> = _receiptDialogOpen.asStateFlow()

    // --- Transactions History ---
    val transactions: StateFlow<List<TransactionEntity>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- User Feedback / Toast message ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (productDao.getProductCount() == 0) {
                    productDao.insertProducts(AppDatabase.getInitialSampleProducts())
                }
            } catch (e: Exception) {
                android.util.Log.e("PosViewModel", "Error seeding initial products", e)
            }
        }

        // Pre-fill last saved printer
        val savedPrinter = printerManager.getLastSavedDevice()
        if (savedPrinter != null) {
            _storeSettings.value = _storeSettings.value.copy(
                selectedPrinterMac = savedPrinter.address,
                selectedPrinterName = savedPrinter.name
            )
        }
    }

    // --- Filter & Search ---
    fun selectCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Cart Actions ---
    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.productId == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(
                CartItem(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = 1
                )
            )
        }
        _cartItems.value = current
    }

    fun addCustomItemToCart(name: String, price: Long, quantity: Int = 1) {
        if (price <= 0) return
        val current = _cartItems.value.toMutableList()
        val cleanName = if (name.isBlank()) "Item Kasir" else name
        current.add(
            CartItem(
                productId = null,
                name = cleanName,
                price = price,
                quantity = quantity
            )
        )
        _cartItems.value = current
        showMessage("Ditambahkan: $cleanName (${EscPosHelper.formatRupiah(price)})")
    }

    fun updateQuantity(item: CartItem, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOf(item)
        if (index >= 0) {
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(item: CartItem) {
        val current = _cartItems.value.toMutableList()
        current.remove(item)
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _discountNominal.value = 0L
        _isTaxApplied.value = false
        _customerName.value = ""
    }

    fun setDiscountNominal(nominal: Long) {
        _discountNominal.value = nominal.coerceAtLeast(0L)
    }

    fun toggleTax(enabled: Boolean) {
        _isTaxApplied.value = enabled
    }

    fun updateCustomerName(name: String) {
        _customerName.value = name
    }

    // --- Calculator Operations ---
    fun onCalcDigit(digit: String) {
        if (isNewNumber || _calcDisplay.value == "0") {
            _calcDisplay.value = digit
            isNewNumber = false
        } else {
            if (_calcDisplay.value.length < 12) {
                _calcDisplay.value += digit
            }
        }
    }

    fun onCalcDenomination(amount: Long) {
        val current = _calcDisplay.value.toLongOrNull() ?: 0L
        val next = current + amount
        _calcDisplay.value = next.toString()
        isNewNumber = false
    }

    fun onCalcOperator(op: String) {
        val currentValue = _calcDisplay.value.toLongOrNull() ?: 0L
        if (pendingOperator != null) {
            calculateRunningResult()
        } else {
            currentOperand = currentValue
        }
        pendingOperator = op
        _calcExpression.value = "${formatNumber(currentOperand)} $op"
        isNewNumber = true
    }

    fun onCalcEquals() {
        if (pendingOperator != null) {
            calculateRunningResult()
            _calcExpression.value = ""
            pendingOperator = null
            isNewNumber = true
        }
    }

    private fun calculateRunningResult() {
        val secondOperand = _calcDisplay.value.toLongOrNull() ?: 0L
        val result = when (pendingOperator) {
            "+" -> currentOperand + secondOperand
            "-" -> (currentOperand - secondOperand).coerceAtLeast(0L)
            "×", "*" -> currentOperand * secondOperand
            "÷", "/" -> if (secondOperand != 0L) currentOperand / secondOperand else currentOperand
            else -> secondOperand
        }
        currentOperand = result
        _calcDisplay.value = result.toString()
    }

    fun onCalcClear() {
        _calcDisplay.value = "0"
        _calcExpression.value = ""
        currentOperand = 0L
        pendingOperator = null
        isNewNumber = true
    }

    fun onCalcBackspace() {
        if (isNewNumber) return
        val text = _calcDisplay.value
        _calcDisplay.value = if (text.length > 1) text.dropLast(1) else "0"
        if (_calcDisplay.value == "0") isNewNumber = true
    }

    fun addCurrentCalcToCart(itemName: String = "") {
        val amount = _calcDisplay.value.toLongOrNull() ?: 0L
        if (amount > 0) {
            addCustomItemToCart(itemName.ifBlank { "Item Manual" }, amount)
            onCalcClear()
        } else {
            showMessage("Masukkan nominal diatas 0")
        }
    }

    private fun formatNumber(num: Long): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(num)
    }

    // --- Payment & Checkout ---
    fun openPaymentDialog() {
        if (_cartItems.value.isEmpty()) {
            val amount = _calcDisplay.value.toLongOrNull() ?: 0L
            if (amount > 0) {
                // If cart is empty but calculator has a number, convert to quick sale
                addCustomItemToCart("Penjualan Cepat", amount)
                onCalcClear()
            } else {
                showMessage("Keranjang masih kosong!")
                return
            }
        }
        _cashReceived.value = grandTotal.value
        _paymentMethod.value = "Tunai"
        _paymentDialogOpen.value = true
    }

    fun closePaymentDialog() {
        _paymentDialogOpen.value = false
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        if (method != "Tunai") {
            _cashReceived.value = grandTotal.value
        }
    }

    fun setCashReceived(amount: Long) {
        _cashReceived.value = amount
    }

    fun completeCheckout(autoPrint: Boolean = false) {
        val total = grandTotal.value
        val method = _paymentMethod.value
        val cash = _cashReceived.value

        if (method == "Tunai" && cash < total) {
            showMessage("Uang tunai kurang! Kurang: ${EscPosHelper.formatRupiah(total - cash)}")
            return
        }

        val change = if (method == "Tunai") (cash - total).coerceAtLeast(0L) else 0L
        val invoiceNo = generateInvoiceNumber()
        val items = _cartItems.value.toList()
        val summary = items.joinToString(", ") { "${it.name} (${it.quantity}x)" }

        val tx = TransactionEntity(
            invoiceNumber = invoiceNo,
            timestamp = System.currentTimeMillis(),
            subtotal = subtotal.value,
            discount = _discountNominal.value,
            tax = if (_isTaxApplied.value) (subtotal.value * 0.11).toLong() else 0L,
            grandTotal = total,
            paymentMethod = method,
            cashReceived = if (method == "Tunai") cash else total,
            changeAmount = change,
            cashierName = _storeSettings.value.cashierName,
            customerName = _customerName.value,
            itemsSummary = summary,
            itemsJson = ""
        )

        viewModelScope.launch {
            transactionDao.insertTransaction(tx)
            _completedTransaction.value = tx
            _completedItems.value = items
            _paymentDialogOpen.value = false
            _receiptDialogOpen.value = true
            clearCart()

            if (autoPrint) {
                printReceipt(tx, items)
            }
        }
    }

    fun closeReceiptDialog() {
        _receiptDialogOpen.value = false
        _completedTransaction.value = null
        _completedItems.value = emptyList()
    }

    private fun generateInvoiceNumber(): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        return "INV-" + sdf.format(Date())
    }

    // --- Product CRUD ---
    fun addNewProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.insertProduct(product)
            showMessage("Produk '${product.name}' berhasil disimpan")
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.updateProduct(product)
            showMessage("Produk diperbarui")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
            showMessage("Produk dihapus")
        }
    }

    // --- Transaction CRUD ---
    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            transactionDao.deleteTransaction(tx)
            showMessage("Transaksi ${tx.invoiceNumber} dihapus")
        }
    }

    // --- Printing ---
    fun printReceipt(tx: TransactionEntity, items: List<CartItem>) {
        val bytes = EscPosHelper.buildReceiptBytes(_storeSettings.value, tx, items)
        val textPreview = EscPosHelper.buildReceiptText(_storeSettings.value, tx, items)
        printerManager.sendBytes(bytes, previewText = textPreview) { success, msg ->
            if (msg != null) showMessage(msg)
        }
    }

    fun printTestReceipt() {
        val bytes = EscPosHelper.buildTestReceiptBytes(_storeSettings.value)
        printerManager.sendBytes(bytes) { success, msg ->
            if (msg != null) showMessage(msg)
        }
    }

    fun printDailyReport(
        dateText: String,
        totalSales: Long,
        txCount: Int,
        cashTotal: Long,
        nonCashTotal: Long
    ) {
        val bytes = EscPosHelper.buildDailyReportBytes(
            _storeSettings.value,
            dateText,
            totalSales,
            txCount,
            cashTotal,
            nonCashTotal
        )
        printerManager.sendBytes(bytes) { success, msg ->
            if (msg != null) showMessage(msg)
        }
    }

    // --- Settings ---
    fun updateStoreSettings(newSettings: StoreSettings) {
        _storeSettings.value = newSettings
        showMessage("Pengaturan toko disimpan")
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.destroy()
    }
}
