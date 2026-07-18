package com.example.darlogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    username: String,
    records: List<RecordItem>,
    isLoading: Boolean,
    onRestoreRecord: (RecordItem, (Boolean) -> Unit) -> Unit,
    onDeleteRecord: (RecordItem, (Boolean, String?) -> Unit) -> Unit,
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
    var selectedYear by remember { mutableStateOf("All years") }
    var selectedMonth by remember { mutableStateOf("All months") }
    val focusManager = LocalFocusManager.current

    // Generate years for filter (from data)
    val availableYears = remember(records) {
        val years = mutableSetOf<String>()
        years.add("All years")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        for (i in 0..5) years.add((currentYear - i).toString())
        years.toList().sortedByDescending { it }
    }

    val months = listOf(
        "All months", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val filteredRecords = remember(records, searchQuery, selectedYear, selectedMonth) {
        records.filter { record ->
            val matchesQuery = searchQuery.isBlank() || listOf(
                record.claimant,
                record.titleNo,
                record.location,
                record.municipality
            ).any { it.contains(searchQuery, ignoreCase = true) }
            
            val matchesYear = selectedYear == "All years"
            val matchesMonth = selectedMonth == "All months"
            
            matchesQuery && matchesYear && matchesMonth
        }
    }

    val municipalityCounts = remember(records) {
        municipalityCatalog.associate { option ->
            option.value to records.count { it.municipality.equals(option.value, ignoreCase = true) }
        }
    }

    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 10
    val totalPages = remember(filteredRecords) { maxOf(1, (filteredRecords.size + pageSize - 1) / pageSize) }
    val pagedRecords = remember(filteredRecords, currentPage) {
        filteredRecords.drop((currentPage - 1) * pageSize).take(pageSize)
    }

    LaunchedEffect(searchQuery, selectedYear, selectedMonth) {
        currentPage = 1
    }

    val bgColor = if (useLightMode) BackgroundLight else BackgroundDark
    val surfaceColor = if (useLightMode) SurfaceLight else SurfaceDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    val searchBorderColor = if (useLightMode) LineColorLight else Surface
    
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = onSyncRecords)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var recordToRestore by remember { mutableStateOf<RecordItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<RecordItem?>(null) }

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
        },
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
                    .background(if (useLightMode) Color.Black.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.08f))
            ) {}

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Archive",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Archived logs are visible only to the user who archived them until restored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTextColor
                        )
                    }
                }

                item {
                    // Filter Section
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Filter Created date", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterDropdown(
                                    label = "Year",
                                    selected = selectedYear,
                                    options = availableYears,
                                    onSelect = { selectedYear = it },
                                    modifier = Modifier.weight(1f),
                                    useLightMode = useLightMode
                                )
                                FilterDropdown(
                                    label = "Month",
                                    selected = selectedMonth,
                                    options = months,
                                    onSelect = { selectedMonth = it },
                                    modifier = Modifier.weight(1f),
                                    useLightMode = useLightMode
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search archived logs...", color = mutedTextColor, fontSize = 14.sp) },
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
                        Text("Archived List", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
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
                            Text("No archived records found", color = mutedTextColor)
                        }
                    }
                } else {
                    items(pagedRecords) { record ->
                        ArchivedRecordCard(
                            record = record,
                            onRestore = { recordToRestore = record; showRestoreConfirm = true },
                            onDelete = { recordToDelete = record; showDeleteConfirm = true },
                            useLightMode = useLightMode
                        )
                    }
                }

                // Browse by Municipality Section
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(BrandGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.GridView, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Browse archive by municipality", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 1000.dp)
                        ) {
                            items(municipalityCatalog) { option ->
                                val count = municipalityCounts[option.value] ?: 0
                                MunicipalityArchiveCard(
                                    label = option.label,
                                    count = count,
                                    useLightMode = useLightMode
                                )
                            }
                        }
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

    if (showRestoreConfirm && recordToRestore != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; recordToRestore = null },
            title = { Text("Restore Record", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to restore this record to the active list?") },
            confirmButton = {
                Button(
                    onClick = {
                        val toRestore = recordToRestore
                        showRestoreConfirm = false
                        recordToRestore = null
                        if (toRestore != null) {
                            onRestoreRecord(toRestore) { success ->
                                coroutineScope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Record restored successfully")
                                    } else {
                                        snackbarHostState.showSnackbar("Restore failed")
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) { Text("Restore") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false; recordToRestore = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm && recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; recordToDelete = null },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this archived record? This action cannot be undone.") },
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
                                        snackbarHostState.showSnackbar("Record permanently deleted")
                                    } else {
                                        snackbarHostState.showSnackbar(msg ?: "Delete failed")
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false; recordToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    useLightMode: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val textColor = if (useLightMode) TextDarkLight else TextLight

    Box(modifier = modifier) {
        Column {
            Text(label, fontSize = 11.sp, color = if (useLightMode) TextMutedLight else TextMuted, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (useLightMode) LineColorLight else LineColor),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selected, fontSize = 13.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (useLightMode) SurfaceLight else SurfaceDark)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MunicipalityArchiveCard(
    label: String,
    count: Int,
    useLightMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useLightMode) SurfaceLight.copy(alpha = 0.72f) else SurfaceDark.copy(alpha = 0.72f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            
            Surface(
                color = BrandGreen.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(count.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandGreen.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ArchivedRecordCard(
    record: RecordItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
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
            containerColor = if (useLightMode) SurfaceLight.copy(alpha = 0.72f) else SurfaceDark.copy(alpha = 0.72f),
            contentColor = if (useLightMode) TextDarkLight else TextLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

                Surface(
                    color = Color.Gray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "Archived",
                        color = if (useLightMode) Color.DarkGray else Color.LightGray,
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
                    DetailRow("Status", record.status, useLightMode)
                    DetailRow("Updated By", record.updatedBy, useLightMode)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onRestore,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
