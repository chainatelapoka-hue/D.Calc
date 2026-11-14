package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Query("SELECT * FROM calculation_history ORDER BY id DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM calculation_history ORDER BY id DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 3): Flow<List<HistoryEntity>>

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAll()
}

@Dao
interface CurrencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<CurrencyRateEntity>)

    @Query("SELECT * FROM currency_rates")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates WHERE code = :code LIMIT 1")
    suspend fun getRate(code: String): CurrencyRateEntity?

    @Query("SELECT COUNT(*) FROM currency_rates")
    suspend fun getCount(): Int
}

@Dao
interface LedgerDao {
    // Accounts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: LedgerAccountEntity): Long

    @Query("SELECT * FROM ledger_accounts ORDER BY lastUpdated DESC")
    fun getAllAccounts(): Flow<List<LedgerAccountEntity>>

    @Query("SELECT * FROM ledger_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): LedgerAccountEntity?

    @Query("UPDATE ledger_accounts SET lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateAccountTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM ledger_accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    // Items under account
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LedgerItemEntity): Long

    @Query("SELECT * FROM ledger_items WHERE accountId = :accountId AND isSettled = 0 ORDER BY dateMillis DESC")
    fun getActiveItems(accountId: Long): Flow<List<LedgerItemEntity>>

    @Query("SELECT * FROM ledger_items WHERE accountId = :accountId ORDER BY dateMillis DESC")
    fun getAllItems(accountId: Long): Flow<List<LedgerItemEntity>>

    @Query("SELECT COALESCE(SUM(price * quantity), 0.0) FROM ledger_items WHERE accountId = :accountId AND isSettled = 0")
    fun getAccountActiveTotal(accountId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(price * quantity), 0.0) FROM ledger_items WHERE isSettled = 0")
    fun getTotalOutstandingAllAccounts(): Flow<Double>

    @Query("SELECT COUNT(*) FROM ledger_items WHERE accountId = :accountId AND isSettled = 0")
    fun getActiveItemCount(accountId: Long): Flow<Int>

    @Query("DELETE FROM ledger_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    // Settlement / Clear Account
    @Query("UPDATE ledger_items SET isSettled = 1 WHERE accountId = :accountId AND isSettled = 0")
    suspend fun markItemsSettled(accountId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: LedgerSettlementEntity): Long

    @Query("SELECT * FROM ledger_settlements WHERE accountId = :accountId ORDER BY settledAt DESC")
    fun getSettlements(accountId: Long): Flow<List<LedgerSettlementEntity>>

    @Query("DELETE FROM ledger_items WHERE accountId = :accountId")
    suspend fun deleteAllItemsForAccount(accountId: Long)

    @Query("DELETE FROM ledger_settlements WHERE accountId = :accountId")
    suspend fun deleteAllSettlementsForAccount(accountId: Long)
}

@Dao
interface BusinessRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: BusinessSavedRecordEntity): Long

    @Query("SELECT * FROM business_saved_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<BusinessSavedRecordEntity>>

    @Query("SELECT * FROM business_saved_records WHERE category = :category ORDER BY timestamp DESC")
    fun getRecordsByCategory(category: String): Flow<List<BusinessSavedRecordEntity>>

    @Query("DELETE FROM business_saved_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM business_saved_records WHERE category = :category")
    suspend fun deleteRecordsByCategory(category: String)
}

