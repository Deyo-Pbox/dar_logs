package com.example.darlogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.launch

// Data models
@androidx.compose.runtime.Stable
data class RecordItem(
    val id: Int,
    val municipality: String,
    val claimant: String,
    val titleNo: String,
    val odtsNo: String,
    val lotNo: String,
    val surveyNo: String,
    val areaHas: String,
    val location: String,
    val transmittedDocuments: String,
    val routeTo: String,
    val routeToUserId: Int? = null,
    val receivedByControlNo: String,
    val status: String,
    val remarks: String,
    val updatedBy: String
)

@androidx.compose.runtime.Stable
data class DashboardStats(
    val activeRecords: Int = 0,
    val archivedRecords: Int = 0,
    val totalUsers: Int = 0,
    val recentEdits: Int = 0,
    val activeUsers: Int = 0
)

@androidx.compose.runtime.Stable
data class MunicipalityOption(
    val value: String,
    val label: String
)

@androidx.compose.runtime.Stable
data class RouteToUserOption(
    val id: Int,
    val label: String
)

@androidx.compose.runtime.Stable
data class NotificationItem(
    val id: Int,
    val type: String,
    val recordId: Int?,
    val senderId: Int?,
    val senderName: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)

@androidx.compose.runtime.Stable
data class NewRecordInput(
    val id: Int? = null,
    val municipality: String,
    val claimant: String,
    val titleNo: String,
    val odtsNo: String,
    val lotNo: String,
    val surveyNo: String,
    val areaHas: String,
    val location: String,
    val transmittedDocuments: String,
    val routeTo: String,
    val routeToUserId: Int? = null,
    val receivedByControlNo: String,
    val remarks: String,
    val workStatus: String
)

