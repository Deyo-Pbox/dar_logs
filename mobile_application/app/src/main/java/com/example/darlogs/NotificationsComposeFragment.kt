package com.example.darlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import com.example.darlogs.data.RecordRepository
import com.example.darlogs.ui.NotificationsScreen
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class NotificationsComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            val useLightMode = ThemeManager.useLightMode
            val context = LocalContext.current

            MaterialTheme(colorScheme = if (useLightMode) DarLightColorScheme else DarDarkColorScheme) {
                NotificationsContent(useLightMode) { ThemeManager.toggleTheme(context, it) }
            }
        }
    }

    @Composable
    private fun NotificationsContent(useLightMode: Boolean, onToggleLightMode: (Boolean) -> Unit) {
        val repository = remember { RecordRepository.getInstance(requireContext()) }
        val notifications by repository.notifications.collectAsState(initial = emptyList())
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val username = activity?.intent?.getStringExtra("username") ?: "User"
        val isAdmin = activity?.intent?.getBooleanExtra("isAdmin", false) ?: false

        LaunchedEffect(Unit) {
            isLoading = true
            repository.refreshNotifications()
            isLoading = false
        }

        NotificationsScreen(
            username = username,
            notifications = notifications,
            isLoading = isLoading,
            onMarkAsRead = { ids, onComplete ->
                coroutineScope.launch {
                    try {
                        repository.markNotificationsRead(ids)
                        onComplete(true)
                    } catch (e: Exception) {
                        onComplete(false)
                    }
                }
            },
            onSyncRecords = {
                coroutineScope.launch {
                    isLoading = true
                    repository.refreshNotifications()
                    isLoading = false
                }
            },
            onManageAccounts = {
                (activity as? DashboardActivity)?.showSection(ManageAccountsFragment(), R.string.menu_manage_accounts)
            },
            onLogout = {
                (activity as? DashboardActivity)?.logout()
            },
            isAdmin = isAdmin,
            useLightMode = useLightMode,
            onToggleLightMode = onToggleLightMode
        )
    }
}
