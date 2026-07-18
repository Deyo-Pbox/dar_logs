package com.example.darlogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MyWorkLogsScreen(
    username: String,
    records: List<RecordItem>,
    isLoading: Boolean,
    onAddRecord: () -> Unit,
    onSaveRecord: (NewRecordInput, (Boolean) -> Unit) -> Unit,
    onDeleteRecord: (RecordItem, (Boolean, String?) -> Unit) -> Unit,
    onArchiveRecord: (RecordItem, (Boolean) -> Unit) -> Unit,
    onToggleStatus: (RecordItem, (Boolean) -> Unit) -> Unit,
    routeToUsers: List<RouteToUserOption>,
    onSyncRecords: () -> Unit,
    onNotificationsClick: () -> Unit,
    onManageAccounts: () -> Unit,
    onLogout: () -> Unit,
    isAdmin: Boolean,
    municipalityCatalog: List<MunicipalityOption>,
    useLightMode: Boolean,
    onToggleLightMode: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

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
    var showAddRecordModal by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<RecordItem?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var recordToArchive by remember { mutableStateOf<RecordItem?>(null) }
    
    // Form State
    var editingRecordId by remember { mutableStateOf<Int?>(null) }
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
    var addModalSubmitting by remember { mutableStateOf(false) }
    var addModalError by remember { mutableStateOf<String?>(null) }
    val addModalScrollState = rememberScrollState()
    
    val openEditRecord: (RecordItem) -> Unit = { record ->
        editingRecordId = record.id
        addModalError = null
        addModalMunicipality = MunicipalityOption(record.municipality, record.municipality)
        addModalClaimant = record.claimant
        addModalTitleNo = record.titleNo
        addModalOdtsNo = record.odtsNo
        addModalLotNo = record.lotNo
        addModalSurveyNo = record.surveyNo
        addModalAreaHas = record.areaHas
        addModalLocation = record.location
        addModalTransmittedDocuments = record.transmittedDocuments
        addModalRouteToUser = routeToUsers.firstOrNull { it.id == record.routeToUserId }
        addModalReceivedByControlNo = record.receivedByControlNo
        addModalRemarks = record.remarks
        addModalStatus = record.status
        showAddRecordModal = true
    }

    val openAddRecord: () -> Unit = {
        editingRecordId = null
        addModalError = null
        addModalMunicipality = null
        addModalClaimant = ""
        addModalTitleNo = ""
        addModalOdtsNo = ""
        addModalLotNo = ""
        addModalSurveyNo = ""
        addModalAreaHas = ""
        addModalLocation = ""
        addModalTransmittedDocuments = ""
        addModalRouteToUser = null
        addModalReceivedByControlNo = ""
        addModalRemarks = ""
        addModalStatus = "not_finished"
        showAddRecordModal = true
        onAddRecord()
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
                        coroutineScope.launch {
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

            FloatingActionButton(
                onClick = openAddRecord,
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
                            text = "My Work Logs",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Track, update, and review your own activity records.",
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
                        placeholder = { Text("Search your work logs...", color = mutedTextColor, fontSize = 14.sp) },
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
                            text = "Personal Records",
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
                            icon = Icons.Default.HistoryEdu,
                            title = "No logs found",
                            subtitle = "Tap + to create your first work log",
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
                            RecordCardExtendedActions(
                                record = record,
                                onEdit = { openEditRecord(record) },
                                onDelete = { recordToDelete = record; showDeleteConfirm = true },
                                onArchive = { recordToArchive = record; showArchiveConfirm = true },
                                onToggleStatus = {
                                    onToggleStatus(record) { success ->
                                        coroutineScope.launch {
                                            if (success) {
                                                val newStatus = if (record.status.equals("finished", ignoreCase = true)) "Pending" else "Finished"
                                                snackbarHostState.showSnackbar("Record marked as $newStatus")
                                            } else {
                                                snackbarHostState.showSnackbar("Failed to update status")
                                            }
                                        }
                                    }
                                },
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
        }
    }

    if (showAddRecordModal) {
        ModalBottomSheet(
            onDismissRequest = { if (!addModalSubmitting) showAddRecordModal = false },
            containerColor = if (useLightMode) SurfaceLight else Color(0xFF071205)
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Municipality") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addModalMunicipalityMenuOpen) },
                        readOnly = true,
                        enabled = !addModalSubmitting
                    )
                    ExposedDropdownMenu(
                        expanded = addModalMunicipalityMenuOpen,
                        onDismissRequest = { addModalMunicipalityMenuOpen = false }
                    ) {
                        OutlinedTextField(
                            value = addModalMunicipalitySearch,
                            onValueChange = { addModalMunicipalitySearch = it },
                            placeholder = { Text("Search municipalities…") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            singleLine = true
                        )
                        municipalityCatalog.filter { it.label.contains(addModalMunicipalitySearch, ignoreCase = true) }.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    addModalMunicipality = option
                                    addModalMunicipalityMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalClaimant,
                    onValueChange = { addModalClaimant = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("LO / Claimant") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalTitleNo,
                    onValueChange = { addModalTitleNo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title No.") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalOdtsNo,
                    onValueChange = { addModalOdtsNo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ODTS No.") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalLotNo,
                    onValueChange = { addModalLotNo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lot No.") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalSurveyNo,
                    onValueChange = { addModalSurveyNo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Survey No.") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalAreaHas,
                    onValueChange = { addModalAreaHas = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Area (has.)") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalLocation,
                    onValueChange = { addModalLocation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalTransmittedDocuments,
                    onValueChange = { addModalTransmittedDocuments = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Transmitted Documents") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = addModalRouteToMenuOpen,
                    onExpandedChange = { if (!addModalSubmitting) addModalRouteToMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = addModalRouteToUser?.label ?: "Select Route Recipient",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Route To") },
                        readOnly = true,
                        enabled = !addModalSubmitting
                    )
                    ExposedDropdownMenu(
                        expanded = addModalRouteToMenuOpen,
                        onDismissRequest = { addModalRouteToMenuOpen = false }
                    ) {
                        OutlinedTextField(
                            value = addModalRouteToSearch,
                            onValueChange = { addModalRouteToSearch = it },
                            placeholder = { Text("Search users…") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            singleLine = true
                        )
                        routeToUsers.filter { it.label.contains(addModalRouteToSearch, ignoreCase = true) }.forEach { option ->
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

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalReceivedByControlNo,
                    onValueChange = { addModalReceivedByControlNo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Received By / Control No.") },
                    enabled = !addModalSubmitting
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = addModalRemarks,
                    onValueChange = { addModalRemarks = it },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    label = { Text("Remarks") },
                    enabled = !addModalSubmitting
                )
                
                addModalError?.let {
                    Text(it, color = DangerRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (addModalMunicipality == null || addModalClaimant.isBlank()) {
                            addModalError = "Municipality and Claimant are required."
                            return@Button
                        }
                        addModalError = null
                        addModalSubmitting = true
                        onSaveRecord(
                            NewRecordInput(
                                id = editingRecordId,
                                municipality = addModalMunicipality?.label ?: "",
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
                                addModalError = "Failed to save record."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !addModalSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    if (addModalSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text(if (editingRecordId != null) "Update" else "Save")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showDeleteConfirm && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; recordToDelete = null },
            title = { Text("Confirm Delete", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this log entry? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = recordToDelete
                        showDeleteConfirm = false
                        recordToDelete = null
                        if (toDelete != null) {
                            onDeleteRecord(toDelete) { success, msg ->
                                coroutineScope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Record deleted successfully")
                                    } else {
                                        snackbarHostState.showSnackbar(msg ?: "Delete failed")
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false; recordToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showArchiveConfirm && recordToArchive != null) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false; recordToArchive = null },
            title = { Text("Confirm Archive", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to archive this record? It will be removed from your active work logs.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toArchive = recordToArchive
                        showArchiveConfirm = false
                        recordToArchive = null
                        if (toArchive != null) {
                            onArchiveRecord(toArchive) { success ->
                                coroutineScope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Record archived successfully")
                                    } else {
                                        snackbarHostState.showSnackbar("Archive failed")
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) { Text("Archive") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showArchiveConfirm = false; recordToArchive = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RecordCardExtendedActions(
    record: RecordItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onToggleStatus: () -> Unit,
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

                val isFinished = record.status.equals("finished", ignoreCase = true)
                val statusLabel = if (isFinished) "Finished" else "Pending"
                val statusColor = if (isFinished) SuccessGreen else AccentGold

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusLabel,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isFinished = record.status.equals("finished", ignoreCase = true)

                        Button(
                            onClick = onToggleStatus,
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFinished) SuccessGreen else AccentGold,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(if (isFinished) "Mark Pending" else "Mark Finished", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (useLightMode) TextDarkLight else TextLight)
                        ) {
                            Text("Edit", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onArchive,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (useLightMode) BrandGreen else SuccessGreen)
                        ) {
                            Text("Archive", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("Delete", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
