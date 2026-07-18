package com.example.darlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.darlogs.ui.AuditLogEntry
import com.example.darlogs.ui.ManageAccountsScreen
import com.example.darlogs.ui.UserAccount
import com.example.darlogs.NotificationsComposeFragment
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import org.json.JSONArray
import org.json.JSONObject

import androidx.compose.ui.platform.LocalContext
import com.example.darlogs.ui.theme.ThemeManager

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
        var users by remember { mutableStateOf(emptyList<UserAccount>()) }
        var auditLogs by remember { mutableStateOf(emptyList<AuditLogEntry>()) }
        var isLoading by remember { mutableStateOf(true) }
        val username = activity?.intent?.getStringExtra("username") ?: "Admin"
        val isAdmin = activity?.intent?.getBooleanExtra("isAdmin", false) ?: false

        LaunchedEffect(Unit) {
            refreshData { u, logs ->
                users = u
                auditLogs = logs
                isLoading = false
            }
        }

        ManageAccountsScreen(
            username = username,
            users = users,
            auditLogs = auditLogs,
            isLoading = isLoading,
            onCreateAccount = { user, pass, role, onComplete ->
                isLoading = true
                val requestBody = JSONObject().apply {
                    put("username", user)
                    put("password", pass)
                    put("role", role)
                }.toString()
                Thread {
                    val response = ApiClient.postJson(getString(R.string.users_api_url), requestBody)
                    activity?.runOnUiThread {
                        if (response.success) {
                            refreshData { u, logs ->
                                users = u
                                auditLogs = logs
                                isLoading = false
                                onComplete(true, null)
                            }
                        } else {
                            isLoading = false
                            onComplete(false, response.json?.optString("message") ?: "Failed to create account")
                        }
                    }
                }.start()
            },
            onUpdateAccount = { id, user, pass, role, onComplete ->
                isLoading = true
                val requestBody = JSONObject().apply {
                    put("id", id)
                    put("username", user)
                    if (pass != null) put("password", pass)
                    put("role", role)
                }.toString()
                Thread {
                    val response = ApiClient.patchJson(getString(R.string.users_api_url), requestBody)
                    activity?.runOnUiThread {
                        if (response.success) {
                            refreshData { u, logs ->
                                users = u
                                auditLogs = logs
                                isLoading = false
                                onComplete(true, null)
                            }
                        } else {
                            isLoading = false
                            onComplete(false, response.json?.optString("message") ?: "Failed to update account")
                        }
                    }
                }.start()
            },
            onDeleteAccount = { id, onComplete ->
                isLoading = true
                Thread {
                    val url = "${getString(R.string.users_api_url)}?id=$id"
                    val response = ApiClient.deleteJson(url)
                    activity?.runOnUiThread {
                        if (response.success) {
                            refreshData { u, logs ->
                                users = u
                                auditLogs = logs
                                isLoading = false
                                onComplete(true, null)
                            }
                        } else {
                            isLoading = false
                            onComplete(false, response.json?.optString("message") ?: "Failed to delete account")
                        }
                    }
                }.start()
            },
            onSyncData = {
                isLoading = true
                refreshData { u, logs ->
                    users = u
                    auditLogs = logs
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

    private fun refreshData(onDone: (List<UserAccount>, List<AuditLogEntry>) -> Unit) {
        Thread {
            val usersResponse = ApiClient.getJson(getString(R.string.users_api_url))
            val auditResponse = ApiClient.getJson(getString(R.string.audit_api_url))

            activity?.runOnUiThread {
                val userList = mutableListOf<UserAccount>()
                if (usersResponse.success && usersResponse.json != null) {
                    val data = usersResponse.json.optJSONArray("data") ?: JSONArray()
                    for (i in 0 until data.length()) {
                        data.optJSONObject(i)?.let { obj ->
                            userList.add(
                                UserAccount(
                                    id = obj.optInt("id"),
                                    username = obj.optString("username"),
                                    role = obj.optString("role"),
                                    approved = obj.optInt("approved"),
                                    createdAt = obj.optString("created_at"),
                                    lastActivity = if (obj.isNull("last_activity")) null else obj.optString("last_activity")
                                )
                            )
                        }
                    }
                }

                val logList = mutableListOf<AuditLogEntry>()
                if (auditResponse.success && auditResponse.json != null) {
                    val data = auditResponse.json.optJSONArray("data") ?: JSONArray()
                    for (i in 0 until data.length()) {
                        data.optJSONObject(i)?.let { obj ->
                            logList.add(
                                AuditLogEntry(
                                    id = obj.optInt("id"),
                                    username = obj.optString("username"),
                                    action = obj.optString("action"),
                                    recordId = if (obj.isNull("record_id")) null else obj.optInt("record_id"),
                                    details = obj.optString("details"),
                                    createdAt = obj.optString("created_at")
                                )
                            )
                        }
                    }
                }
                onDone(userList, logList)
            }
        }.start()
    }
}
