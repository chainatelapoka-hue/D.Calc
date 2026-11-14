package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "BASIC"
)

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey
    val code: String,
    val rateAgainstUsd: Double,
    val lastUpdatedUtc: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger_accounts")
data class LedgerAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger_items")
data class LedgerItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val itemName: String,
    val price: Double,
    val quantity: Int = 1,
    val dateMillis: Long = System.currentTimeMillis(),
    val formattedDate: String = "",
    val isSettled: Boolean = false
)

@Entity(tableName = "ledger_settlements")
data class LedgerSettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val amount: Double,
    val settledAt: Long = System.currentTimeMillis(),
    val formattedDate: String = "",
    val note: String = "Balance cleared"
)

@Entity(tableName = "business_saved_records")
data class BusinessSavedRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val summaryResult: String,
    val inputDataJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = ""
)