// Main Dashboard Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    username: String,
    stats: DashboardStats,
    records: List<RecordItem>,
    isLoading: Boolean,
    onAddRecord: () -> Unit,
    onSaveRecord: (NewRecordInput, (Boolean) -> Unit) -> Unit,
    onDeleteRecord: (RecordItem, (Boolean, String?) -> Unit) -> Unit,
    routeToUsers: List<RouteToUserOption>,
    onSyncRecords: () -> Unit,
    onRecordClick: (RecordItem) -> Unit,
    onNotificationsClick: () -> Unit,
    onManageAccounts: () -> Unit,
    onLogout: () -> Unit,
    isAdmin: Boolean,
    municipalityCatalog: List<MunicipalityOption>,
    useLightMode: Boolean,
    onToggleLightMode: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMunicipality by remember { mutableStateOf("All") }
    var showAllMunicipalitiesSheet by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickFilters = remember(municipalityCatalog, selectedMunicipality) {
        val selectedOption = municipalityCatalog.firstOrNull { it.value == selectedMunicipality }
        val defaultOptions = municipalityCatalog.take(2)
        val visibleDefaults = defaultOptions.filter { it.value != selectedMunicipality }

        buildList {
            add(MunicipalityOption("All", "All"))
            selectedOption?.takeIf { it.value != "All" }?.let { add(it) }

            if (selectedOption == null || selectedOption.value == "All") {
                addAll(visibleDefaults)
            } else {
                visibleDefaults.firstOrNull()?.let { add(it) }
            }
        }
    }

    val filteredRecords by remember(records, selectedMunicipality, searchQuery) {
        derivedStateOf {
            records.filter { record ->
                val matchesMunicipality = selectedMunicipality == "All" || record.municipality.equals(selectedMunicipality, ignoreCase = true)
                val matchesQuery = searchQuery.isBlank() || listOf(
                    record.claimant,
                    record.titleNo,
                    record.location,
                    record.municipality
                ).any { it.contains(searchQuery, ignoreCase = true) }
                matchesMunicipality && matchesQuery
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

    LaunchedEffect(searchQuery, selectedMunicipality) {
        currentPage = 1
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    val bgColor = if (useLightMode) BackgroundLight else BackgroundDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    val sheetSurface = if (useLightMode) SurfaceLight.copy(alpha = 0.92f) else SurfaceDark.copy(alpha = 0.92f)
    val searchBorderColor = if (useLightMode) LineColorLight else Surface
    val selectedChipTextColor = if (useLightMode) Color.White else Surface
    val unselectedChipBg = if (useLightMode) SurfaceSecondaryLight.copy(alpha = 0.32f) else SurfaceDark.copy(alpha = 0.32f)
    val selectedChipBg = BrandGreen.copy(alpha = 0.88f)
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = onSyncRecords)
    var showAddRecordModal by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<RecordItem?>(null) }
    var addModalError by remember { mutableStateOf<String?>(null) }
    var addModalSubmitting by remember { mutableStateOf(false) }
    var addModalMunicipality by remember { mutableStateOf<MunicipalityOption?>(null) }
    var addModalMunicipalityMenuOpen by remember { mutableStateOf(false) }
    var addModalMunicipalitySearch by remember { mutableStateOf("") }
    var addModalClaimant by remember { mutableStateOf("") }
    var addModalTitleNo by remember { mutableStateOf("") }
    var addModalOdtsNo by remember { mutableStateOf("") }
    var addModalLotNo by remember { mutableStateOf("") }
    var addModalSurveyNo by remember { mutableStateOf("") }
    var addModalAreaHas by remember { mutableStateOf("") }
    var addModalLocation by remember { mutableStateOf("") }
    var addModalTransmittedDocuments by remember { mutableStateOf("") }
    var addModalRouteToUser by remember { mutableStateOf<RouteToUserOption?>(null) }
    var addModalRouteToMenuOpen by remember { mutableStateOf(false) }
    var addModalRouteToSearch by remember { mutableStateOf("") }
    var addModalReceivedByControlNo by remember { mutableStateOf("") }
    var addModalRemarks by remember { mutableStateOf("") }
    var addModalStatus by remember { mutableStateOf("not_finished") }
    var editingRecordId by remember { mutableStateOf<Int?>(null) }
    val addModalScrollState = rememberScrollState()
    val openEditRecord: (RecordItem) -> Unit = { record ->
        editingRecordId = record.id
        addModalError = null
        addModalMunicipality = MunicipalityOption(record.municipality, record.municipality)
        addModalMunicipalitySearch = ""
        addModalClaimant = record.claimant
        addModalTitleNo = record.titleNo
        addModalOdtsNo = record.odtsNo
        addModalLotNo = record.lotNo
        addModalSurveyNo = record.surveyNo
        addModalAreaHas = record.areaHas
        addModalLocation = record.location
        addModalTransmittedDocuments = record.transmittedDocuments
        addModalRouteToUser = routeToUsers.firstOrNull { it.id == record.routeToUserId }
        addModalRouteToSearch = ""
        addModalReceivedByControlNo = record.receivedByControlNo
        addModalRemarks = record.remarks
        addModalStatus = record.status
        showAddRecordModal = true
    }

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },

        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }

            FloatingActionButton(
                onClick = {
                    addModalError = null
                    addModalMunicipality = null
                    addModalMunicipalitySearch = ""
                    addModalClaimant = ""
                    addModalTitleNo = ""
                    addModalOdtsNo = ""
                    addModalLotNo = ""
                    addModalSurveyNo = ""
                    addModalAreaHas = ""
                    addModalLocation = ""
                    addModalTransmittedDocuments = ""
                    addModalRouteToUser = null
                    addModalRouteToSearch = ""
                    addModalReceivedByControlNo = ""
                    addModalRemarks = ""
                    addModalStatus = "not_finished"
                    editingRecordId = null
                    showAddRecordModal = true
                    onAddRecord()
                },
                containerColor = BrandGreen,
                contentColor = if (useLightMode) TextDarkLight else Surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
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
                    .background(if (useLightMode) Color.Black.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.08f))
            ) {}

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                item { StatsOverviewSection(stats = stats, useLightMode = useLightMode) }

                item {
                    Text(
                        text = "Filter by Municipality",
                        style = MaterialTheme.typography.labelMedium,
                        color = mutedTextColor,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                    ) {
                        items(quickFilters) { option ->
                            val isSelected = selectedMunicipality == option.value
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMunicipality = option.value },
                                label = { Text(option.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = selectedChipBg,
                                    selectedLabelColor = selectedChipTextColor,
                                    containerColor = unselectedChipBg,
                                    labelColor = textColor
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showAllMunicipalitiesSheet = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("More", fontSize = 10.sp, maxLines = 1)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = if (useLightMode) SurfaceSecondaryLight.copy(alpha = 0.32f) else SurfaceDark.copy(alpha = 0.32f),
                                    labelColor = if (useLightMode) TextDarkLight else TextDark
                                )
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusedState ->
                                if (!focusedState.isFocused) {
                                    focusManager.clearFocus()
                                }
                            },
                        placeholder = { Text("Search claimant, title, location...", color = mutedTextColor) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedTextColor) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = textColor),
                        colors = OutlinedTextFieldDefaults.colors(
                            cursorColor = BrandGreen,
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = searchBorderColor
                        )
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
                            text = "Active Records",
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
                    items(6, key = { "skeleton_$it" }) {
                        ShimmerRecordCard(useLightMode = useLightMode)
                    }
                } else if (filteredRecords.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyStateView(
                            icon = Icons.Default.Inbox,
                            title = "No active records",
                            subtitle = if (searchQuery.isNotEmpty() || selectedMunicipality != "All") "Try adjusting your filters" else "Tap + to add your first record",
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
                            RecordCardExpanded(
                                record = record,
                                onClick = { openEditRecord(record) },
                                onDelete = { recordToDelete = record; showDeleteConfirm = true },
                                useLightMode = useLightMode
                            )
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp),
                backgroundColor = if (useLightMode) SurfaceLight else SurfaceDark,
                contentColor = BrandGreen
            )
        }

        if (showAddRecordModal) {
            ModalBottomSheet(
                onDismissRequest = { if (!addModalSubmitting) showAddRecordModal = false },
                containerColor = if (useLightMode) SurfaceLight else Color(0xFF071205),
                scrimColor = Color.Black.copy(alpha = 0.70f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(addModalScrollState)
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (editingRecordId != null) "Edit Record" else "Add Record", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        IconButton(onClick = { if (!addModalSubmitting) showAddRecordModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = addModalMunicipalityMenuOpen,
                        onExpandedChange = { if (!addModalSubmitting) addModalMunicipalityMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = addModalMunicipality?.label ?: "",
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text("Municipality") },
                            placeholder = { Text("Select municipality") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = addModalMunicipalityMenuOpen)
                            },
                            readOnly = true,
                            enabled = !addModalSubmitting,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = searchBorderColor,
                                disabledBorderColor = searchBorderColor
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = addModalMunicipalityMenuOpen,
                            onDismissRequest = { addModalMunicipalityMenuOpen = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = addModalMunicipalitySearch,
                                onValueChange = { addModalMunicipalitySearch = it },
                                placeholder = { Text("Search municipalities…") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    unfocusedBorderColor = searchBorderColor
                                )
                            )
                            val filteredMunicipalities = municipalityCatalog.filter {
                                it.label.contains(addModalMunicipalitySearch, ignoreCase = true)
                            }
                            if (filteredMunicipalities.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No municipalities found", color = mutedTextColor) },
                                    onClick = { }
                                )
                            } else {
                                filteredMunicipalities.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            addModalMunicipality = option
                                            addModalMunicipalityMenuOpen = false
                                            addModalMunicipalitySearch = ""
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = addModalClaimant,
                        onValueChange = { addModalClaimant = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("LO / Claimant") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalTitleNo,
                        onValueChange = { addModalTitleNo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title No.") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalOdtsNo,
                        onValueChange = { addModalOdtsNo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("ODTS No.") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalLotNo,
                        onValueChange = { addModalLotNo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Lot No.") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalSurveyNo,
                        onValueChange = { addModalSurveyNo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Survey No.") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalAreaHas,
                        onValueChange = { addModalAreaHas = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Area (has.)") },
                        singleLine = true,
                        enabled = !addModalSubmitting,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalLocation,
                        onValueChange = { addModalLocation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalTransmittedDocuments,
                        onValueChange = { addModalTransmittedDocuments = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Transmitted / Requested Documents") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = addModalRouteToMenuOpen,
                        onExpandedChange = { if (!addModalSubmitting) addModalRouteToMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = addModalRouteToUser?.label ?: if (routeToUsers.isEmpty()) "No users available" else "Select route recipient",
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text("Route To") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = addModalRouteToMenuOpen)
                            },
                            readOnly = true,
                            enabled = !addModalSubmitting,
                            placeholder = { Text(if (routeToUsers.isEmpty()) "No users available" else "Select route recipient", color = mutedTextColor.copy(alpha = 0.55f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = searchBorderColor,
                                disabledBorderColor = searchBorderColor,
                                cursorColor = BrandGreen
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = addModalRouteToMenuOpen,
                            onDismissRequest = { addModalRouteToMenuOpen = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = addModalRouteToSearch,
                                onValueChange = { addModalRouteToSearch = it },
                                placeholder = { Text("Search users…", color = mutedTextColor.copy(alpha = 0.6f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    unfocusedBorderColor = searchBorderColor,
                                    disabledBorderColor = searchBorderColor,
                                    cursorColor = BrandGreen
                                )
                            )
                            val filteredRouteUsers = routeToUsers.filter {
                                it.label.contains(addModalRouteToSearch, ignoreCase = true)
                            }
                            if (filteredRouteUsers.isEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (routeToUsers.isEmpty()) "No users available" else "No users match search",
                                            color = mutedTextColor
                                        )
                                    },
                                    onClick = { }
                                )
                            } else {
                                filteredRouteUsers.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            addModalRouteToUser = option
                                            addModalRouteToMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalReceivedByControlNo,
                        onValueChange = { addModalReceivedByControlNo = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Received By / Control No.") },
                        singleLine = true,
                        enabled = !addModalSubmitting
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status", fontWeight = FontWeight.Medium, color = textColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Pending" to "not_finished", "Finished" to "finished").forEach { (label, value) ->
                            FilterChip(
                                selected = addModalStatus == value,
                                onClick = { addModalStatus = value },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (useLightMode) SurfaceSecondaryLight.copy(alpha = 0.24f) else SurfaceDark.copy(alpha = 0.28f),
                                    labelColor = if (useLightMode) TextDarkLight else TextLight
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addModalRemarks,
                        onValueChange = { addModalRemarks = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        label = { Text("Remarks / Action Taken") },
                        enabled = !addModalSubmitting,
                        maxLines = 5
                    )
                    addModalError?.let { errorText ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorText, color = DangerRed, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { if (!addModalSubmitting) showAddRecordModal = false },
                            modifier = Modifier.weight(1f),
                            enabled = !addModalSubmitting
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (addModalMunicipality == null) {
                                    addModalError = "Please select a municipality."
                                    return@Button
                                }
                                if (addModalClaimant.isBlank() || addModalTitleNo.isBlank()) {
                                    addModalError = "Claimant and Title No. are required."
                                    return@Button
                                }
                                addModalError = null
                                addModalSubmitting = true
                                onSaveRecord(
                                    NewRecordInput(
                                        id = editingRecordId,
                                        municipality = addModalMunicipality!!.label,
                                        claimant = addModalClaimant,
                                        titleNo = addModalTitleNo,
                                        odtsNo = addModalOdtsNo,
                                        lotNo = addModalLotNo,
                                        surveyNo = addModalSurveyNo,
                                        areaHas = addModalAreaHas,
                                        location = addModalLocation,
                                        transmittedDocuments = addModalTransmittedDocuments,
                                        routeTo = addModalRouteToUser?.label ?: "",
                                        routeToUserId = addModalRouteToUser?.id,
                                        receivedByControlNo = addModalReceivedByControlNo,
                                        remarks = addModalRemarks,
                                        workStatus = addModalStatus
                                    )
                                ) { success ->
                                    addModalSubmitting = false
                                    if (success) {
                                        showAddRecordModal = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (editingRecordId != null) "Record updated successfully" else "New record created successfully")
                                        }
                                    } else {
                                        addModalError = "Unable to save record. Please try again."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !addModalSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) {
                            if (addModalSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else Text(if (editingRecordId != null) "Update" else "Save")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showDeleteConfirm && recordToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false; recordToDelete = null },
                title = { Text("Confirm Delete", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this record? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            val toDelete = recordToDelete
                            showDeleteConfirm = false
                            recordToDelete = null
                            if (toDelete != null) {
                                onDeleteRecord(toDelete) { success, errorMsg ->
                                    coroutineScope.launch {
                                        if (success) {
                                            snackbarHostState.showSnackbar("Record deleted successfully")
                                        } else {
                                            snackbarHostState.showSnackbar(errorMsg ?: "Delete failed")
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirm = false; recordToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAllMunicipalitiesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAllMunicipalitiesSheet = false },
                containerColor = sheetSurface
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth().navigationBarsPadding()) {
                    Text("Select Municipality", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(municipalityCatalog) { option ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedMunicipality == option.value) BrandGreen else if (useLightMode) SurfaceLight else Background)
                                    .clickable {
                                        selectedMunicipality = option.value
                                        showAllMunicipalitiesSheet = false
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.label,
                                    color = if (selectedMunicipality == option.value) selectedChipTextColor else textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsOverviewSection(stats: DashboardStats, useLightMode: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
        item { StatMetricCard("Active Records", stats.activeRecords.toString(), SuccessGreen, useLightMode) }
        item { StatMetricCard("Total Users", stats.totalUsers.toString(), BrandGreen, useLightMode) }
        item { StatMetricCard("Active Users", stats.activeUsers.toString(), AccentGold, useLightMode) }
    }
}

@Composable
fun StatMetricCard(label: String, value: String, statusColor: Color, useLightMode: Boolean) {
    Card(
        modifier = Modifier.size(width = 108.dp, height = 70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLightMode) SurfaceLight.copy(alpha = 0.72f) else SurfaceDark.copy(alpha = 0.72f), contentColor = if (useLightMode) TextDarkLight else TextLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, fontSize = 10.sp, color = if (useLightMode) TextMutedLight else TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (useLightMode) TextDarkLight else TextLight)
                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
            }
        }
    }
}

@Composable
fun RecordCardExpanded(record: RecordItem, onClick: () -> Unit, onDelete: () -> Unit, useLightMode: Boolean) {
    var isExpanded by remember { mutableStateOf(false) }

    // Helper to clean up data
    fun String?.formatValue() = if (this.isNullOrBlank() || this.equals("null", ignoreCase = true)) "—" else this

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLightMode) SurfaceLight.copy(alpha = 0.72f) else SurfaceDark.copy(alpha = 0.72f), contentColor = if (useLightMode) TextDarkLight else TextLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.municipality.formatValue(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (useLightMode) TextDarkLight else TextLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Claimant: ${record.claimant.formatValue()}",
                        fontSize = 14.sp,
                        color = if (useLightMode) TextDarkLight else TextLight,
                        lineHeight = 18.sp
                    )
                }

                val isFinished = record.status.equals("finished", ignoreCase = true)
                val pillBg = if (isFinished) SuccessGreen.copy(alpha = 0.24f) else AccentGold.copy(alpha = 0.28f)
                val pillText = if (useLightMode) TextDarkLight else TextLight

                Surface(color = pillBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = if (isFinished) "Finished" else "Pending",
                        color = pillText,
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

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onClick() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onDelete() },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
