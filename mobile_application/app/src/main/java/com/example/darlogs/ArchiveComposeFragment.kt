package com.example.darlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import com.example.darlogs.data.LocalSyncWorker
import com.example.darlogs.data.RecordRepository
import com.example.darlogs.ui.ArchiveScreen
import com.example.darlogs.ui.MunicipalityOption
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class ArchiveComposeFragment : Fragment() {
    private lateinit var repository: RecordRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = RecordRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            val useLightMode = ThemeManager.useLightMode
            val context = LocalContext.current

            MaterialTheme(colorScheme = if (useLightMode) DarLightColorScheme else DarDarkColorScheme) {
                ArchiveContent(useLightMode) { ThemeManager.toggleTheme(context, it) }
            }
        }
    }

    @Composable
    private fun ArchiveContent(useLightMode: Boolean, onToggleLightMode: (Boolean) -> Unit) {
        val records by repository.archivedRecords.collectAsState(initial = emptyList())
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val username = activity?.intent?.getStringExtra("username") ?: "User"
        val isAdmin = activity?.intent?.getBooleanExtra("isAdmin", false) ?: false

        val municipalityCatalog = remember {
            listOf(
                MunicipalityOption("BOMBON", "Bombon"),
                MunicipalityOption("CALABANGA", "Calabanga"),
                MunicipalityOption("CANAMAN", "Canaman"),
                MunicipalityOption("CARAMOAN", "Caramoan"),
                MunicipalityOption("GARCHITORENA", "Garchitorena"),
                MunicipalityOption("GOA", "Goa"),
                MunicipalityOption("LAGONOY", "Lagonoy"),
                MunicipalityOption("MAGARAO", "Magarao"),
                MunicipalityOption("NAGA", "Naga"),
                MunicipalityOption("OCAMPO", "Ocampo"),
                MunicipalityOption("PILI", "Pili"),
                MunicipalityOption("PRESENTACION", "Presentacion"),
                MunicipalityOption("SAGÑAY", "Sagñay"),
                MunicipalityOption("SANJOSE", "San Jose"),
                MunicipalityOption("SIRUMA", "Siruma"),
                MunicipalityOption("TIGAON", "Tigaon"),
                MunicipalityOption("TINAMBAC", "Tinambac")
            )
        }

        LaunchedEffect(Unit) {
            isLoading = true
            repository.refreshAll()
            isLoading = false
            LocalSyncWorker.scheduleSync(requireContext())
        }

        ArchiveScreen(
            username = username,
            records = records,
            isLoading = isLoading,
            onRestoreRecord = { record, onComplete ->
                coroutineScope.launch {
                    val success = repository.restoreRecord(record.id)
                    onComplete(success)
                    if (success) LocalSyncWorker.scheduleSync(requireContext())
                }
            },
            onDeleteRecord = { record, onComplete ->
                coroutineScope.launch {
                    val success = repository.deleteRecord(record.id)
                    onComplete(success, if (success) null else "Failed to delete")
                    if (success) LocalSyncWorker.scheduleSync(requireContext())
                }
            },
            onSyncRecords = {
                coroutineScope.launch {
                    isLoading = true
                    repository.refreshAll()
                    isLoading = false
                    LocalSyncWorker.scheduleSync(requireContext())
                }
            },
            onNotificationsClick = {
                (activity as? DashboardActivity)?.showSection(NotificationsComposeFragment(), R.string.menu_notifications)
            },
            onManageAccounts = {
                (activity as? DashboardActivity)?.showSection(ManageAccountsFragment(), R.string.menu_manage_accounts)
            },
            onLogout = {
                (activity as? DashboardActivity)?.logout()
            },
            isAdmin = isAdmin,
            municipalityCatalog = municipalityCatalog,
            useLightMode = useLightMode,
            onToggleLightMode = onToggleLightMode
        )
    }
}
