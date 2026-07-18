package com.example.darlogs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.darlogs.ui.RecordItem

@Entity(tableName = "activity_logs")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val id: Int? = null, // Matches MySQL column name 'id'
    val municipality: String?,
    val lo_claimant: String?,
    val title_no: String?,
    val odts_no: String?,
    val lot_no: String?,
    val survey_no: String?,
    val area_has: String?,
    val location: String?,
    val transmitted_documents: String?,
    val route_to: String?,
    val route_to_user_id: Int? = null,
    val routed_from_user_id: Int? = null,
    val routed_at: String? = null,
    val received_by_control_no: String?,
    val remarks_action_taken: String?,
    val work_status: String?,
    val archived_at: String? = null,
    val archived_by: Int? = null,
    val created_by: Int? = null,
    val updated_by: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    
    // Metadata for offline sync
    val updated_by_name: String? = null,
    val isArchived: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    fun toRecordItem(): RecordItem {
        return RecordItem(
            id = id ?: localId.toInt(),
            municipality = municipality ?: "",
            claimant = lo_claimant ?: "",
            titleNo = title_no ?: "",
            odtsNo = odts_no ?: "",
            lotNo = lot_no ?: "",
            surveyNo = survey_no ?: "",
            areaHas = area_has ?: "",
            location = location ?: "",
            transmittedDocuments = transmitted_documents ?: "",
            routeTo = route_to ?: "",
            routeToUserId = route_to_user_id,
            receivedByControlNo = received_by_control_no ?: "",
            status = work_status ?: "not_finished",
            remarks = remarks_action_taken ?: "",
            updatedBy = updated_by_name ?: "System"
        )
    }

    companion object {
        fun fromRecordItem(item: RecordItem, isArchived: Boolean = false, syncStatus: SyncStatus = SyncStatus.SYNCED): RecordEntity {
            return RecordEntity(
                id = if (item.id != 0) item.id else null,
                municipality = item.municipality,
                lo_claimant = item.claimant,
                title_no = item.titleNo,
                odts_no = item.odtsNo,
                lot_no = item.lotNo,
                survey_no = item.surveyNo,
                area_has = item.areaHas,
                location = item.location,
                transmitted_documents = item.transmittedDocuments,
                route_to = item.routeTo,
                route_to_user_id = item.routeToUserId,
                received_by_control_no = item.receivedByControlNo,
                work_status = item.status,
                remarks_action_taken = item.remarks,
                updated_by_name = item.updatedBy,
                isArchived = isArchived,
                syncStatus = syncStatus
            )
        }
    }
}
