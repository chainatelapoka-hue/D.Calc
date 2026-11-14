package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoryEntity::class,
        CurrencyRateEntity::class,
        LedgerAccountEntity::class,
        LedgerItemEntity::class,
        LedgerSettlementEntity::class,
        BusinessSavedRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun businessRecordDao(): BusinessRecordDao

    companion object {
        @Volatile
        private var INSTANCE: NexoraDatabase? = null

        fun getDatabase(context: Context): NexoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexoraDatabase::class.java,
                    "nexora_calc.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
