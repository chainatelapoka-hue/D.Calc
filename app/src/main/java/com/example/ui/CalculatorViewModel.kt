package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.local.NexoraDatabase
import com.example.data.local.UserPreferences
import com.example.data.local.UserSettings
import com.example.data.model.CurrencyCatalog
import com.example.data.repository.CalculatorRepository
import com.example.engine.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NexoraDatabase.getDatabase(application)
    val userPreferences = UserPreferences(application)
    val repository = CalculatorRepository(application, database, userPreferences)

    val userSettings: StateFlow<UserSettings> = userPreferences.settingsFlow

    // Main Calculator State
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _livePreviewResult = MutableStateFlow<String?>(null)
    val livePreviewResult: StateFlow<String?> = _livePreviewResult.asStateFlow()

    private val _finalResult = MutableStateFlow<String?>(null)
    val finalResult: StateFlow<String?> = _finalResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Recent 3 history items for display panel
    val recentHistory: StateFlow<List<HistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currency Converter State
    private val _currencyFrom = MutableStateFlow(userSettings.value.defaultFromCurrency)
    val currencyFrom: StateFlow<String> = _currencyFrom.asStateFlow()

    private val _currencyTo = MutableStateFlow(userSettings.value.defaultToCurrency)
    val currencyTo: StateFlow<String> = _currencyTo.asStateFlow()

    private val _currencyInputAmount = MutableStateFlow("100")
    val currencyInputAmount: StateFlow<String> = _currencyInputAmount.asStateFlow()

    private val _useCustomRate = MutableStateFlow(false)
    val useCustomRate: StateFlow<Boolean> = _useCustomRate.asStateFlow()

    private val _customRateInput = MutableStateFlow("")
    val customRateInput: StateFlow<String> = _customRateInput.asStateFlow()

    val ratesState = repository.ratesState
    val currencyLastUpdated = repository.lastUpdated
    val currencyIsOffline = repository.isOfflineOrCached
    val currencyIsRefreshing = repository.isRefreshing
    val currencyStatusMessage = repository.statusMessage

    val currencyConversionResult: StateFlow<Pair<Double, Double>> = combine(
        _currencyInputAmount,
        _currencyFrom,
        _currencyTo,
        _useCustomRate,
        _customRateInput
    ) { amountStr, from, to, useCustom, customRateStr ->
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val customRate = if (useCustom) customRateStr.toDoubleOrNull() else null
        repository.convertCurrency(amount, from, to, customRate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 1.0))

    // Large Number Converter State
    private val _largeNumberInput = MutableStateFlow("10000000")
    val largeNumberInput: StateFlow<String> = _largeNumberInput.asStateFlow()

    val largeNumberBreakdown: StateFlow<LargeNumberBreakdown?> = _largeNumberInput.map { input ->
        try {
            val clean = input.replace(",", "").trim()
            if (clean.isNotEmpty()) {
                val bd = BigDecimal(clean)
                NumberFormatter.getLargeNumberBreakdown(bd)
            } else null
        } catch (_: Exception) {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Compute live preview whenever expression changes
        viewModelScope.launch {
            _expression.collect { expr ->
                if (expr.isNotBlank()) {
                    computeLivePreview(expr)
                } else {
                    _livePreviewResult.value = null
                }
            }
        }
    }

    private fun computeLivePreview(expr: String) {
        val lastChar = expr.lastOrNull()
        if (lastChar != null && (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '÷' || lastChar == '(')) {
            return
        }
        val result = MathEngine.evaluate(expr)
        if (result is MathResult.Success) {
            _livePreviewResult.value = result.formatted
            _errorMessage.value = null
        }
    }

    fun onDigit(digit: String) {
        triggerHaptic()
        _errorMessage.value = null
        if (_finalResult.value != null && isDigitOrDecimal(digit)) {
            // Start fresh if user presses digit right after equals
            _expression.value = digit
            _finalResult.value = null
            return
        }
        _expression.value = _expression.value + digit
    }

    private fun isDigitOrDecimal(s: String): Boolean {
        return s.all { it.isDigit() || it == '.' }
    }

    fun onOperator(op: String) {
        triggerHaptic()
        _errorMessage.value = null
        val currentExpr = _expression.value

        if (currentExpr.isEmpty()) {
            if (_finalResult.value != null) {
                // Continue with previous result
                val cleanPrev = _finalResult.value!!.replace(",", "")
                _expression.value = cleanPrev + op
                _finalResult.value = null
                return
            }
            if (op == "-") {
                _expression.value = "-"
            }
            return
        }

        val lastChar = currentExpr.last()
        if (isOperatorChar(lastChar)) {
            // Replace previous operator
            _expression.value = currentExpr.dropLast(1) + op
        } else {
            _expression.value = currentExpr + op
        }
        _finalResult.value = null
    }

    private fun isOperatorChar(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '%' || c == '^'
    }

    fun onClear() {
        triggerHaptic()
        _expression.value = ""
        _livePreviewResult.value = null
        _finalResult.value = null
        _errorMessage.value = null
    }

    fun onBackspace() {
        triggerHaptic()
        _errorMessage.value = null
        val current = _expression.value
        if (current.isNotEmpty()) {
            // Check if removing a multi-letter function like "sin(" or "log("
            val funcs = listOf("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "log(", "ln(", "sqrt(", "cbrt(", "abs(", "fact(")
            val matchedFunc = funcs.firstOrNull { current.endsWith(it) }
            if (matchedFunc != null) {
                _expression.value = current.dropLast(matchedFunc.length)
            } else {
                _expression.value = current.dropLast(1)
            }
        }
        _finalResult.value = null
    }

    fun onEqual(category: String = "BASIC") {
        triggerHaptic()
        val expr = _expression.value
        if (expr.isBlank()) return

        val result = MathEngine.evaluate(expr)
        when (result) {
            is MathResult.Success -> {
                _finalResult.value = result.formatted
                _livePreviewResult.value = null
                _errorMessage.value = null
                // Save to local database
                viewModelScope.launch {
                    repository.saveHistory(expr, result.formatted, category)
                }
            }
            is MathResult.Error -> {
                _errorMessage.value = result.message
                _finalResult.value = null
            }
        }
    }

    fun onFunction(func: String) {
        triggerHaptic()
        _errorMessage.value = null
        when (func) {
            "±" -> toggleSign()
            "x²" -> _expression.value = _expression.value + "^2"
            "x³" -> _expression.value = _expression.value + "^3"
            "xʸ" -> _expression.value = _expression.value + "^"
            "1/x" -> {
                if (_expression.value.isNotEmpty()) {
                    _expression.value = "1/(" + _expression.value + ")"
                } else {
                    _expression.value = "1/"
                }
            }
            "π" -> _expression.value = _expression.value + "π"
            "e" -> _expression.value = _expression.value + "e"
            "%" -> onOperator("%")
            "√" -> _expression.value = _expression.value + "sqrt("
            "∛" -> _expression.value = _expression.value + "cbrt("
            "n!" -> _expression.value = _expression.value + "fact("
            "|x|" -> _expression.value = _expression.value + "abs("
            "mod" -> onOperator("%")
            else -> {
                _expression.value = _expression.value + "$func("
            }
        }
    }

    private fun toggleSign() {
        val expr = _expression.value
        if (expr.isEmpty()) {
            _expression.value = "-"
            return
        }
        if (expr.startsWith("-")) {
            _expression.value = expr.substring(1)
        } else {
            _expression.value = "-$expr"
        }
    }

    fun reuseHistory(item: HistoryEntity) {
        triggerHaptic()
        _expression.value = item.result
        _finalResult.value = null
        _errorMessage.value = null
    }

    fun reuseExpression(item: HistoryEntity) {
        triggerHaptic()
        _expression.value = item.expression
        _finalResult.value = null
        _errorMessage.value = null
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // Currency Actions
    fun setCurrencyFrom(code: String) {
        _currencyFrom.value = code
        // Suggest default custom rate if RMB to BDT or USD to BDT
        checkCustomRateDefaults(code, _currencyTo.value)
    }

    fun setCurrencyTo(code: String) {
        _currencyTo.value = code
        checkCustomRateDefaults(_currencyFrom.value, code)
    }

    private fun checkCustomRateDefaults(from: String, to: String) {
        if (from == "CNY" && to == "BDT") {
            _customRateInput.value = userSettings.value.customRmbToBdt.toString()
        } else if (from == "USD" && to == "BDT") {
            _customRateInput.value = userSettings.value.customUsdToBdt.toString()
        }
    }

    fun swapCurrencies() {
        triggerHaptic()
        val temp = _currencyFrom.value
        _currencyFrom.value = _currencyTo.value
        _currencyTo.value = temp
        checkCustomRateDefaults(_currencyFrom.value, _currencyTo.value)
    }

    fun setCurrencyInputAmount(amt: String) {
        _currencyInputAmount.value = amt
    }

    fun setUseCustomRate(use: Boolean) {
        triggerHaptic()
        _useCustomRate.value = use
        if (use && _customRateInput.value.isBlank()) {
            checkCustomRateDefaults(_currencyFrom.value, _currencyTo.value)
        }
    }

    fun setCustomRateInput(rate: String) {
        _customRateInput.value = rate
    }

    fun refreshCurrencyRates() {
        triggerHaptic()
        viewModelScope.launch {
            repository.refreshRates(force = true)
        }
    }

    // Large number converter
    fun setLargeNumberInput(input: String) {
        _largeNumberInput.value = input
    }

    fun copyToClipboard(text: String, label: String = "D. Calc") {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    fun pasteToExpression() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotBlank()) {
                val sanitized = text.replace(" ", "").replace("\n", "")
                _expression.value = _expression.value + sanitized
                triggerHaptic()
            }
        }
    }

    fun triggerHaptic() {
        if (!userSettings.value.hapticEnabled) return
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(12L)
                }
            }
        } catch (_: Exception) {}
    }

    // --- LEDGER (ACCOUNT BOOK) STATE & ACTIONS ---

    val allLedgerAccounts: StateFlow<List<com.example.data.local.LedgerAccountEntity>> =
        repository.allLedgerAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalOutstandingLedger: StateFlow<Double> =
        repository.totalOutstandingAllAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedAccount = MutableStateFlow<com.example.data.local.LedgerAccountEntity?>(null)
    val selectedAccount: StateFlow<com.example.data.local.LedgerAccountEntity?> = _selectedAccount.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAccountActiveItems: StateFlow<List<com.example.data.local.LedgerItemEntity>> =
        _selectedAccount.flatMapLatest { account ->
            if (account == null) flowOf(emptyList())
            else repository.getActiveItemsForAccount(account.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAccountTotal: StateFlow<Double> =
        _selectedAccount.flatMapLatest { account ->
            if (account == null) flowOf(0.0)
            else repository.getAccountActiveTotal(account.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedAccountSettlements: StateFlow<List<com.example.data.local.LedgerSettlementEntity>> =
        _selectedAccount.flatMapLatest { account ->
            if (account == null) flowOf(emptyList())
            else repository.getSettlementsForAccount(account.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(account: com.example.data.local.LedgerAccountEntity?) {
        _selectedAccount.value = account
        triggerHaptic()
    }

    fun createLedgerAccount(
        name: String,
        phone: String = "",
        notes: String = "",
        onCreated: ((com.example.data.local.LedgerAccountEntity) -> Unit)? = null
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createLedgerAccount(name, phone, notes)
            val created = repository.getAccountById(id)
            if (created != null) {
                _selectedAccount.value = created
                onCreated?.invoke(created)
            }
            triggerHaptic()
        }
    }

    fun addLedgerItem(
        accountId: Long,
        itemName: String,
        price: Double,
        quantity: Int = 1
    ) {
        if (itemName.isBlank() || price <= 0.0) return
        viewModelScope.launch {
            repository.addLedgerItem(accountId, itemName, price, quantity)
            triggerHaptic()
        }
    }

    fun deleteLedgerItem(itemId: Long, accountId: Long) {
        viewModelScope.launch {
            repository.deleteLedgerItem(itemId, accountId)
            triggerHaptic()
        }
    }

    fun settleLedgerAccount(
        accountId: Long,
        amount: Double,
        note: String = "Balance cleared"
    ) {
        viewModelScope.launch {
            repository.settleLedgerAccount(accountId, amount, note)
            triggerHaptic()
        }
    }

    fun deleteLedgerAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteLedgerAccount(accountId)
            if (_selectedAccount.value?.id == accountId) {
                _selectedAccount.value = null
            }
            triggerHaptic()
        }
    }

    // --- BUSINESS SAVED RECORDS ---

    val allBusinessRecords: StateFlow<List<com.example.data.local.BusinessSavedRecordEntity>> =
        repository.allBusinessRecords
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBusinessRecord(
        title: String,
        category: String,
        summaryResult: String,
        inputDataJson: String,
        onSaved: (() -> Unit)? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.saveBusinessRecord(title, category, summaryResult, inputDataJson)
            triggerHaptic()
            onSaved?.invoke()
        }
    }

    fun deleteBusinessRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteBusinessRecord(id)
            triggerHaptic()
        }
    }
}


