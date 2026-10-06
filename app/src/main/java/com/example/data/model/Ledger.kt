package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "ledger_accounts")
@JsonClass(generateAdapter = true)
data class LedgerAccount(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @Json(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    @Json(name = "name")
    val name: String,

    @ColumnInfo(name = "phone")
    @Json(name = "phone")
    val phone: String = "",

    @ColumnInfo(name = "address")
    @Json(name = "address")
    val address: String = "",

    @ColumnInfo(name = "type")
    @Json(name = "type")
    val type: String, // "SUPPLIER" or "CUSTOMER" (Contractors are listed under CUSTOMER/CONTRACTOR)

    @ColumnInfo(name = "net_balance")
    @Json(name = "net_balance")
    val netBalance: Double = 0.0, // For Customer: >0 is You Will Get (Receivable), <0 is You Will Give. For Supplier: >0 is You Will Pay (Payable)

    @ColumnInfo(name = "credit_limit")
    @Json(name = "credit_limit")
    val creditLimit: Double = 0.0,

    @ColumnInfo(name = "notes")
    @Json(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    @Json(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @Json(name = "updated_at")
    val updatedAt: String
)

@Entity(tableName = "ledger_entries")
@JsonClass(generateAdapter = true)
data class LedgerEntry(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @Json(name = "id")
    val id: String,

    @ColumnInfo(name = "account_id")
    @Json(name = "account_id")
    val accountId: String,

    @ColumnInfo(name = "type")
    @Json(name = "type")
    val type: String, // "GAVE" (Debit / Sold / Paid Out) or "GOT" (Credit / Payment Received / Goods Received)

    @ColumnInfo(name = "amount")
    @Json(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "balance_after")
    @Json(name = "balance_after")
    val balanceAfter: Double = 0.0,

    @ColumnInfo(name = "date")
    @Json(name = "date")
    val date: String,

    @ColumnInfo(name = "description")
    @Json(name = "description")
    val description: String = "",

    @ColumnInfo(name = "bill_ref")
    @Json(name = "bill_ref")
    val billRef: String = "",

    @ColumnInfo(name = "created_at")
    @Json(name = "created_at")
    val createdAt: String
)
