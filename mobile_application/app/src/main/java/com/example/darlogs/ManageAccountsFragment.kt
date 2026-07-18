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
import com.example.darlogs.ui.ManageAccountsScreen
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class ManageAccountsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            val useLightMode = ThemeManager.useLightMode
            val context = LocalContext.current

            MaterialTheme(colorScheme = if (useLightMode) DarLightColorScheme else DarDarkColorScheme) {
                ManageAccountsContent(useLightMode) { ThemeManager.toggleTheme(context, it) }
            }
        }
    }

    @Composable
    private fun ManageAccountsContent(useLightMode: Boolean, onToggleLightMode: (Boolean) -> Unit) {
        val repository = remember { RecordRepository.getInstance(requireContext()) }
        val users by repository.cachedUsers.collectAsState(initial = emptyList())
        val auditLogs by repository.auditLogs.collectAsState(initial = emptyList())
        var isLoading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val username = activity?.intent?.getStringExtra("username") ?: "Admin"
        val isAdmin = activity?.intent?.getBooleanExtra("isAdmin", false) ?: false

        LaunchedEffect(Unit) {
            isLoading = true
            repository.refreshUsers()
            repository.refreshAuditLogs()
            isLoading = false
        }

        ManageAccountsScreen(
            username = username,
            users = users,
            auditLogs = auditLogs,
            isLoading = isLoading,
            onCreateAccount = { user, pass, role, onComplete ->
                coroutineScope.launch {
                    val success = repository.createAccount(user, pass, role)
                    onComplete(success, if (success) null else "Failed to create account")
                }
            },
            onUpdateAccount = { id, user, pass, role, onComplete ->
                coroutineScope.launch {
                    val success = repository.updateAccount(id, user, pass, role)
                    onComplete(success, if (success) null else "Failed to update account")
                }
            },
            onDeleteAccount = { id, onComplete ->
                coroutineScope.launch {
                    val success = repository.deleteAccount(id)
                    onComplete(success, if (success) null else "Failed to delete account")
                }
            },
            onSyncData = {
                coroutineScope.launch {
                    isLoading = true
                    repository.refreshUsers()
                    repository.refreshAuditLogs()
                    isLoading = false
                }
            },
            onNotificationsClick = {
                (activity as? DashboardActivity)?.showSection(NotificationsComposeFragment(), R.string.menu_notifications)
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
