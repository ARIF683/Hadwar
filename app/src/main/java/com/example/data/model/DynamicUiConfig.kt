package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DynamicUiConfig(
    val theme: ThemeConfig = ThemeConfig(),
    val announcement: AnnouncementConfig = AnnouncementConfig(),
    val features: FeaturesConfig = FeaturesConfig()
)

@JsonClass(generateAdapter = true)
data class ThemeConfig(
    val primaryColor: String = "#2563EB",
    val accentColor: String = "#1D4ED8",
    val storeTitle: String = "Hardware & Tools Hub",
    val tagline: String = "Real-time Inventory & Smart Billing"
)

@JsonClass(generateAdapter = true)
data class AnnouncementConfig(
    val enabled: Boolean = false,
    val message: String = "",
    val severity: String = "info", // "info", "warning", "success", "alert"
    val dismissible: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FeaturesConfig(
    val showQuickActions: Boolean = true,
    val showLowStockAlert: Boolean = true,
    val showRecentTransactions: Boolean = true,
    val allowDirectBilling: Boolean = true
)
