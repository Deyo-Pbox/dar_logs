package com.example.darlogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompletedRecordsScreen(
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val filteredRecords by remember(records, searchQuery) {
        derivedStateOf {
            records.filter { record ->
                searchQuery.isBlank() || listOf(
                    record.claimant,
                    record.titleNo,
                    record.location,
                    record.municipality
                ).any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 10
    val totalPages by remember { derivedStateOf { maxOf(1, (filteredRecords.size + pageSize - 1) / pageSize) } }
    val pagedRecords by remember(filteredRecords, currentPage) {
        derivedStateOf {
            filteredRecords.drop((currentPage - 1) * pageSize).take(pageSize)
        }
    }

    LaunchedEffect(searchQuery) {
        currentPage = 1
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) currentPage = totalPages
    }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    val bgColor = if (useLightMode) BackgroundLight else BackgroundDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    val searchBorderColor = if (useLightMode) LineColorLight else Surface
    
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = onSyncRecords)

    Scaffold(
        containerColor = bgColor,
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Completed Records",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Records marked as finished (not archived).",
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
                        placeholder = { Text("Search completed logs...", color = mutedTextColor, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedTextColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
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

                if (isLoading && records.isEmpty()) {
                    items(6, key = { "sk_$it" }) {
                        ShimmerRecordCard(useLightMode = useLightMode)
                    }
                } else if (filteredRecords.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.CheckCircle,
                            title = "No completed records",
                            subtitle = "Finished records will appear here.",
                            useLightMode = useLightMode
                        )
                    }
                } else {
                    val indexedRecords = pagedRecords.withIndex().toList()
                    items(
                        count = indexedRecords.size,
                        key = { idx -> indexedRecords[idx].value.id }
                    ) { idx ->
                        val (index, record) = indexedRecords[idx]
                        StaggeredAnimatedItem(index = index) {
                            CompletedRecordCard(
                                record = record,
                                useLightMode = useLightMode
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = if (useLightMode) SurfaceLight else SurfaceDark,
                contentColor = BrandGreen
            )

            AnimatedVisibility(
                visible = showScrollToTop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 100.dp),
                enter = scaleIn(animationSpec = tween(300)),
                exit = scaleOut(animationSpec = tween(200))
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = BrandGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    }
}

@Composable
fun CompletedRecordCard(
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

                val statusColor = SuccessGreen

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Finished",
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
