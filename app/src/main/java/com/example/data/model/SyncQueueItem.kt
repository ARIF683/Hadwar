package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "upsert", "patch", "delete", "deleteAll", "tx", "clearTx", "purchase"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
