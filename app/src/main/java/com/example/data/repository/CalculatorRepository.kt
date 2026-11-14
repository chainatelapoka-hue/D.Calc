package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.*
import com.example.data.model.CurrencyCatalog
import com.example.data.remote.CurrencyApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalculatorRepository(
    private val context: Context,
    private val database: NexoraDatabase,
    val userPreferences: UserPreferences
) {
    private val historyDao = database.historyDao()
    private val currencyDao = database.currencyDao()
    private val ledgerDao = database.ledgerDao()
    private val businessRecordDao = database.businessRecordDao()
    private val apiService = CurrencyApiService.create()

    private val _ratesState = MutableStateFlow<Map<String, Double>>(CurrencyCatalog.BASELINE_RATES_VS_USD)
    val ratesState: StateFlow<Map<String, Double>> = _ratesState.asStateFlow()

    private val _lastUpdated = MutableStateFlow("Baseline Offline Rates")
    val lastUpdated: StateFlow<String> = _lastUpdated.asStateFlow()

    private val _isOfflineOrCached = MutableStateFlow(true)
    val isOfflineOrCached: StateFlow<Boolean> = _isOfflineOrCached.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecentHistory(3)
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    init {
        // Load cached rates from Room first
        CoroutineScope(Dispatchers.IO).launch {
            loadCachedRates()
            // Auto refresh if network is available
            if (isNetworkAvailable()) {
                refreshRates()
            }
        }
    }

    private suspend fun loadCachedRates() {
        val count = currencyDao.getCount()
        if (count > 0) {
            val list = currencyDao.getAllRates().first()
            if (list.isNotEmpty()) {
                val map = list.associate { it.code to it.rateAgainstUsd }
                _ratesState.value = CurrencyCatalog.BASELINE_RATES_VS_USD + map
                _lastUpdated.value = list.firstOrNull()?.lastUpdatedUtc ?: "Cached rates"
                _isOfflineOrCached.value = true
            }
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun refreshRates(force: Boolean = false) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _statusMessage.value = null

        withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    _isOfflineOrCached.value = true
                    _statusMessage.value = "No internet: using local cached rates"
                    return@withContext
                }

                val response = apiService.getLatestRates("USD")
                if (response.result == "success" && response.rates != null) {
                    val map = response.rates
                    _ratesState.value = map
                    val updateTime = response.timeLastUpdateUtc ?: "Recent"
                    _lastUpdated.value = updateTime
                    _isOfflineOrCached.value = false
                    _statusMessage.value = "Rates updated successfully"

                    // Persist to Room
                    val entities = map.map { (code, rate) ->
                        CurrencyRateEntity(
                            code = code,
                            rateAgainstUsd = rate,
                            lastUpdatedUtc = updateTime
                        )
                    }
                    currencyDao.insertAll(entities)
                } else {
                    _isOfflineOrCached.value = true
                    _statusMessage.value = "Rates server returned unexpected format; using cache"
                }
            } catch (e: Exception) {
                _isOfflineOrCached.value = true
                _statusMessage.value = "Offline: ${e.message ?: "Could not reach exchange rate server"}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        customDirectRate: Double? = null
    ): Pair<Double, Double> {
        // If user provided custom direct rate (e.g. 1 RMB = 17.5 BDT or 1 USD = 122.5 BDT)
        if (customDirectRate != null && customDirectRate > 0) {
            val converted = amount * customDirectRate
            return Pair(converted, customDirectRate)
        }

        val rates = _ratesState.value
        val fromRateVsUsd = rates[fromCurrency] ?: CurrencyCatalog.BASELINE_RATES_VS_USD[fromCurrency] ?: 1.0
        val toRateVsUsd = rates[toCurrency] ?: CurrencyCatalog.BASELINE_RATES_VS_USD[toCurrency] ?: 1.0

        // 1 USD = fromRateVsUsd (from)
        // 1 USD = toRateVsUsd (to)
        // 1 from = (toRateVsUsd / fromRateVsUsd) to
        val effectiveRate = if (fromRateVsUsd > 0) toRateVsUsd / fromRateVsUsd else 1.0
        val converted = amount * effectiveRate
        return Pair(converted, effectiveRate)
    }

    suspend fun saveHistory(expression: String, result: String, category: String = "BASIC") {
        withContext(Dispatchers.IO) {
            historyDao.insert(
                HistoryEntity(
                    expression = expression,
                    result = result,
                    category = category
                )
            )
        }
    }

    suspend fun deleteHistory(id: Long) {
        withContext(Dispatchers.IO) {
            historyDao.deleteById(id)
        }
    }

    suspend fun clearAllHistory() {
        withContext(Dispatchers.IO) {
            historyDao.clearAll()
        }
    }

    // --- LEDGER / ACCOUNT BOOK OPERATIONS ---

    val allLedgerAccounts: Flow<List<LedgerAccountEntity>> = ledgerDao.getAllAccounts()
    val totalOutstandingAllAccounts: Flow<Double> = ledgerDao.getTotalOutstandingAllAccounts()

    fun getActiveItemsForAccount(accountId: Long): Flow<List<LedgerItemEntity>> =
        ledgerDao.getActiveItems(accountId)

    fun getAllItemsForAccount(accountId: Long): Flow<List<LedgerItemEntity>> =
        ledgerDao.getAllItems(accountId)

    fun getAccountActiveTotal(accountId: Long): Flow<Double> =
        ledgerDao.getAccountActiveTotal(accountId)

    fun getSettlementsForAccount(accountId: Long): Flow<List<LedgerSettlementEntity>> =
        ledgerDao.getSettlements(accountId)

    suspend fun getAccountById(id: Long): LedgerAccountEntity? = withContext(Dispatchers.IO) {
        ledgerDao.getAccountById(id)
    }

    suspend fun createLedgerAccount(name: String, phone: String = "", notes: String = ""): Long =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            ledgerDao.insertAccount(
                LedgerAccountEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    notes = notes.trim(),
                    createdAt = now,
                    lastUpdated = now
                )
            )
        }

    suspend fun addLedgerItem(
        accountId: Long,
        itemName: String,
        price: Double,
        quantity: Int = 1
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(now))

        val id = ledgerDao.insertItem(
            LedgerItemEntity(
                accountId = accountId,
                itemName = itemName.trim(),
                price = price,
                quantity = quantity.coerceAtLeast(1),
                dateMillis = now,
                formattedDate = formattedDate,
                isSettled = false
            )
        )
        ledgerDao.updateAccountTimestamp(accountId, now)
        id
    }

    suspend fun deleteLedgerItem(itemId: Long, accountId: Long) = withContext(Dispatchers.IO) {
        ledgerDao.deleteItem(itemId)
        ledgerDao.updateAccountTimestamp(accountId, System.currentTimeMillis())
    }

    suspend fun settleLedgerAccount(
        accountId: Long,
        settledAmount: Double,
        note: String = "Balance cleared"
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(now))

        val settlementId = ledgerDao.insertSettlement(
            LedgerSettlementEntity(
                accountId = accountId,
                amount = settledAmount,
                settledAt = now,
                formattedDate = formattedDate,
                note = note.trim()
            )
        )
        ledgerDao.markItemsSettled(accountId)
        ledgerDao.updateAccountTimestamp(accountId, now)
        settlementId
    }

    suspend fun deleteLedgerAccount(accountId: Long) = withContext(Dispatchers.IO) {
        ledgerDao.deleteAllItemsForAccount(accountId)
        ledgerDao.deleteAllSettlementsForAccount(accountId)
        ledgerDao.deleteAccount(accountId)
    }

    // --- BUSINESS SAVED RECORDS ---

    val allBusinessRecords: Flow<List<BusinessSavedRecordEntity>> = businessRecordDao.getAllRecords()

    fun getBusinessRecordsByCategory(category: String): Flow<List<BusinessSavedRecordEntity>> =
        businessRecordDao.getRecordsByCategory(category)

    suspend fun saveBusinessRecord(
        title: String,
        category: String,
        summaryResult: String,
        inputDataJson: String
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(now))
        businessRecordDao.insertRecord(
            BusinessSavedRecordEntity(
                title = title.trim(),
                category = category,
                summaryResult = summaryResult,
                inputDataJson = inputDataJson,
                timestamp = now,
                formattedDate = formattedDate
            )
        )
    }

    suspend fun deleteBusinessRecord(id: Long) = withContext(Dispatchers.IO) {
        businessRecordDao.deleteRecord(id)
    }
}


