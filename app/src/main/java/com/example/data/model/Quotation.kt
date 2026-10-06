package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuotationLineItem(
    val itemId: String? = null,
    val name: String,
    val code: String = "",
    val type: String = "",
    val unit: String = "pcs",
    val qty: Double = 1.0,
    val unitPrice: Double = 0.0,
    val discountPercent: Double = 0.0,
    val total: Double = 0.0
)

@Entity(tableName = "quotations")
@JsonClass(generateAdapter = true)
data class QuotationRecord(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "quotation_no")
    val quotationNo: String,

    @ColumnInfo(name = "customer_name")
    val customerName: String,

    @ColumnInfo(name = "customer_phone")
    val customerPhone: String = "",

    @ColumnInfo(name = "customer_address")
    val customerAddress: String = "",

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "valid_until")
    val validUntil: String = "",

    @ColumnInfo(name = "items_json")
    val itemsJson: String,

    @ColumnInfo(name = "subtotal")
    val subtotal: Double,

    @ColumnInfo(name = "discount")
    val discount: Double = 0.0,

    @ColumnInfo(name = "tax_percent")
    val taxPercent: Double = 0.0,

    @ColumnInfo(name = "tax_amount")
    val taxAmount: Double = 0.0,

    @ColumnInfo(name = "grand_total")
    val grandTotal: Double,

    @ColumnInfo(name = "status")
    val status: String = "Draft", // Draft, Sent, Accepted, Converted

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
