package com.example.darlogs.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PendingRecordsScreen(
    username: String,
    records: List<RecordItem>,
    isLoading: Boolean,
    onSyncRecords: () -> Unit,
    onNotificationsClick: () -> Unit,
    onManageAccounts: () -> Unit,
    onLogout: () -> Unit,
    isAdmin: Boolean,
    useLightMode: Boolean,
    onToggleLightMode: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val filteredRecords = remember(records, searchQuery) {
        records.filter { record ->
            searchQuery.isBlank() || listOf(
                record.claimant,
                record.titleNo,
                record.location,
                record.municipality
            ).any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    var currentPage by remember { mutableStateOf(1) }
    val pageSize = 10
    val totalPages = remember(filteredRecords) { maxOf(1, (filteredRecords.size + pageSize - 1) / pageSize) }
    val pagedRecords = remember(filteredRecords, currentPage) {
        filteredRecords.drop((currentPage - 1) * pageSize).take(pageSize)
    }

    LaunchedEffect(searchQuery) {
        currentPage = 1
    }

    val bgColor = if (useLightMode) BackgroundLight else BackgroundDark
    val surfaceColor = if (useLightMode) SurfaceLight else SurfaceDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    val searchBorderColor = if (useLightMode) LineColorLight else Surface
    
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = onSyncRecords)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DARTopAppBar(
                username = username,
                isAdmin = isAdmin,
                useLightMode = useLightMode,
                onNotificationsClick = onNotificationsClick,
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
                            text = "Pending Records",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        val pageSubtitle = if (isAdmin)
                            "View all active pending records across every user account."
                        else
                            "View your active pending records that still need follow-up."
                        Text(
                            text = pageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTextColor
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search pending records...", color = mutedTextColor, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedTextColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = searchBorderColor
                        ),
                        textStyle = TextStyle(fontSize = 14.sp, color = textColor)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Record List",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        PaginationControls(
                            currentPage = currentPage,
                            totalPages = totalPages,
                            onPageChange = { currentPage = it },
                            useLightMode = useLightMode
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandGreen)
                        }
                    }
                } else if (filteredRecords.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No pending records found", color = mutedTextColor)
                        }
                    }
                } else {
                    items(pagedRecords) { record ->
                        PendingRecordCard(
                            record = record,
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
fun PendingRecordCard(
    record: RecordItem,
    useLightMode: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Helper to clean up data
    fun String?.formatValue() = if (this.isNullOrBlank() || this.equals("null", ignoreCase = true)) "—" else this

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useLightMode) SurfaceLight else SurfaceDark.copy(alpha = 0.72f),
            contentColor = if (useLightMode) TextDarkLight else TextLight
        ),
        border = if (useLightMode) BorderStroke(1.dp, LineColorLight.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (useLightMode) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.municipality.formatValue(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Claimant: ${record.claimant.formatValue()}",
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }

                val statusColor = AccentGold

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Pending",
                        color = if (useLightMode) statusColor else TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Grid
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(label = "Title", value = record.titleNo, modifier = Modifier.weight(1f), useLightMode = useLightMode)
                    InfoItem(label = "Lot", value = record.lotNo, modifier = Modifier.weight(1f), useLightMode = useLightMode)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(label = "ODTS", value = record.odtsNo, modifier = Modifier.weight(1f), useLightMode = useLightMode)
                    InfoItem(label = "Survey", value = record.surveyNo, modifier = Modifier.weight(1f), useLightMode = useLightMode)
                }
                InfoItem(label = "Area", value = record.areaHas, useLightMode = useLightMode)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = if (useLightMode) LineColorLight else LineColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    DetailRow("Location", record.location, useLightMode)
                    DetailRow("Transmitted", record.transmittedDocuments, useLightMode)
                    DetailRow("Route To", record.routeTo, useLightMode)
                    DetailRow("Received By", record.receivedByControlNo, useLightMode)
                    DetailRow("Remarks", record.remarks, useLightMode)
                    DetailRow("Updated By", record.updatedBy, useLightMode)
                }
            }
        }
    }
}
