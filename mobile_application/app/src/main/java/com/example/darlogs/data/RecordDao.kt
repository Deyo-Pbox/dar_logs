package com.example.darlogs.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Query("SELECT * FROM activity_logs WHERE isArchived = 0 ORDER BY localId DESC")
    fun getAllActiveRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM activity_logs WHERE isArchived = 1 ORDER BY localId DESC")
    fun getAllArchivedRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM activity_logs WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncRecords(): List<RecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<RecordEntity>)

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("DELETE FROM activity_logs WHERE id = :id")
    suspend fun deleteByRemoteId(id: Int)

    @Query("SELECT * FROM activity_logs WHERE id = :id LIMIT 1")
    suspend fun getByRemoteId(id: Int): RecordEntity?

    @Query("SELECT * FROM activity_logs WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Long): RecordEntity?

    @Query("DELETE FROM activity_logs WHERE syncStatus = 'SYNCED'")
    suspend fun clearSyncedRecords()

    @Query("DELETE FROM activity_logs WHERE isArchived = 1")
    suspend fun clearArchivedRecords()

    // Dashboard Stats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStats(stats: StatsEntity)

    @Query("SELECT * FROM dashboard_stats WHERE id = 1")
    fun getStats(): Flow<StatsEntity?>

    // Route Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteUsers(users: List<RouteUserEntity>)

    @Query("DELETE FROM route_users")
    suspend fun clearRouteUsers()

    @Query("SELECT * FROM route_users ORDER BY username ASC")
    fun getAllRouteUsers(): Flow<List<RouteUserEntity>>

    // Notifications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    // Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // Audit Log
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    @Query("SELECT * FROM audit_log ORDER BY created_at DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("DELETE FROM audit_log")
    suspend fun clearAuditLogs()
}
