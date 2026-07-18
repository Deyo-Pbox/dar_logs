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
import com.example.darlogs.ui.ArchiveScreen
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class ArchiveComposeFragment : Fragment() {
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
                ArchiveContent(useLightMode) { ThemeManager.toggleTheme(context, it) }
            }
        }
    }

    @Composable
    private fun ArchiveContent(useLightMode: Boolean, onToggleLightMode: (Boolean) -> Unit) {
        val records by viewModel.archivedRecords.collectAsState()
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val username = activity?.intent?.getStringExtra("username") ?: "User"
        val isAdmin = activity?.intent?.getBooleanExtra("isAdmin", false) ?: false

        ArchiveScreen(
            username = username,
            records = records,
            isLoading = isLoading,
            onRestoreRecord = { record, onComplete ->
                coroutineScope.launch {
                    val success = viewModel.restoreRecord(record.id)
                    onComplete(success)
                }
            },
            onDeleteRecord = { record, onComplete ->
                coroutineScope.launch {
                    val success = viewModel.deleteRecord(record.id)
                    onComplete(success, if (success) null else "Failed to delete")
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
            useLightMode = useLightMode,
            onToggleLightMode = onToggleLightMode
        )
    }
}
