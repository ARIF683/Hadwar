package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PurchaseLine(
    @Json(name = "item_id")
    val itemId: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "qty")
    val qty: Double,
    @Json(name = "rate")
    val rate: Double,
    @Json(name = "bill_name")
    val billName: String,
    @Json(name = "created")
    val created: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PurchaseRecord(
    @Json(name = "supplier")
    val supplier: String = "",
    @Json(name = "bill_no")
    val billNo: String = "",
    @Json(name = "bill_date")
    val billDate: String = "",
    @Json(name = "total")
    val total: Double = 0.0,
    @Json(name = "lines")
    val lines: List<PurchaseLine> = emptyList()
)
