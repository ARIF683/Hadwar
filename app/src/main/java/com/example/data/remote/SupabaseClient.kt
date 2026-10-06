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
            val req = newRequestBuilder("daily_cashflow?select=*&order=created_at.desc&limit=2000")
                .get()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: "HTTP ${resp.code}"
                Log.w(tag, "Fetch cashflow non-success: $err")
                return@withContext emptyList()
            }
            val body = resp.body?.string() ?: "[]"
            cashflowListAdapter.fromJson(body) ?: emptyList()
        } catch (e: Exception) {
            Log.w(tag, "Failed to load cashflow from remote", e)
            emptyList()
        }
    }

    suspend fun upsertCashflow(records: List<DailyCashflowRecord>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext

        try {
            // 1. Try on_conflict=id upsert first
            val json = cashflowListAdapter.toJson(records)
            var req = newRequestBuilder("daily_cashflow?on_conflict=id")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(json.toRequestBody(jsonMediaType))
                .build()
            var resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            val err1 = resp.body?.string() ?: "HTTP ${resp.code}"
            Log.w(tag, "First attempt to upsert cashflow failed: $err1. Trying plain POST...")

            // 2. Try plain POST without on_conflict (in case no unique constraint on id)
            req = newRequestBuilder("daily_cashflow")
                .addHeader("Prefer", "return=minimal")
                .post(json.toRequestBody(jsonMediaType))
                .build()
            resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            val err2 = resp.body?.string() ?: "HTTP ${resp.code}"
            Log.w(tag, "Second attempt to insert cashflow failed: $err2. Trying explicit JSON payload...")

            // 3. Explicit JSON formatting for Postgres column names
            val jsonArray = JSONArray()
            for (rec in records) {
                val obj = JSONObject().apply {
                    put("id", rec.id)
                    put("type", rec.type)
                    put("amount", rec.amount)
                    put("title", rec.title)
                    put("category", rec.category)
                    put("payment_mode", rec.paymentMode)
                    put("date", rec.date)
                    put("note", rec.note)
                    put("created_at", rec.createdAt)
                }
                jsonArray.put(obj)
            }
            req = newRequestBuilder("daily_cashflow")
                .addHeader("Prefer", "return=minimal")
                .post(jsonArray.toString().toRequestBody(jsonMediaType))
                .build()
            resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) return@withContext

            val err3 = resp.body?.string() ?: "HTTP ${resp.code}"
            if (resp.code == 404 || err3.contains("PGRST200") || err3.contains("does not exist")) {
                Log.w(tag, "Table daily_cashflow does not exist on Supabase: $err3")
                return@withContext
            }
            throw Exception("Failed to upsert cashflow: $err3")
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

            val err1 = resp.body?.string() ?: "HTTP ${resp.code}"

            // Fallback for single ID: id=eq.xxx
            if (ids.size == 1) {
                val singleId = java.net.URLEncoder.encode(ids[0], "UTF-8")
                req = newRequestBuilder("daily_cashflow?id=eq.$singleId")
                    .delete()
                    .build()
                resp = okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) return@withContext
            }

            val err2 = resp.body?.string() ?: "HTTP ${resp.code}"
            if (resp.code == 404 || err2.contains("PGRST200") || err2.contains("does not exist")) {
                Log.w(tag, "Table daily_cashflow does not exist on Supabase: $err2")
                return@withContext
            }
            throw Exception("Failed to delete cashflow: $err2")
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("PGRST200") || msg.contains("404") || msg.contains("does not exist")) {
                Log.w(tag, "Table daily_cashflow not found on Supabase: $msg")
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
            quotationListAdapter.fromJson(body) ?: emptyList()
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch quotations", e)
            emptyList()
        }
    }

    suspend fun upsertQuotations(records: List<QuotationRecord>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        val json = quotationListAdapter.toJson(records)
        val req = newRequestBuilder("quotations?on_conflict=id")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "HTTP ${resp.code}"
            Log.e(tag, "Failed to upsert quotations: $err")
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
            ledgerAccountListAdapter.fromJson(body) ?: emptyList()
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch ledger accounts", e)
            emptyList()
        }
    }

    suspend fun upsertLedgerAccounts(records: List<LedgerAccount>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        val json = ledgerAccountListAdapter.toJson(records)
        val req = newRequestBuilder("ledger_accounts?on_conflict=id")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            Log.e(tag, "Failed to upsert ledger accounts: ${resp.body?.string()}")
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
            ledgerEntryListAdapter.fromJson(body) ?: emptyList()
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch ledger entries", e)
            emptyList()
        }
    }

    suspend fun upsertLedgerEntries(records: List<LedgerEntry>): Unit = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        val json = ledgerEntryListAdapter.toJson(records)
        val req = newRequestBuilder("ledger_entries?on_conflict=id")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        val resp = okHttpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            Log.e(tag, "Failed to upsert ledger entries: ${resp.body?.string()}")
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
        return DailyCashflowRecord(
            id = obj.optString("id"),
            type = obj.optString("type"),
            amount = obj.optDouble("amount", 0.0),
            title = obj.optString("title"),
            category = obj.optString("category"),
            paymentMode = obj.optString("payment_mode", "Cash").ifEmpty { "Cash" },
            date = obj.optString("date"),
            note = obj.optString("note", ""),
            createdAt = obj.optString("created_at", "")
        )
    }

    // Realtime WebSocket support matching Phoenix channels protocol
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
                val joinMsg = JSONObject().apply {
                    put("topic", "realtime:public:items")
                    put("event", "phx_join")
                    put("ref", "join_${System.currentTimeMillis()}")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            put("broadcast", JSONObject().apply { put("self", false) })
                            put("presence", JSONObject().apply { put("key", "") })
                            val changeArr = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "items")
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "daily_cashflow")
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "quotations")
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "ledger_accounts")
                                })
                                put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", "ledger_entries")
                                })
                            }
                            put("postgres_changes", changeArr)
                        })
                    })
                }
                ws.send(joinMsg.toString())

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

                    if (event == "phx_reply" && payload?.optString("status") == "ok") {
                        val topic = root.optString("topic")
                        if (topic == "realtime:public:items") {
                            Log.d(tag, "Subscribed to realtime:public:items successfully")
                            onStatusChanged(true)
                        }
                    } else if (event == "system" && payload?.optString("status") == "ok") {
                        Log.d(tag, "Realtime system subscription confirmed")
                        onStatusChanged(true)
                    } else if (event == "postgres_changes") {
                        val dataObj = payload?.optJSONObject("data") ?: payload
                        val table = dataObj?.optString("table") ?: payload?.optString("table") ?: ""
                        val type = dataObj?.optString("type") ?: ""
                        val record = dataObj?.optJSONObject("record")
                        val oldRecord = dataObj?.optJSONObject("old_record")

                        if (table == "daily_cashflow") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val cashflow = parseCashflowFromDb(it)
                                    onCashflowChanged?.invoke(type, cashflow, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onCashflowChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (table == "quotations") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val q = moshi.adapter(QuotationRecord::class.java).fromJson(it.toString())
                                    onQuotationChanged?.invoke(type, q, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onQuotationChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (table == "ledger_accounts") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val a = moshi.adapter(LedgerAccount::class.java).fromJson(it.toString())
                                    onLedgerAccountChanged?.invoke(type, a, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onLedgerAccountChanged?.invoke("DELETE", null, oldId)
                            }
                        } else if (table == "ledger_entries") {
                            if (type == "INSERT" || type == "UPDATE") {
                                record?.let {
                                    val e = moshi.adapter(LedgerEntry::class.java).fromJson(it.toString())
                                    onLedgerEntryChanged?.invoke(type, e, null)
                                }
                            } else if (type == "DELETE") {
                                val oldId = record?.optString("id") ?: oldRecord?.optString("id")
                                onLedgerEntryChanged?.invoke("DELETE", null, oldId)
                            }
                        } else {
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
