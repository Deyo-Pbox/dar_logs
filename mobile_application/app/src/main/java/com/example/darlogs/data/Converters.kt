package com.example.darlogs.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus {
        return try {
            if (value == null) SyncStatus.SYNCED 
            else SyncStatus.valueOf(value)
        } catch (e: Exception) {
            SyncStatus.SYNCED
        }
    }
}
