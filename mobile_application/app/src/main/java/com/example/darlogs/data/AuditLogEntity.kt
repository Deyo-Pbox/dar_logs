package com.example.darlogs.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey val id: Int,
    val user_id: Int?,
    val username: String,
    val action: String,
    val table_name: String?,
    val record_id: Int?,
    val details: String?,
    val created_at: String
)
