package com.example.darlogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.darlogs.ui.NotificationItem
import com.example.darlogs.ui.NotificationsScreen
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import org.json.JSONArray
import org.json.JSONObject

import androidx.compose.ui.platform.LocalContext
import com.example.darlogs.ui.theme.ThemeManager

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
        var notifications by remember { mutableStateOf(emptyList<NotificationItem>()) }
        var isLoading by remember { mutableStateOf(true) }
        val username = activity?.intent?.getStringExtra("username") ?: "User"
        val isAdmin = activity?.intent?.getBooleanExtra("isAdmin", false) ?: false

        LaunchedEffect(Unit) {
            loadNotificationsData { newNotifications ->
                notifications = newNotifications
                isLoading = false
            }
        }

        NotificationsScreen(
            username = username,
            notifications = notifications,
            isLoading = isLoading,
            onMarkAsRead = { ids, onComplete ->
                if (ids.isEmpty()) {
                    onComplete(true)
                    return@NotificationsScreen
                }
                Thread {
                    val requestBody = JSONObject().apply {
                        put("action", "mark_read")
                        val jsonIds = JSONArray()
                        ids.forEach { jsonIds.put(it) }
                        put("ids", jsonIds)
                    }.toString()
                    val response = ApiClient.postJson(getString(R.string.notifications_api_url), requestBody)
                    activity?.runOnUiThread {
                        if (response.success) {
                            loadNotificationsData { newNotifications ->
                                notifications = newNotifications
                                onComplete(true)
                            }
                        } else {
                            onComplete(false)
                        }
                    }
                }.start()
            },
            onSyncRecords = {
                isLoading = true
                loadNotificationsData { newNotifications ->
                    notifications = newNotifications
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

    private fun loadNotificationsData(
        onDataLoaded: (List<NotificationItem>) -> Unit
    ) {
        Thread {
            val response = ApiClient.getJson(getString(R.string.notifications_api_url))

            activity?.runOnUiThread {
                val list = mutableListOf<NotificationItem>()
                if (response.success && response.json != null) {
                    val data = response.json.optJSONArray("data") ?: JSONArray()
                    for (i in 0 until data.length()) {
                        data.optJSONObject(i)?.let { obj ->
                            list.add(
                                NotificationItem(
                                    id = obj.optInt("id"),
                                    type = obj.optString("type"),
                                    recordId = if (obj.isNull("record_id")) null else obj.optInt("record_id"),
                                    senderId = if (obj.isNull("sender_id")) null else obj.optInt("sender_id"),
                                    senderName = obj.optString("sender_name", "System"),
                                    message = obj.optString("message"),
                                    isRead = obj.optInt("is_read") == 1,
                                    createdAt = obj.optString("created_at")
                                )
                            )
                        }
                    }
                }
                onDataLoaded(list)
            }
        }.start()
    }
}
