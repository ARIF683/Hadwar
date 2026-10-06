package com.example.data.remote

import android.util.Log
import com.example.data.model.DailyCashflowRecord
import com.example.data.model.Item
import com.example.data.model.LedgerAccount
import com.example.data.model.LedgerEntry
import com.example.data.model.QuotationRecord
import com.example.data.model.PurchaseRecord
import com.example.data.model.TransactionRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SupabaseClient(
    private val baseUrl: String = "https://xkjvcufajsyqbzjjllma.supabase.co",
    private val apiKey: String = "sb_publishable_sXokssWnrTPWROtmfHjoWA_LRqVPt8V"
) {
    private val tag = "SupabaseClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val itemListAdapter = moshi.adapter<List<Item>>(
        Types.newParameterizedType(List::class.java, Item::class.java)
    )
    private val txListAdapter = moshi.adapter<List<TransactionRecord>>(
        Types.newParameterizedType(List::class.java, TransactionRecord::class.java)
    )
    private val cashflowListAdapter = moshi.adapter<List<DailyCashflowRecord>>(
        Types.newParameterizedType(List::class.java, DailyCashflowRecord::class.java)
    )
    private val quotationListAdapter = moshi.adapter<List<QuotationRecord>>(
        Types.newParameterizedType(List::class.java, QuotationRecord::class.java)
    )
    private val ledgerAccountListAdapter = moshi.adapter<List<LedgerAccount>>(
        Types.newParameterizedType(List::class.java, LedgerAccount::class.java)
    )
    private val ledgerEntryListAdapter = moshi.adapter<List<LedgerEntry>>(
        Types.newParameterizedType(List::class.java, LedgerEntry::class.java)
    )
    private val purchaseAdapter = moshi.adapter(PurchaseRecord::class.java)

    // Standard HTTP client with timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // WebSocket client with NO read timeout (0) so it never drops long-lived subscriptions
    private val wsHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private fun newRequestBuilder(path: String): Request.Builder {
        val url = if (path.startsWith("http")) path else "$baseUrl/rest/v1/$path"
        return Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
    }

    suspend fun fetchAllItems(): List<Item> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Item>()
        var offset = 0
        while (true) {
            val req = newRequestBuilder("items?select=*&order=o.asc&limit=1000&offset=$offset")
                .get()
                .build()
            val resp: Response = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                throw Exception("Failed to load items: $err")
            }
            val body = resp.body?.string() ?: "[]"
            val page = itemListAdapter.fromJson(body) ?: emptyList()
            result.addAll(page)
            if (page.size < 1000) break
            offset += 1000
        }
        result
    }

    suspend fun fetchRecentTransactions(limit: Int = 500): List<TransactionRecord> = withContext(Dispatchers.IO) {
        val req = newRequestBuilder("transactions?select=*&order=created_at.desc&limit=$limit")
            .get()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to load transactions: $err")
        }
        val body = resp.body?.string() ?: "[]"
        txListAdapter.fromJson(body) ?: emptyList()
    }

    suspend fun upsertItems(items: List<Item>): Unit = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val json = itemListAdapter.toJson(items)
        val req = newRequestBuilder("items?on_conflict=id")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to upsert items: $err")
        }
    }

    suspend fun patchItem(id: String, patchJson: String): Unit = withContext(Dispatchers.IO) {
        val req = newRequestBuilder("items?id=eq.${java.net.URLEncoder.encode(id, "UTF-8")}")
            .addHeader("Prefer", "return=minimal")
            .patch(patchJson.toRequestBody(jsonMediaType))
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to patch item $id: $err")
        }
    }

    suspend fun deleteItems(ids: List<String>): Unit = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val idsFormatted = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
        val req = newRequestBuilder("items?id=in.($idsFormatted)")
            .delete()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to delete items: $err")
        }
    }

    suspend fun deleteAllItems(): Unit = withContext(Dispatchers.IO) {
        val req = newRequestBuilder("items?id=not.is.null")
            .delete()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to delete all items: $err")
        }
    }

    suspend fun insertTransactions(transactions: List<TransactionRecord>): Unit = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) return@withContext
        val json = txListAdapter.toJson(transactions)
        
        // 1. Try plain POST first (standard PostgREST insert without on_conflict to avoid Postgres 42P10 error)
        var req = newRequestBuilder("transactions")
            .addHeader("Prefer", "return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        var resp = okHttpClient.newCall(req).execute()
        if (resp.isSuccessful) return@withContext

        val firstErr = resp.body?.string() ?: "HTTP ${resp.code}"
        Log.w(tag, "First attempt to insert transactions failed: $firstErr. Retrying with fallback payload...")

        // 2. Fallback: If table has id column instead of or in addition to client_id
        try {
            val jsonArray = org.json.JSONArray()
            for (tx in transactions) {
                val obj = org.json.JSONObject().apply {
                    put("id", tx.clientId)
                    put("client_id", tx.clientId)
                    if (tx.itemId != null) put("item_id", tx.itemId)
                    put("item_name", tx.itemName)
                    put("action", tx.action)
                    put("qty", tx.qty)
                    put("balance", tx.balance)
                    put("note", tx.note)
                    put("unit", tx.unit)
                    put("created_at", tx.createdAt)
                }
                jsonArray.put(obj)
            }
            req = newRequestBuilder("transactions")
                .addHeader("Prefer", "return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            // 3. Fallback: Strip client_id completely if Supabase table does not have client_id column
            val jsonArrayNoClientId = org.json.JSONArray()
            for (tx in transactions) {
                val obj = org.json.JSONObject().apply {
                    if (tx.itemId != null) put("item_id", tx.itemId)
                    put("item_name", tx.itemName)
                    put("action", tx.action)
                    put("qty", tx.qty)
                    put("balance", tx.balance)
                    put("note", tx.note)
                    put("unit", tx.unit)
                    put("created_at", tx.createdAt)
                }
                jsonArrayNoClientId.put(obj)
            }
            req = newRequestBuilder("transactions")
                .addHeader("Prefer", "return=minimal")
                .post(jsonArrayNoClientId.toString().toRequestBody(jsonMediaType))
                .build()
            resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            val finalErr = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to insert transactions: $finalErr")
        } catch (e: Exception) {
            throw Exception("Failed to insert transactions: ${e.message ?: firstErr}")
        }
    }

    suspend fun clearTransactions(): Unit = withContext(Dispatchers.IO) {
        val req = newRequestBuilder("transactions?id=not.is.null")
            .delete()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to clear transactions: $err")
        }
    }

    suspend fun insertPurchase(purchase: PurchaseRecord): Unit = withContext(Dispatchers.IO) {
        val json = purchaseAdapter.toJson(purchase)
        val req = newRequestBuilder("purchases")
            .addHeader("Prefer", "return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            throw Exception("Failed to record purchase: $err")
        }
    }

    suspend fun fetchAllCashflow(): List<DailyCashflowRecord> = withContext(Dispatchers.IO) {
        try {
            val req = newRequestBuilder("daily_cashflow?select=*&order=date.desc&limit=2000")
                .get()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                Log.w(tag, "Fetch cashflow non-success: $err")
                return@withContext emptyList()
            }
            val body = resp.body?.string() ?: "[]"
            val jsonArr = JSONArray(body)
            val list = mutableListOf<DailyCashflowRecord>()
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(parseCashflowFromDb(obj))
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to load cashflow from remote", e)
            emptyList()
        }
    }

    suspend fun upsertCashflow(records: List<DailyCashflowRecord>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext

        try {
            // Build exact payload matching Hardware_website (id, date, type, category, amount, payment_mode, note, created_at)
            // No 'title' column in Supabase schema
            val jsonArray = JSONArray()
            for (rec in records) {
                val obj = JSONObject().apply {
                    put("id", rec.id)
                    put("date", rec.date.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) })
                    put("type", rec.type)
                    put("category", rec.category)
                    put("amount", rec.amount)
                    put("payment_mode", rec.paymentMode)
                    put("note", rec.note)
                    put("created_at", rec.createdAt.ifEmpty { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()) })
                }
                jsonArray.put(obj)
            }

            var req = newRequestBuilder("daily_cashflow?on_conflict=id")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            var resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            val err1 = resp.body?.string() ?: "HTTP ${resp.code}"
            Log.w(tag, "First attempt to upsert cashflow failed: $err1. Retrying with plain POST...")

            req = newRequestBuilder("daily_cashflow")
                .addHeader("Prefer", "return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            val err2 = resp.body?.string() ?: "HTTP ${resp.code}"
            if (resp.code == 404 || err2.contains("PGRST200") || err2.contains("does not exist")) {
                Log.w(tag, "Table daily_cashflow does not exist on Supabase: $err2")
                return@withContext
            }
            throw Exception("Failed to upsert cashflow: $err2")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST200") || msg.contains("404") || msg.contains("does not exist")) {
                Log.w(tag, "Table daily_cashflow not found on Supabase: $msg")
                return@withContext
            }
            throw e
        }
    }

    suspend fun deleteCashflow(ids: List<String>): Unit = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        try {
            val idsFormatted = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
            var req = newRequestBuilder("daily_cashflow?id=in.($idsFormatted)")
                .delete()
                .build()
            var resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            if (ids.size == 1) {
                val singleId = java.net.URLEncoder.encode(ids[0], "UTF-8")
                req = newRequestBuilder("daily_cashflow?id=eq.$singleId")
                    .delete()
                    .build()
                resp = okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) return@withContext
            }

            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            if (resp.code == 404 || err.contains("PGRST200") || err.contains("does not exist")) {
                return@withContext
            }
            throw Exception("Failed to delete cashflow: $err")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST200") || msg.contains("404") || msg.contains("does not exist")) {
                return@withContext
            }
            throw e
        }
    }

    // ==================== QUOTATIONS SYNC ====================
    suspend fun fetchAllQuotations(): List<QuotationRecord> = withContext(Dispatchers.IO) {
        try {
            val req = newRequestBuilder("quotations?select=*&order=created_at.desc&limit=1000")
                .get()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string() ?: "[]"
            val jsonArr = JSONArray(body)
            val list = mutableListOf<QuotationRecord>()
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(parseQuotationFromDb(obj))
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch quotations", e)
            emptyList()
        }
    }

    suspend fun upsertQuotations(records: List<QuotationRecord>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        try {
            val jsonArray = JSONArray()
            for (q in records) {
                val obj = JSONObject().apply {
                    put("id", q.id)
                    put("quotation_no", q.quotationNo)
                    put("customer_name", q.customerName)
                    put("customer_phone", q.customerPhone)
                    put("customer_address", q.customerAddress)
                    put("date", q.date)
                    put("valid_until", q.validUntil)
                    val itemsArray = try { JSONArray(q.itemsJson) } catch (e: Exception) { JSONArray() }
                    put("items", itemsArray)
                    put("subtotal", q.subtotal)
                    put("discount", q.discount)
                    put("tax_percent", q.taxPercent)
                    put("tax_amount", q.taxAmount)
                    put("grand_total", q.grandTotal)
                    put("status", q.status)
                    put("notes", q.notes)
                    put("created_at", q.createdAt)
                }
                jsonArray.put(obj)
            }
            val req = newRequestBuilder("quotations?on_conflict=id")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                Log.e(tag, "Failed to upsert quotations: $err")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to upsert quotations", e)
        }
    }

    suspend fun deleteQuotations(ids: List<String>): Unit = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val idsFormatted = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
        val req = newRequestBuilder("quotations?id=in.($idsFormatted)")
            .delete()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            Log.e(tag, "Failed to delete quotations: ${resp.body?.string()}")
        }
    }

    // ==================== LEDGER ACCOUNTS SYNC ====================
    suspend fun fetchAllLedgerAccounts(): List<LedgerAccount> = withContext(Dispatchers.IO) {
        try {
            val req = newRequestBuilder("ledger_accounts?select=*&order=name.asc&limit=1000")
                .get()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string() ?: "[]"
            val jsonArr = JSONArray(body)
            val list = mutableListOf<LedgerAccount>()
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(parseLedgerAccountFromDb(obj))
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch ledger accounts", e)
            emptyList()
        }
    }

    suspend fun upsertLedgerAccounts(records: List<LedgerAccount>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        try {
            val jsonArray = JSONArray()
            for (acc in records) {
                val obj = JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("phone", acc.phone)
                    put("address", acc.address)
                    put("type", acc.type)
                    put("net_balance", acc.netBalance)
                    put("credit_limit", acc.creditLimit)
                    put("notes", acc.notes)
                    put("created_at", acc.createdAt)
                    put("updated_at", acc.updatedAt)
                }
                jsonArray.put(obj)
            }
            val req = newRequestBuilder("ledger_accounts?on_conflict=id")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.e(tag, "Failed to upsert ledger accounts: ${resp.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to upsert ledger accounts", e)
        }
    }

    suspend fun deleteLedgerAccounts(ids: List<String>): Unit = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val idsFormatted = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
        val req = newRequestBuilder("ledger_accounts?id=in.($idsFormatted)")
            .delete()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            Log.e(tag, "Failed to delete ledger accounts: ${resp.body?.string()}")
        }
    }

    // ==================== LEDGER ENTRIES SYNC ====================
    suspend fun fetchAllLedgerEntries(): List<LedgerEntry> = withContext(Dispatchers.IO) {
        try {
            val req = newRequestBuilder("ledger_entries?select=*&order=created_at.desc&limit=5000")
                .get()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string() ?: "[]"
            val jsonArr = JSONArray(body)
            val list = mutableListOf<LedgerEntry>()
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(parseLedgerEntryFromDb(obj))
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch ledger entries", e)
            emptyList()
        }
    }

    suspend fun upsertLedgerEntries(records: List<LedgerEntry>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        try {
            val jsonArray = JSONArray()
            for (entry in records) {
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("account_id", entry.accountId)
                    put("type", entry.type)
                    put("amount", entry.amount)
                    put("balance_after", entry.balanceAfter)
                    put("date", entry.date)
                    put("description", entry.description)
                    put("bill_ref", entry.billRef)
                    put("created_at", entry.createdAt)
                }
                jsonArray.put(obj)
            }
            val req = newRequestBuilder("ledger_entries?on_conflict=id")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.e(tag, "Failed to upsert ledger entries: ${resp.body?.string()}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to upsert ledger entries", e)
        }
    }

    suspend fun deleteLedgerEntries(ids: List<String>): Unit = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val idsFormatted = ids.joinToString(",") { java.net.URLEncoder.encode(it, "UTF-8") }
        val req = newRequestBuilder("ledger_entries?id=in.($idsFormatted)")
            .delete()
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            Log.e(tag, "Failed to delete ledger entries: ${resp.body?.string()}")
        }
    }

    fun parseItemFromDb(obj: JSONObject): Item {
        val mrpVal = if (obj.has("mrp") && !obj.isNull("mrp")) {
            val v = obj.optDouble("mrp")
            if (v.isNaN()) null else v
        } else null

        return Item(
            id = obj.optString("id"),
            o = obj.optInt("o", 0),
            name = obj.optString("name"),
            code = obj.optString("code", ""),
            barcode = obj.optString("barcode", ""),
            type = obj.optString("type", ""),
            brand = obj.optString("brand", ""),
            size = obj.optString("size", ""),
            aliases = obj.optString("aliases", ""),
            mrp = mrpVal,
            cost = obj.optDouble("cost", 0.0),
            price = obj.optDouble("price", 0.0),
            qty = obj.optDouble("qty", 0.0),
            low = obj.optDouble("low", 0.0),
            unit = obj.optString("unit", "pcs").ifEmpty { "pcs" },
            updatedAt = obj.optString("updated_at", "")
        )
    }

    fun parseCashflowFromDb(obj: JSONObject): DailyCashflowRecord {
        val cat = obj.optString("category", "General")
        val titleVal = obj.optString("title", cat).ifBlank { cat }
        return DailyCashflowRecord(
            id = obj.optString("id"),
            type = obj.optString("type", "SALE"),
            amount = obj.optDouble("amount", 0.0),
            title = titleVal,
            category = cat,
            paymentMode = obj.optString("payment_mode", obj.optString("paymentMode", "Cash")).ifEmpty { "Cash" },
            date = obj.optString("date", ""),
            note = obj.optString("note", ""),
            createdAt = obj.optString("created_at", obj.optString("createdAt", ""))
        )
    }

    fun parseQuotationFromDb(obj: JSONObject): QuotationRecord {
        val itemsArray = when {
            obj.has("items") && !obj.isNull("items") -> obj.get("items").toString()
            obj.has("items_json") && !obj.isNull("items_json") -> obj.get("items_json").toString()
            else -> "[]"
        }
        return QuotationRecord(
            id = obj.optString("id"),
            quotationNo = obj.optString("quotation_no", obj.optString("quotationNo", "")),
            customerName = obj.optString("customer_name", obj.optString("customerName", "")),
            customerPhone = obj.optString("customer_phone", obj.optString("customerPhone", "")),
            customerAddress = obj.optString("customer_address", obj.optString("customerAddress", "")),
            date = obj.optString("date", ""),
            validUntil = obj.optString("valid_until", obj.optString("validUntil", "")),
            itemsJson = itemsArray,
            subtotal = obj.optDouble("subtotal", 0.0),
            discount = obj.optDouble("discount", 0.0),
            taxPercent = obj.optDouble("tax_percent", obj.optDouble("taxPercent", 0.0)),
            taxAmount = obj.optDouble("tax_amount", obj.optDouble("taxAmount", 0.0)),
            grandTotal = obj.optDouble("grand_total", obj.optDouble("grandTotal", 0.0)),
            status = obj.optString("status", "Draft"),
            notes = obj.optString("notes", ""),
            createdAt = obj.optString("created_at", obj.optString("createdAt", ""))
        )
    }

    fun parseLedgerAccountFromDb(obj: JSONObject): LedgerAccount {
        return LedgerAccount(
            id = obj.optString("id"),
            name = obj.optString("name"),
            phone = obj.optString("phone", ""),
            address = obj.optString("address", ""),
            type = obj.optString("type", "CUSTOMER"),
            netBalance = obj.optDouble("net_balance", obj.optDouble("netBalance", 0.0)),
            creditLimit = obj.optDouble("credit_limit", obj.optDouble("creditLimit", 0.0)),
            notes = obj.optString("notes", ""),
            createdAt = obj.optString("created_at", obj.optString("createdAt", "")),
            updatedAt = obj.optString("updated_at", obj.optString("updatedAt", ""))
        )
    }

    fun parseLedgerEntryFromDb(obj: JSONObject): LedgerEntry {
        return LedgerEntry(
            id = obj.optString("id"),
            accountId = obj.optString("account_id", obj.optString("accountId", "")),
            type = obj.optString("type", "GAVE"),
            amount = obj.optDouble("amount", 0.0),
            balanceAfter = obj.optDouble("balance_after", obj.optDouble("balanceAfter", 0.0)),
            date = obj.optString("date", ""),
            description = obj.optString("description", ""),
            billRef = obj.optString("bill_ref", obj.optString("billRef", "")),
            createdAt = obj.optString("created_at", obj.optString("createdAt", ""))
        )
    }

    // Realtime WebSocket support matching Phoenix channels protocol and Hardware_website
    fun connectRealtime(
        coroutineScope: CoroutineScope,
        onStatusChanged: (Boolean) -> Unit,
        onItemChanged: (type: String, item: Item?, oldId: String?) -> Unit,
        onCashflowChanged: ((type: String, cashflow: DailyCashflowRecord?, oldId: String?) -> Unit)? = null,
        onQuotationChanged: ((type: String, quotation: QuotationRecord?, oldId: String?) -> Unit)? = null,
        onLedgerAccountChanged: ((type: String, account: LedgerAccount?, oldId: String?) -> Unit)? = null,
        onLedgerEntryChanged: ((type: String, entry: LedgerEntry?, oldId: String?) -> Unit)? = null,
        onClosedOrFailed: () -> Unit
    ): WebSocket? {
        val wsUrl = baseUrl.replace("https://", "wss://") + "/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"
        val request = Request.Builder().url(wsUrl).build()

        var webSocket: WebSocket? = null
        var heartbeatJob: Job? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "Realtime WS connected to Supabase")
                val tables = listOf("items", "purchases", "daily_cashflow", "quotations", "ledger_accounts", "ledger_entries")
                tables.forEach { table ->
                    val joinMsg = JSONObject().apply {
                        put("topic", "realtime:public:$table")
                        put("event", "phx_join")
                        put("ref", "join_${table}_${System.currentTimeMillis()}")
                        put("payload", JSONObject().apply {
                            put("config", JSONObject().apply {
                                put("broadcast", JSONObject().apply { put("self", false) })
                                put("presence", JSONObject().apply { put("key", "") })
                                put("postgres_changes", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", table)
                                    })
                                })
                            })
                        })
                    }
                    ws.send(joinMsg.toString())
                }

                // Start Phoenix heartbeat loop every 20 seconds
                heartbeatJob?.cancel()
                heartbeatJob = coroutineScope.launch {
                    var hbCounter = 1L
                    while (isActive) {
                        delay(20_000)
                        val hbMsg = JSONObject().apply {
                            put("topic", "phoenix")
                            put("event", "heartbeat")
                            put("payload", JSONObject())
                            put("ref", "hb_${hbCounter++}")
                        }
                        ws.send(hbMsg.toString())
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val event = root.optString("event")
                    val payload = root.optJSONObject("payload")
                    val topic = root.optString("topic")

                    if (event == "phx_reply" && payload?.optString("status") == "ok") {
                        if (topic.contains("items")) {
                            Log.d(tag, "Subscribed to realtime items successfully")
                            onStatusChanged(true)
                        }
                    } else if (event == "system" && payload?.optString("status") == "ok") {
                        Log.d(tag, "Realtime system subscription confirmed")
                        onStatusChanged(true)
                    } else if (event == "postgres_changes") {
                        val dataObj = payload?.optJSONObject("data") ?: payload
                        val table = dataObj?.optString("table") ?: payload?.optString("table") ?: ""
                        val type = dataObj?.optString("type") ?: ""
                        val record = dataObj?.optJSONObject("record") ?: dataObj?.optJSONObject("new")
                        val oldRecord = dataObj?.optJSONObject("old_record") ?: dataObj?.optJSONObject("old")

                        if (topic.contains("daily_cashflow") || table == "daily_cashflow") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val cashflow = parseCashflowFromDb(it)
                                    onCashflowChanged?.invoke(type, cashflow, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onCashflowChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (topic.contains("quotations") || table == "quotations") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val q = parseQuotationFromDb(it)
                                    onQuotationChanged?.invoke(type, q, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onQuotationChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (topic.contains("ledger_accounts") || table == "ledger_accounts") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val a = parseLedgerAccountFromDb(it)
                                    onLedgerAccountChanged?.invoke(type, a, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onLedgerAccountChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (topic.contains("ledger_entries") || table == "ledger_entries") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val e = parseLedgerEntryFromDb(it)
                                    onLedgerEntryChanged?.invoke(type, e, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onLedgerEntryChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (topic.contains("items") || table == "items") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val item = parseItemFromDb(it)
                                    onItemChanged(type, item, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onItemChanged("DELETE", null, oldId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing realtime message: $text", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Realtime WS closing: $code / $reason")
                heartbeatJob?.cancel()
                onStatusChanged(false)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Realtime WS closed: $code / $reason")
                heartbeatJob?.cancel()
                onStatusChanged(false)
                onClosedOrFailed()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "Realtime WS failure: ${t.message}")
                heartbeatJob?.cancel()
                onStatusChanged(false)
                onClosedOrFailed()
            }
        }

        webSocket = wsHttpClient.newWebSocket(request, listener)
        return webSocket
    }
}

