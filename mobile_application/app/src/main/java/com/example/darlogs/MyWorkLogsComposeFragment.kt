package com.example.darlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.darlogs.ui.MyWorkLogsScreen
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class MyWorkLogsComposeFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            val useLightMode = ThemeManager.useLightMode
            val context = LocalContext.current

            MaterialTheme(colorScheme = if (useLightMode) DarLightColorScheme else DarDarkColorScheme) {
                MyWorkLogsContent(useLightMode) { ThemeManager.toggleTheme(context, it) }
            }
        }
    }

    @Composable
    private fun MyWorkLogsContent(useLightMode: Boolean, onToggleLightMode: (Boolean) -> Unit) {
        val records by viewModel.activeRecords.collectAsState()
        val routeToUsers by viewModel.routeToUsers.collectAsState()
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        val intent = activity?.intent
        val serverUsername = intent?.getStringExtra("username") ?: "User"
        val isAdmin = intent?.getBooleanExtra("isAdmin", false) ?: (intent?.getStringExtra("userRole") == "admin")

        MyWorkLogsScreen(
            username = serverUsername,
            records = records,
            isLoading = isLoading,
            onAddRecord = {},
            onSaveRecord = { newRecord, onComplete ->
                coroutineScope.launch {
                    val success = viewModel.saveRecord(newRecord)
                    onComplete(success)
                }
            },
            onDeleteRecord = { record, onComplete ->
                coroutineScope.launch {
                    val success = viewModel.deleteRecord(record.id)
                    onComplete(success, if (success) null else "Delete failed")
                }
            },
            onArchiveRecord = { record, onComplete ->
                coroutineScope.launch {
                    val success = viewModel.archiveRecord(record.id)
                    onComplete(success)
                }
            },
            onToggleStatus = { record, onComplete ->
                coroutineScope.launch {
                    val success = viewModel.toggleRecordStatus(record.id)
                    onComplete(success)
                }
            },
            onSyncRecords = {
                coroutineScope.launch {
                    isLoading = true
                    viewModel.refreshAll()
                    isLoading = false
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
            municipalityCatalog = viewModel.municipalityCatalog,
            routeToUsers = routeToUsers,
            useLightMode = useLightMode,
            onToggleLightMode = onToggleLightMode
        )
    }
}
