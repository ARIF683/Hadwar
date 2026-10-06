package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Item
import com.example.data.model.SyncQueueItem
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY o ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY o ASC")
    suspend fun getAllItemsList(): List<Item>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemById(id: String): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemByIdOnce(id: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Item>)

    @Update
    suspend fun update(item: Item)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT MAX(o) FROM items")
    suspend fun getMaxOrder(): Int?
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TransactionRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TransactionRecord>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAll(): List<SyncQueueItem>

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun getFlow(): Flow<List<SyncQueueItem>>

    @Insert
    suspend fun insert(item: SyncQueueItem): Long

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getCountFlow(): Flow<Int>
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY created_at DESC")
    fun getAllQuotations(): Flow<List<com.example.data.model.QuotationRecord>>

    @Query("SELECT * FROM quotations ORDER BY created_at DESC")
    suspend fun getAllQuotationsList(): List<com.example.data.model.QuotationRecord>

    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    suspend fun getQuotationById(id: String): com.example.data.model.QuotationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quotation: com.example.data.model.QuotationRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotations: List<com.example.data.model.QuotationRecord>)

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM quotations")
    suspend fun deleteAll()
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<com.example.data.model.LedgerAccount>>

    @Query("SELECT * FROM ledger_accounts ORDER BY name ASC")
    suspend fun getAllAccountsList(): List<com.example.data.model.LedgerAccount>

    @Query("SELECT * FROM ledger_accounts WHERE type = :type ORDER BY name ASC")
    fun getAccountsByType(type: String): Flow<List<com.example.data.model.LedgerAccount>>

    @Query("SELECT * FROM ledger_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): com.example.data.model.LedgerAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: com.example.data.model.LedgerAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAccounts(accounts: List<com.example.data.model.LedgerAccount>)

    @Query("DELETE FROM ledger_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("SELECT * FROM ledger_entries WHERE account_id = :accountId ORDER BY date DESC, created_at DESC")
    fun getEntriesForAccount(accountId: String): Flow<List<com.example.data.model.LedgerEntry>>

    @Query("SELECT * FROM ledger_entries WHERE account_id = :accountId ORDER BY date ASC, created_at ASC")
    suspend fun getEntriesForAccountOnce(accountId: String): List<com.example.data.model.LedgerEntry>

    @Query("SELECT * FROM ledger_entries ORDER BY created_at DESC")
    suspend fun getAllEntriesList(): List<com.example.data.model.LedgerEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: com.example.data.model.LedgerEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(entries: List<com.example.data.model.LedgerEntry>)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)

    @Query("DELETE FROM ledger_entries WHERE account_id = :accountId")
    suspend fun deleteEntriesForAccount(accountId: String)

    @Query("DELETE FROM ledger_accounts")
    suspend fun deleteAllAccounts()

    @Query("DELETE FROM ledger_entries")
    suspend fun deleteAllEntries()
}

@Dao
interface DailyCashflowDao {
    @Query("SELECT * FROM daily_cashflow ORDER BY date DESC, created_at DESC")
    fun getAllCashflow(): Flow<List<com.example.data.model.DailyCashflowRecord>>

    @Query("SELECT * FROM daily_cashflow ORDER BY date DESC, created_at DESC")
    suspend fun getAllCashflowList(): List<com.example.data.model.DailyCashflowRecord>

    @Query("SELECT * FROM daily_cashflow WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, created_at DESC")
    fun getCashflowForMonth(monthPrefix: String): Flow<List<com.example.data.model.DailyCashflowRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: com.example.data.model.DailyCashflowRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<com.example.data.model.DailyCashflowRecord>)

    @Update
    suspend fun update(record: com.example.data.model.DailyCashflowRecord)

    @Query("DELETE FROM daily_cashflow WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM daily_cashflow")
    suspend fun deleteAll()
}
