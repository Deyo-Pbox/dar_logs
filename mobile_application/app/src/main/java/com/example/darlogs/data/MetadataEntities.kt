package com.example.darlogs.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_stats")
data class StatsEntity(
    @PrimaryKey val id: Int = 1,
    val activeRecords: Int,
    val archivedRecords: Int,
    val totalUsers: Int,
    val recentEdits: Int,
    val activeUsers: Int
)

@Entity(tableName = "route_users")
data class RouteUserEntity(
    @PrimaryKey val id: Int,
    val username: String
)
