package com.example.darlogs.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    username: String,
    notifications: List<NotificationItem>,
    isLoading: Boolean,
    onMarkAsRead: (List<Int>, (Boolean) -> Unit) -> Unit,
    onSyncRecords: () -> Unit,
    onManageAccounts: () -> Unit,
    onLogout: () -> Unit,
    isAdmin: Boolean,
    useLightMode: Boolean,
    onToggleLightMode: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = onSyncRecords)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val bgColor = if (useLightMode) BackgroundLight else BackgroundDark
    val surfaceColor = if (useLightMode) SurfaceLight else SurfaceDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted

    Scaffold(
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DARTopAppBar(
                username = username,
                isAdmin = isAdmin,
                useLightMode = useLightMode,
                onNotificationsClick = { /* already here */ },
                onSyncRecords = onSyncRecords,
                onManageAccounts = onManageAccounts,
                onLogout = onLogout,
                onToggleLightMode = onToggleLightMode
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .pullRefresh(pullRefreshState)
                .padding(innerPadding)
                .fillMaxSize()
                .background(bgColor)
        ) {
            Image(
                painter = painterResource(id = R.drawable.dar_logo_white_bg),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .alpha(0.08f),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(if (useLightMode) Color.Transparent else Color.Black.copy(alpha = 0.08f))
            ) {}

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Routing updates and record hand-offs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTextColor
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { 
                                val unreadIds = notifications.filter { !it.isRead }.map { it.id }
                                onMarkAsRead(unreadIds) { success ->
                                    if (success) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("All notifications marked as read") }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            enabled = notifications.any { !it.isRead },
                            border = BorderStroke(1.dp, if (useLightMode) LineColorLight else LineColor)
                        ) {
                            Text("Mark all as read", fontSize = 12.sp)
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandGreen)
                        }
                    }
                } else if (notifications.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No notifications yet.", color = mutedTextColor)
                        }
                    }
                } else {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onMarkRead = { 
                                onMarkAsRead(listOf(notification.id)) { success ->
                                    if (success) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Notification marked as read") }
                                    }
                                }
                            },
                            useLightMode = useLightMode
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = surfaceColor,
                contentColor = BrandGreen
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onMarkRead: () -> Unit,
    useLightMode: Boolean
) {
    val bgColor = if (useLightMode) SurfaceLight else SurfaceDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useLightMode) {
                if (notification.isRead) bgColor.copy(alpha = 0.6f) else bgColor
            } else {
                if (notification.isRead) bgColor.copy(alpha = 0.5f) else bgColor.copy(alpha = 0.85f)
            },
            contentColor = textColor
        ),
        border = if (useLightMode) BorderStroke(1.dp, LineColorLight.copy(alpha = 0.4f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (notification.type == "route") Icons.Default.DirectionsRun else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (notification.isRead) Color.Gray else BrandGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (notification.type == "route") "New Record Routed" else "Update",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(text = "From: ${notification.senderName}", fontSize = 12.sp, color = mutedTextColor)
                    }
                }
                
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BrandGreen, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = notification.message, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = notification.createdAt, fontSize = 11.sp, color = mutedTextColor)
                if (!notification.isRead) {
                    TextButton(onClick = onMarkRead, contentPadding = PaddingValues(0.dp)) {
                        Text("Mark as read", fontSize = 12.sp, color = BrandGreen)
                    }
                }
            }
        }
    }
}
