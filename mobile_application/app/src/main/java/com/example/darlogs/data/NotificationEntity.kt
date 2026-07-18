package com.example.darlogs.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val user_id: Int,
    val is_read: Int,
    val message: String,
    val record_id: Int?,
    val sender_id: Int?,
    val type: String,
    val created_at: String
)
