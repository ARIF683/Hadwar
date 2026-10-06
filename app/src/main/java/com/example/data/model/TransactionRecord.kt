package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "transactions")
@JsonClass(generateAdapter = true)
data class TransactionRecord(
    @PrimaryKey
    @ColumnInfo(name = "client_id")
    @Json(name = "client_id")
    val clientId: String,

    @ColumnInfo(name = "item_id")
    @Json(name = "item_id")
    val itemId: String? = null,

    @ColumnInfo(name = "item_name")
    @Json(name = "item_name")
    val itemName: String,

    @ColumnInfo(name = "action")
    @Json(name = "action")
    val action: String, // "in" or "out"

    @ColumnInfo(name = "qty")
    @Json(name = "qty")
    val qty: Double,

    @ColumnInfo(name = "balance")
    @Json(name = "balance")
    val balance: Double,

    @ColumnInfo(name = "note")
    @Json(name = "note")
    val note: String = "",

    @ColumnInfo(name = "unit")
    @Json(name = "unit")
    val unit: String = "pcs",

    @ColumnInfo(name = "created_at")
    @Json(name = "created_at")
    val createdAt: String
)
