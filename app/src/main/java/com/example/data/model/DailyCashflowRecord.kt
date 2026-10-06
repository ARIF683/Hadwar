package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "daily_cashflow")
@JsonClass(generateAdapter = true)
data class DailyCashflowRecord(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @Json(name = "id")
    val id: String,

    @ColumnInfo(name = "type")
    @Json(name = "type")
    val type: String, // "SALE" or "EXPENSE"

    @ColumnInfo(name = "amount")
    @Json(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "title")
    @Json(name = "title")
    val title: String, // e.g. "Counter Sale", "Hardware Items", "Shop Rent", "Electricity Bill"

    @ColumnInfo(name = "category")
    @Json(name = "category")
    val category: String, // e.g. "Counter Sale", "Wholesale", "Rent", "Utilities", "Salary", "Transport", "Maintenance", "Tea/Snacks", "Other"

    @ColumnInfo(name = "payment_mode")
    @Json(name = "payment_mode")
    val paymentMode: String = "Cash", // "Cash", "UPI", "Card", "Bank Transfer", "Cheque"

    @ColumnInfo(name = "date")
    @Json(name = "date")
    val date: String, // "YYYY-MM-DD" e.g. "2026-10-05"

    @ColumnInfo(name = "note")
    @Json(name = "note")
    val note: String = "",

    @ColumnInfo(name = "created_at")
    @Json(name = "created_at")
    val createdAt: String
)
