package com.example.darlogs.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import kotlinx.coroutines.launch

data class UserAccount(
    val id: Int,
    val username: String,
    val role: String,
    val approved: Int,
    val createdAt: String,
    val lastActivity: String?
)

data class AuditLogEntry(
    val id: Int,
    val username: String,
    val action: String,
    val recordId: Int?,
    val details: String,
    val createdAt: String
)

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    username: String,
    users: List<UserAccount>,
    auditLogs: List<AuditLogEntry>,
    isLoading: Boolean,
    onCreateAccount: (String, String, String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateAccount: (Int, String, String?, String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteAccount: (Int, (Boolean, String?) -> Unit) -> Unit,
    onSyncData: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLogout: () -> Unit,
    isAdmin: Boolean,
    useLightMode: Boolean,
    onToggleLightMode: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val pullRefreshState = rememberPullRefreshState(isLoading, onRefresh = onSyncData)

    val bgColor = if (useLightMode) BackgroundLight else BackgroundDark
    val surfaceColor = if (useLightMode) SurfaceLight else SurfaceDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    val borderColor = if (useLightMode) LineColorLight else LineColor

    // Create Form State
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("user") }
    var isCreating by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    
    // Edit Form State
    var showEditModal by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserAccount?>(null) }
    var editUsername by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = bgColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DARTopAppBar(
                username = username,
                isAdmin = isAdmin,
                useLightMode = useLightMode,
                onNotificationsClick = onNotificationsClick,
                onSyncRecords = onSyncData,
                onManageAccounts = { /* already here */ },
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Account Management",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Control system access and monitor user activities.",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedTextColor
                        )
                    }
                }

                // Current User Profile Section
                item {
                    val currentUser = users.find { it.username == username }
                    if (currentUser != null) {
                        MyProfileCard(
                            user = currentUser,
                            useLightMode = useLightMode,
                            onEdit = {
                                editingUser = currentUser
                                editUsername = currentUser.username
                                editPassword = ""
                                editRole = currentUser.role
                                updateError = null
                                showEditModal = true
                            }
                        )
                    }
                }

                // Register User Section
                item {
                    Column {
                        Text("Register User", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.72f)),
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(32.dp).background(BrandGreen.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PersonAdd, null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Create New Account", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                OutlinedTextField(
                                    value = newUsername,
                                    onValueChange = { newUsername = it },
                                    label = { Text("Username") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    enabled = !isCreating,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                var passwordVisible by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    enabled = !isCreating,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(image, null)
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("Assign Role", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RoleCard(
                                        title = "User",
                                        description = "Standard Access",
                                        icon = Icons.Default.Person,
                                        selected = newRole == "user",
                                        onClick = { if (!isCreating) newRole = "user" },
                                        modifier = Modifier.weight(1f),
                                        useLightMode = useLightMode
                                    )
                                    RoleCard(
                                        title = "Admin",
                                        description = "Full Control",
                                        icon = Icons.Default.Security,
                                        selected = newRole == "admin",
                                        onClick = { if (!isCreating) newRole = "admin" },
                                        modifier = Modifier.weight(1f),
                                        useLightMode = useLightMode
                                    )
                                }
                                
                                createError?.let {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = DangerRed.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(it, color = DangerRed, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        if (newUsername.length < 3 || newPassword.length < 6) {
                                            createError = "Username (3+) and Password (6+) required"
                                            return@Button
                                        }
                                        createError = null
                                        isCreating = true
                                        onCreateAccount(newUsername, newPassword, newRole) { success, msg ->
                                            isCreating = false
                                            if (success) {
                                                newUsername = ""
                                                newPassword = ""
                                                newRole = "user"
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Account created successfully") }
                                            } else {
                                                createError = msg ?: "Error creating account"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    enabled = !isCreating && newUsername.isNotBlank() && newPassword.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isCreating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    else Text("Create Account", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // User List Section
                item {
                    Text("All Registered Accounts", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                }
                
                val otherUsers = users.filter { it.username != username }
                if (otherUsers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No other users found", color = mutedTextColor, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(otherUsers) { user ->
                        AccountCard(
                            user = user,
                            isCurrentUser = false,
                            onEdit = {
                                editingUser = user
                                editUsername = user.username
                                editPassword = ""
                                editRole = user.role
                                updateError = null
                                showEditModal = true
                            },
                            onDelete = {
                                userToDelete = user
                                showDeleteConfirm = true
                            },
                            useLightMode = useLightMode
                        )
                    }
                }

                // Audit Logs Section
                item {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(AccentGold.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.History, null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("System Audit Logs", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Text("Recent system-wide actions and updates.", fontSize = 12.sp, color = mutedTextColor, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                items(auditLogs) { log ->
                    AuditLogCard(log = log, useLightMode = useLightMode)
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

    if (showEditModal && editingUser != null) {
        ModalBottomSheet(
            onDismissRequest = { if (!isUpdating) showEditModal = false },
            containerColor = if (useLightMode) SurfaceLight else Color(0xFF071205)
        ) {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
                Text("Update Account", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = editUsername,
                    onValueChange = { editUsername = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editPassword,
                    onValueChange = { editPassword = it },
                    label = { Text("New Password (optional)") },
                    placeholder = { Text("Leave blank to keep current") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isUpdating
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Role", fontWeight = FontWeight.SemiBold, color = textColor)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    RoleCard(
                        title = "User",
                        description = "Standard access",
                        icon = Icons.Default.Person,
                        selected = editRole == "user",
                        onClick = { if (!isUpdating) editRole = "user" },
                        modifier = Modifier.weight(1f),
                        useLightMode = useLightMode
                    )
                    RoleCard(
                        title = "Admin",
                        description = "Full system access",
                        icon = Icons.Default.Security,
                        selected = editRole == "admin",
                        onClick = { if (!isUpdating) editRole = "admin" },
                        modifier = Modifier.weight(1f),
                        useLightMode = useLightMode
                    )
                }
                
                updateError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = DangerRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (editUsername.length < 3) {
                            updateError = "Username is too short"
                            return@Button
                        }
                        updateError = null
                        isUpdating = true
                        onUpdateAccount(editingUser!!.id, editUsername, editPassword.takeIf { it.isNotBlank() }, editRole) { success, msg ->
                            isUpdating = false
                            if (success) {
                                showEditModal = false
                                coroutineScope.launch { snackbarHostState.showSnackbar("Account updated successfully") }
                            } else {
                                updateError = msg ?: "Error updating account"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdating && editUsername.length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("Update Account")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteConfirm && userToDelete != null) {
        DARConfirmDialog(
            title = "Confirm Deletion",
            message = "Are you sure you want to delete the account for '${userToDelete!!.username}'? This action is permanent and will remove all access for this user.",
            confirmText = "Delete Account",
            confirmColor = DangerRed,
            onConfirm = {
                val toDelete = userToDelete
                showDeleteConfirm = false
                userToDelete = null
                if (toDelete != null) {
                    onDeleteAccount(toDelete.id) { success, msg ->
                        coroutineScope.launch {
                            if (success) snackbarHostState.showSnackbar("Account deleted successfully")
                            else snackbarHostState.showSnackbar(msg ?: "Deletion failed")
                        }
                    }
                }
            },
            onDismiss = { showDeleteConfirm = false; userToDelete = null },
            useLightMode = useLightMode
        )
    }
}

@Composable
fun MyProfileCard(
    user: UserAccount,
    useLightMode: Boolean,
    onEdit: () -> Unit
) {
    val textColor = if (useLightMode) TextDarkLight else TextLight
    
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text("Your Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
            border = BorderStroke(1.5.dp, BrandGreen.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.username.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.username, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = textColor)
                    Surface(
                        color = AccentGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            user.role.uppercase(), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = AccentGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.ManageAccounts, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useLightMode: Boolean
) {
    val borderColor = if (selected) BrandGreen else (if (useLightMode) LineColorLight else LineColor).copy(alpha = 0.3f)
    val bgColor = if (selected) BrandGreen.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (useLightMode) TextDarkLight else TextLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, borderColor),
        color = bgColor,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(if (selected) BrandGreen else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (selected) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Text(description, fontSize = 10.sp, color = if (useLightMode) TextMutedLight else TextMuted, lineHeight = 12.sp)
        }
    }
}

@Composable
fun AccountCard(
    user: UserAccount,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    useLightMode: Boolean
) {
    val textColor = if (useLightMode) TextDarkLight else TextLight
    
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useLightMode) SurfaceLight else SurfaceDark.copy(alpha = 0.72f)
        ),
        border = if (useLightMode) BorderStroke(1.dp, LineColorLight.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (useLightMode) 2.dp else 0.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (user.role == "admin") AccentGold.copy(alpha = 0.2f) else BrandGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.username.take(1).uppercase(), 
                    fontWeight = FontWeight.Bold, 
                    color = if (user.role == "admin") AccentGold else BrandGreen
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val roleColor = if (user.role == "admin") AccentGold else SuccessGreen
                    Box(modifier = Modifier.size(6.dp).background(roleColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(user.role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = roleColor)
                }
                Text(
                    "Active: ${user.lastActivity ?: "Never"}", 
                    fontSize = 10.sp, 
                    color = if (useLightMode) TextMutedLight else TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            Row {
                IconButton(onClick = onEdit) { 
                    Icon(Icons.Default.Edit, "Edit", tint = BrandGreen, modifier = Modifier.size(20.dp)) 
                }
                if (!isCurrentUser) {
                    IconButton(onClick = onDelete) { 
                        Icon(Icons.Default.Delete, "Delete", tint = DangerRed, modifier = Modifier.size(20.dp)) 
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLogEntry, useLightMode: Boolean) {
    val textColor = if (useLightMode) TextDarkLight else TextLight
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (useLightMode) SurfaceLight else SurfaceDark.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, (if (useLightMode) LineColorLight else LineColor).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (useLightMode) 1.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).background(BrandGreen.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(log.username.take(1).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(log.username, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                }
                Text(log.createdAt, fontSize = 10.sp, color = if (useLightMode) TextMutedLight else TextMuted)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(log.details, fontSize = 13.sp, color = textColor, lineHeight = 18.sp)
            
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val actionColor = when(log.action.lowercase()) {
                    "add" -> SuccessGreen
                    "delete" -> DangerRed
                    "edit", "update" -> AccentGold
                    else -> AccentGold
                }
                Surface(
                    color = actionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        log.action.uppercase(), 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = actionColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (log.recordId != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record #${log.recordId}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (useLightMode) TextMutedLight else TextMuted)
                }
            }
        }
    }
}
