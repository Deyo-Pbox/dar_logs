package com.example.darlogs.data

import android.content.Context
import android.util.Log
import com.example.darlogs.ApiClient
import com.example.darlogs.R
import com.example.darlogs.ui.DashboardStats
import com.example.darlogs.ui.NewRecordInput
import com.example.darlogs.ui.RecordItem
import com.example.darlogs.ui.RouteToUserOption
import com.example.darlogs.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RecordRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val recordDao = database.recordDao()

    val activeRecords: Flow<List<RecordItem>> = recordDao.getAllActiveRecords()
        .map { entities -> 
            entities.filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                   .map { it.toRecordItem() } 
        }

    val archivedRecords: Flow<List<RecordItem>> = recordDao.getAllArchivedRecords()
        .map { entities -> 
            entities.filter { it.syncStatus != SyncStatus.PENDING_DELETE }
                   .map { it.toRecordItem() } 
        }

    val dashboardStats: Flow<DashboardStats> = recordDao.getStats()
        .map { entity ->
            entity?.let {
                DashboardStats(
                    activeRecords = it.activeRecords,
                    archivedRecords = it.archivedRecords,
                    totalUsers = it.totalUsers,
                    recentEdits = it.recentEdits,
                    activeUsers = it.activeUsers
                )
            } ?: DashboardStats()
        }

    val routeToUsers: Flow<List<RouteToUserOption>> = recordDao.getAllRouteUsers()
        .map { entities ->
            entities.map { RouteToUserOption(id = it.id, label = it.username) }
        }

    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isOnline(context)) return@withContext
        try {
            refreshRecords()
            refreshArchivedRecords()
            refreshStats()
            refreshRouteUsers()
            refreshNotifications()
            refreshUsers()
            refreshAuditLogs()
        } catch (e: Exception) {
            Log.e("RecordRepository", "Error refreshing all data", e)
        }
    }

    suspend fun refreshRecords() = withContext(Dispatchers.IO) {
        val url = "${context.getString(R.string.records_api_url)}?scope=all"
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val data = response.json.optJSONArray("data") ?: JSONArray()
            val entities = mutableListOf<RecordEntity>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.let { obj ->
                    entities.add(parseJsonToRecordEntity(obj, isArchived = false))
                }
            }
            recordDao.clearSyncedRecords()
            recordDao.insertRecords(entities)
        }
    }

    suspend fun refreshArchivedRecords() = withContext(Dispatchers.IO) {
        val url = "${context.getString(R.string.archive_api_url)}?scope=all"
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val data = response.json.optJSONArray("data") ?: JSONArray()
            val entities = mutableListOf<RecordEntity>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.let { obj ->
                    entities.add(parseJsonToRecordEntity(obj, isArchived = true))
                }
            }
            recordDao.clearArchivedRecords()
            recordDao.insertRecords(entities)
        }
    }

    suspend fun refreshStats() = withContext(Dispatchers.IO) {
        val url = context.getString(R.string.dashboard_api_url)
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val statsJson = response.json.optJSONObject("stats") ?: JSONObject()
            val entity = StatsEntity(
                activeRecords = statsJson.optInt("total_records"),
                archivedRecords = statsJson.optInt("archived_records"),
                totalUsers = statsJson.optInt("total_users"),
                recentEdits = statsJson.optInt("recent_edits"),
                activeUsers = statsJson.optInt("active_users")
            )
            recordDao.updateStats(entity)
        }
    }

    suspend fun refreshRouteUsers() = withContext(Dispatchers.IO) {
        val url = context.getString(R.string.route_users_api_url)
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val data = response.json.optJSONArray("data") ?: JSONArray()
            val users = mutableListOf<RouteUserEntity>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.let { obj ->
                    users.add(RouteUserEntity(id = obj.optInt("id"), username = obj.optString("username")))
                }
            }
            recordDao.clearRouteUsers()
            recordDao.insertRouteUsers(users)
        }
    }

    suspend fun refreshNotifications() = withContext(Dispatchers.IO) {
        val url = "${context.getString(R.string.notifications_api_url)}?scope=all"
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val data = response.json.optJSONArray("data") ?: JSONArray()
            val notifications = mutableListOf<NotificationEntity>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.let { obj ->
                    notifications.add(NotificationEntity(
                        id = obj.optInt("id"),
                        user_id = obj.optInt("user_id"),
                        type = obj.optString("type"),
                        record_id = if (obj.isNull("record_id")) null else obj.optInt("record_id"),
                        sender_id = if (obj.isNull("sender_id")) null else obj.optInt("sender_id"),
                        message = obj.optString("message"),
                        is_read = if (obj.optBoolean("is_read")) 1 else obj.optInt("is_read"),
                        created_at = obj.optString("created_at")
                    ))
                }
            }
            recordDao.clearNotifications()
            recordDao.insertNotifications(notifications)
        }
    }

    suspend fun refreshUsers() = withContext(Dispatchers.IO) {
        val url = context.getString(R.string.users_api_url)
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val data = response.json.optJSONArray("data") ?: JSONArray()
            val users = mutableListOf<UserEntity>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.let { obj ->
                    users.add(UserEntity(
                        id = obj.optInt("id"),
                        username = obj.optString("username"),
                        password = obj.optString("password"),
                        role = obj.optString("role"),
                        approved = obj.optInt("approved"),
                        created_at = obj.optString("created_at"),
                        updated_at = obj.optString("updated_at"),
                        last_activity = obj.optString("last_activity")
                    ))
                }
            }
            recordDao.clearUsers()
            recordDao.insertUsers(users)
        }
    }

    suspend fun refreshAuditLogs() = withContext(Dispatchers.IO) {
        val url = context.getString(R.string.audit_api_url)
        val response = ApiClient.getJson(url)
        if (response.success && response.json != null) {
            val data = response.json.optJSONArray("data") ?: JSONArray()
            val logs = mutableListOf<AuditLogEntity>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.let { obj ->
                    logs.add(AuditLogEntity(
                        id = obj.optInt("id"),
                        user_id = if (obj.isNull("user_id")) null else obj.optInt("user_id"),
                        username = obj.optString("username"),
                        action = obj.optString("action"),
                        table_name = obj.optString("table_name"),
                        record_id = if (obj.isNull("record_id")) null else obj.optInt("record_id"),
                        details = obj.optString("details"),
                        created_at = obj.optString("created_at")
                    ))
                }
            }
            recordDao.clearAuditLogs()
            recordDao.insertAuditLogs(logs)
        }
    }

    suspend fun authenticateOffline(username: String, passwordRaw: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            val user = recordDao.getUserByUsername(username) ?: return@withContext null
            if (user.password.isEmpty()) return@withContext null

            val result = at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(passwordRaw.toCharArray(), user.password)
            if (result.verified) return@withContext user
        } catch (e: Exception) {
            Log.e("RecordRepository", "Offline auth error", e)
        }
        return@withContext null
    }

    private fun parseJsonToRecordEntity(obj: JSONObject, isArchived: Boolean): RecordEntity {
        return RecordEntity(
            id = obj.optInt("id"),
            municipality = obj.optString("municipality"),
            lo_claimant = obj.optString("lo_claimant"),
            title_no = obj.optString("title_no"),
            odts_no = obj.optString("odts_no"),
            lot_no = obj.optString("lot_no"),
            survey_no = obj.optString("survey_no"),
            area_has = obj.optString("area_has"),
            location = obj.optString("location"),
            transmitted_documents = obj.optString("transmitted_documents"),
            route_to = obj.optString("route_to"),
            route_to_user_id = if (obj.isNull("route_to_user_id")) null else obj.optInt("route_to_user_id"),
            routed_from_user_id = if (obj.isNull("routed_from_user_id")) null else obj.optInt("routed_from_user_id"),
            routed_at = if (obj.isNull("routed_at")) null else obj.optString("routed_at"),
            received_by_control_no = obj.optString("received_by_control_no"),
            work_status = obj.optString("work_status"),
            remarks_action_taken = obj.optString("remarks_action_taken"),
            updated_by_name = obj.optString("updated_by_name", obj.optString("created_by_name", "System")),
            updated_by = if (obj.isNull("updated_by")) null else obj.optInt("updated_by"),
            created_by = if (obj.isNull("created_by")) null else obj.optInt("created_by"),
            created_at = obj.optString("created_at"),
            updated_at = obj.optString("updated_at"),
            archived_at = if (obj.isNull("archived_at")) null else obj.optString("archived_at"),
            archived_by = if (obj.isNull("archived_by")) null else obj.optInt("archived_by"),
            isArchived = isArchived,
            syncStatus = SyncStatus.SYNCED
        )
    }

    suspend fun saveRecord(input: NewRecordInput): Boolean = withContext(Dispatchers.IO) {
        val existing = input.id?.let { recordDao.getByRemoteId(it) }
        
        val entity = RecordEntity(
            localId = existing?.localId ?: 0,
            id = input.id,
            municipality = input.municipality,
            lo_claimant = input.claimant,
            title_no = input.titleNo,
            odts_no = input.odtsNo,
            lot_no = input.lotNo,
            survey_no = input.surveyNo,
            area_has = input.areaHas,
            location = input.location,
            transmitted_documents = input.transmittedDocuments,
            route_to = input.routeTo,
            route_to_user_id = input.routeToUserId,
            received_by_control_no = input.receivedByControlNo,
            work_status = input.workStatus,
            remarks_action_taken = input.remarks,
            updated_by_name = existing?.updated_by_name ?: "Me",
            syncStatus = if (input.id == null) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        )

        val localId = if (existing != null) {
            recordDao.updateRecord(entity)
            existing.localId
        } else {
            recordDao.insertRecord(entity)
        }
        
        if (NetworkUtils.isOnline(context)) {
            return@withContext syncSingleRecord(entity.copy(localId = localId))
        }
        return@withContext true
    }

    suspend fun syncSingleRecord(entity: RecordEntity): Boolean {
        val url = context.getString(R.string.records_api_url)
        val payload = JSONObject().apply {
            if (entity.id != null) put("id", entity.id)
            put("municipality", entity.municipality)
            put("lo_claimant", entity.lo_claimant)
            put("title_no", entity.title_no)
            put("odts_no", entity.odts_no)
            put("lot_no", entity.lot_no)
            put("survey_no", entity.survey_no)
            put("area_has", entity.area_has)
            put("location", entity.location)
            put("transmitted_documents", entity.transmitted_documents)
            put("route_to", entity.route_to)
            entity.route_to_user_id?.let { put("route_to_user_id", it) }
            put("received_by_control_no", entity.received_by_control_no)
            put("remarks_action_taken", entity.remarks_action_taken)
            put("work_status", entity.work_status)
        }.toString()

        val response = if (entity.id != null) {
            ApiClient.putJson(url, payload)
        } else {
            ApiClient.postJson(url, payload)
        }

        if (response.success && response.json?.optBoolean("success") == true) {
            val newRemoteId = response.json.optJSONObject("data")?.optInt("id") ?: entity.id
            recordDao.updateRecord(entity.copy(id = newRemoteId, syncStatus = SyncStatus.SYNCED))
            return true
        }
        return false
    }

    private suspend fun findRecordByIdOrLocalId(recordId: Int): RecordEntity? {
        return recordDao.getByRemoteId(recordId) ?: recordDao.getByLocalId(recordId.toLong())
    }

    suspend fun deleteRecord(recordId: Int): Boolean = withContext(Dispatchers.IO) {
        val entity = findRecordByIdOrLocalId(recordId) ?: return@withContext false

        if (NetworkUtils.isOnline(context) && entity.id != null && entity.id!! > 0) {
            val url = "${context.getString(R.string.records_api_url)}?id=${entity.id}"
            val response = ApiClient.deleteJson(url)
            if (response.success && response.json?.optBoolean("success") == true) {
                recordDao.deleteRecord(entity)
                return@withContext true
            }
        }

        if (entity.id == null || entity.id!! <= 0) {
            recordDao.deleteRecord(entity)
            return@withContext true
        }

        recordDao.updateRecord(entity.copy(syncStatus = SyncStatus.PENDING_DELETE))
        return@withContext true
    }

    suspend fun restoreRecord(recordId: Int): Boolean = withContext(Dispatchers.IO) {
        val entity = findRecordByIdOrLocalId(recordId) ?: return@withContext false
        if (NetworkUtils.isOnline(context) && entity.id != null && entity.id!! > 0) {
            val payload = JSONObject().apply {
                put("action", "restore")
                put("id", entity.id)
            }.toString()
            val response = ApiClient.postJson(context.getString(R.string.archive_api_url), payload)
            if (response.success && response.json?.optBoolean("success") == true) {
                recordDao.updateRecord(entity.copy(isArchived = false, syncStatus = SyncStatus.SYNCED))
                return@withContext true
            }
        }
        recordDao.updateRecord(entity.copy(isArchived = false, syncStatus = SyncStatus.PENDING_UPDATE))
        return@withContext true
    }

    suspend fun archiveRecord(recordId: Int): Boolean = withContext(Dispatchers.IO) {
        val entity = findRecordByIdOrLocalId(recordId) ?: return@withContext false
        if (NetworkUtils.isOnline(context) && entity.id != null && entity.id!! > 0) {
            val payload = JSONObject().apply {
                put("action", "archive")
                put("id", entity.id)
            }.toString()
            val response = ApiClient.postJson(context.getString(R.string.archive_api_url), payload)
            if (response.success && response.json?.optBoolean("success") == true) {
                recordDao.updateRecord(entity.copy(isArchived = true, syncStatus = SyncStatus.SYNCED))
                return@withContext true
            }
        }
        recordDao.updateRecord(entity.copy(isArchived = true, syncStatus = SyncStatus.PENDING_UPDATE))
        return@withContext true
    }

    suspend fun toggleRecordStatus(recordId: Int): Boolean = withContext(Dispatchers.IO) {
        val entity = findRecordByIdOrLocalId(recordId) ?: return@withContext false
        val newStatus = if (entity.work_status.equals("finished", ignoreCase = true)) "not_finished" else "finished"
        val updatedEntity = entity.copy(work_status = newStatus, syncStatus = SyncStatus.PENDING_UPDATE)
        recordDao.updateRecord(updatedEntity)
        if (NetworkUtils.isOnline(context) && updatedEntity.id != null && updatedEntity.id!! > 0) return@withContext syncSingleRecord(updatedEntity)
        return@withContext true
    }

    suspend fun getPendingSyncRecords(): List<RecordEntity> = recordDao.getPendingSyncRecords()
}
