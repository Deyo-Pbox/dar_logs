package com.example.darlogs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"])]
)
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val password: String, // Matches MySQL column name exactly
    val role: String,
    val approved: Int,
    @ColumnInfo(name = "created_at") val created_at: String?,
    @ColumnInfo(name = "updated_at") val updated_at: String?,
    @ColumnInfo(name = "last_activity") val last_activity: String?
)
