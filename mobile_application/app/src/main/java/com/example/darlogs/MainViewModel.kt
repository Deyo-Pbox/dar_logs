package com.example.darlogs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.darlogs.data.LocalSyncWorker
import com.example.darlogs.data.MunicipalityCatalog
import com.example.darlogs.data.RecordRepository
import com.example.darlogs.ui.DashboardStats
import com.example.darlogs.ui.NewRecordInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecordRepository.getInstance(application)

    val activeRecords = repository.activeRecords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val archivedRecords = repository.archivedRecords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val dashboardStats = repository.dashboardStats.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats()
    )

    val routeToUsers = repository.routeToUsers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val municipalityCatalog = MunicipalityCatalog.options

    fun refreshAll() {
        viewModelScope.launch {
            repository.refreshAll()
            LocalSyncWorker.scheduleSync(getApplication())
        }
    }

    suspend fun saveRecord(input: NewRecordInput): Boolean {
        val success = repository.saveRecord(input)
        if (success) LocalSyncWorker.scheduleSync(getApplication())
        return success
    }

    suspend fun deleteRecord(recordId: Int): Boolean {
        val success = repository.deleteRecord(recordId)
        if (success) LocalSyncWorker.scheduleSync(getApplication())
        return success
    }

    suspend fun archiveRecord(recordId: Int): Boolean {
        val success = repository.archiveRecord(recordId)
        if (success) LocalSyncWorker.scheduleSync(getApplication())
        return success
    }

    suspend fun restoreRecord(recordId: Int): Boolean {
        val success = repository.restoreRecord(recordId)
        if (success) LocalSyncWorker.scheduleSync(getApplication())
        return success
    }

    suspend fun toggleRecordStatus(recordId: Int): Boolean {
        val success = repository.toggleRecordStatus(recordId)
        if (success) LocalSyncWorker.scheduleSync(getApplication())
        return success
    }
}
