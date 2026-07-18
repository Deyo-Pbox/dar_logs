package com.example.darlogs.ui

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.darlogs.R
import com.example.darlogs.ui.theme.*
import com.example.darlogs.utils.NetworkUtils

private fun getUniqueHardwareId(context: Context): String = 
    android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)

private fun fingerprintPreferenceKey(context: Context, username: String): String = 
    "fingerprint_enabled_${username.trim().lowercase()}_${getUniqueHardwareId(context)}"

private fun fingerprintDeviceKey(username: String): String = 
    "fingerprint_device_${username.trim().lowercase()}"

private fun getSavedPasswordKey(context: Context, username: String): String = 
    "saved_password_${username.trim().lowercase()}_${getUniqueHardwareId(context)}"

private fun isFingerprintEnabledForUsername(context: Context, prefs: android.content.SharedPreferences, username: String): Boolean {
    val normalized = username.trim().lowercase()
    if (normalized.isEmpty()) return false

    val deviceId = getUniqueHardwareId(context)
    val key = fingerprintPreferenceKey(context, normalized)
    val registeredDeviceId = prefs.getString(fingerprintDeviceKey(normalized), null)

    return prefs.getBoolean(key, false) && registeredDeviceId == deviceId
}

private fun setFingerprintEnabledForUsername(context: Context, prefs: android.content.SharedPreferences, username: String, enabled: Boolean) {
    val normalized = username.trim().lowercase()
    if (normalized.isEmpty()) return

    val deviceId = getUniqueHardwareId(context)
    prefs.edit()
        .putBoolean(fingerprintPreferenceKey(context, normalized), enabled)
        .putString(fingerprintDeviceKey(normalized), if (enabled) deviceId else null)
        .remove("fingerprint_enabled")
        .apply()
}

@Composable
fun DARTopAppBar(
    username: String,
    isAdmin: Boolean,
    useLightMode: Boolean,
    onNotificationsClick: () -> Unit,
    onSyncRecords: () -> Unit,
    onManageAccounts: () -> Unit,
    onLogout: () -> Unit,
    onToggleLightMode: (Boolean) -> Unit
) {
    var isAvatarMenuOpen by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    
    // Confirmation Dialog States
    var showUnregisterConfirm by remember { mutableStateOf(false) }
    var showUnlinkConfirm by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("dar_logs_profile_prefs", Context.MODE_PRIVATE) }
    val profileNameKey = remember(username) { "profile_name_${username.trim().lowercase()}" }
    var displayName by remember(username) { mutableStateOf(prefs.getString(profileNameKey, username).orEmpty().ifBlank { username }) }
    var fingerprintEnabled by remember(username) { mutableStateOf(isFingerprintEnabledForUsername(context, prefs, username)) }
    
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted
    val surfaceColor = if (useLightMode) SurfaceLight else SurfaceDark
    
    val glassColor = if (useLightMode) {
        Color.White.copy(alpha = 0.85f)
    } else {
        Color(0xFF0A1F14).copy(alpha = 0.75f)
    }

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = if (useLightMode) 8.dp else 0.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (useLightMode) PrimaryHeaderLightGradient else PrimaryHeaderDarkGradient)
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glassColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f),
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.dar_logo_white_bg),
                                contentDescription = "DAR logo",
                                modifier = Modifier.padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "DAR Activity Logs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                letterSpacing = (-0.5).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NetworkStatusIndicator()
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box {
                            Surface(
                                onClick = { isAvatarMenuOpen = true },
                                shape = CircleShape,
                                color = AccentGold,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = username.take(1).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        color = BrandDeep,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isAvatarMenuOpen,
                                onDismissRequest = { isAvatarMenuOpen = false },
                                modifier = Modifier.background(surfaceColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    DropdownMenuItem(
                                        text = {
                                            Surface(
                                                color = if (useLightMode) BrandGreen.copy(alpha = 0.05f) else BrandGreen.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.15f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically, 
                                                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                            .background(AccentGold),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(displayName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = BrandDeep, fontSize = 20.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            displayName,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = textColor,
                                                            fontSize = 16.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(if (isAdmin) "Administrator" else "User", fontSize = 12.sp, color = mutedTextColor)
                                                    }
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = null,
                                                        tint = BrandGreen.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            isAvatarMenuOpen = false
                                            showProfileDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = textColor.copy(alpha = 0.1f))

                                DropdownMenuItem(
                                    text = { Text("Sync Records", fontWeight = FontWeight.Medium) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        isAvatarMenuOpen = false
                                        onSyncRecords()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Theme", fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = if (useLightMode) "Light" else "Dark", color = mutedTextColor, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = { },
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = if (useLightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Switch(
                                            checked = useLightMode,
                                            onCheckedChange = { onToggleLightMode(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = BrandGreen,
                                                uncheckedThumbColor = AccentGold
                                            )
                                        )
                                    }
                                )

                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Manage Accounts", fontWeight = FontWeight.Medium) },
                                        leadingIcon = { Icon(Icons.Default.ManageAccounts, null, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            isAvatarMenuOpen = false
                                            onManageAccounts()
                                        }
                                    )
                                }
                                
                                DropdownMenuItem(
                                    text = { Text("Sign Out", fontWeight = FontWeight.Medium, color = DangerRed) },
                                    leadingIcon = { Icon(Icons.Default.Logout, null, tint = DangerRed, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        isAvatarMenuOpen = false
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        var editedName by remember(displayName) { mutableStateOf(displayName) }
        val dialogSurface = if (useLightMode) SurfaceLight.copy(alpha = 0.97f) else SurfaceDark.copy(alpha = 0.97f)
        val dialogText = if (useLightMode) TextDarkLight else TextLight
        val dialogMuted = if (useLightMode) TextMutedLight else TextMuted
        val dialogBorder = if (useLightMode) LineColorLight else LineColorDark

        AlertDialog(
            containerColor = dialogSurface,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(20.dp),
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    "Account Settings",
                    fontWeight = FontWeight.Bold,
                    color = dialogText,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Display Name", color = dialogMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = dialogText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = dialogBorder,
                            focusedLabelColor = BrandGreen,
                            cursorColor = BrandGreen
                        )
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (useLightMode) SurfaceSecondaryLight.copy(alpha = 0.7f) else SurfaceSecondaryDark.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (useLightMode) LineColorLight else LineColorDark)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fingerprint Login", fontWeight = FontWeight.SemiBold, color = dialogText)
                                Text(
                                    text = if (fingerprintEnabled) "Enabled" else "Not set",
                                    color = if (fingerprintEnabled) SuccessGreen else dialogMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Register a fingerprint for faster secure sign-in.",
                                fontSize = 12.sp,
                                color = dialogMuted
                            )
                            if (fingerprintEnabled) {
                                Button(
                                    onClick = { showUnregisterConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                                ) {
                                    Text("Unregister Fingerprint")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val activity = context as? FragmentActivity
                                        if (activity == null) {
                                            Toast.makeText(context, "Fingerprint setup is unavailable on this device", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val biometricManager = BiometricManager.from(context)
                                        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
                                            BiometricManager.BIOMETRIC_SUCCESS -> {
                                                val executor = ContextCompat.getMainExecutor(context)
                                                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                        setFingerprintEnabledForUsername(context, prefs, username, true)
                                                        fingerprintEnabled = true
                                                        Toast.makeText(context, "Fingerprint enabled", Toast.LENGTH_SHORT).show()
                                                    }

                                                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                        Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                                                    }

                                                    override fun onAuthenticationFailed() {
                                                        Toast.makeText(context, "Fingerprint not recognized. Please try again.", Toast.LENGTH_SHORT).show()
                                                    }
                                                })

                                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                                    .setTitle("Register fingerprint")
                                                    .setSubtitle("Use your fingerprint to secure this account")
                                                    .setAllowedAuthenticators(allowedAuthenticators)
                                                    .setNegativeButtonText("Cancel")
                                                    .build()

                                                prompt.authenticate(promptInfo)
                                            }

                                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Toast.makeText(context, "This device does not have a fingerprint sensor", Toast.LENGTH_SHORT).show()
                                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Toast.makeText(context, "No fingerprints have been enrolled on this device", Toast.LENGTH_SHORT).show()
                                            else -> Toast.makeText(context, "Fingerprint setup is unavailable right now", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
                                ) {
                                    Text("Register Fingerprint")
                                }
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (useLightMode) SurfaceSecondaryLight.copy(alpha = 0.7f) else SurfaceSecondaryDark.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (useLightMode) LineColorLight else LineColorDark)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Device Management", fontWeight = FontWeight.SemiBold, color = dialogText)
                            Text(
                                text = "To use a different account on this device, you must first unlink this account.",
                                fontSize = 12.sp,
                                color = dialogMuted
                            )
                            OutlinedButton(
                                onClick = { showUnlinkConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                            ) {
                                Text("Unlink Account & Sign Out")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = editedName.trim()
                    if (trimmed.isNotBlank()) {
                        displayName = trimmed
                        prefs.edit().putString(profileNameKey, trimmed).apply()
                    }
                    showProfileDialog = false
                }) {
                    Text("Save", color = BrandGreen, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = dialogMuted)
                }
            }
        )
    }

    if (showUnregisterConfirm) {
        DARConfirmDialog(
            title = "Unregister Fingerprint",
            message = "Are you sure you want to disable fingerprint login for this device? You will need to use your password next time.",
            confirmText = "Unregister",
            confirmColor = DangerRed,
            onConfirm = {
                setFingerprintEnabledForUsername(context, prefs, username, false)
                context.getSharedPreferences("dar_logs_login_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .remove(getSavedPasswordKey(context, username))
                    .apply()
                fingerprintEnabled = false
                showUnregisterConfirm = false
                Toast.makeText(context, "Fingerprint unregistered", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showUnregisterConfirm = false },
            useLightMode = useLightMode
        )
    }

    if (showUnlinkConfirm) {
        DARConfirmDialog(
            title = "Unlink Account",
            message = "This will remove your remembered session from this device. You will need to enter your username and password to log in again.",
            confirmText = "Unlink & Sign Out",
            confirmColor = DangerRed,
            onConfirm = {
                context.getSharedPreferences("dar_logs_login_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply()
                showUnlinkConfirm = false
                onLogout()
            },
            onDismiss = { showUnlinkConfirm = false },
            useLightMode = useLightMode
        )
    }
}

@Composable
fun NetworkStatusIndicator() {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    var isOnline by remember { mutableStateOf(NetworkUtils.isOnline(context)) }
    
    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isOnline = true
            }
            override fun onLost(network: android.net.Network) {
                isOnline = false
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isOnline) SuccessGreen else DangerRed, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isOnline) "ONLINE" else "OFFLINE",
            fontSize = 10.sp,
            color = if (isOnline) SuccessGreen else DangerRed,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun DARConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color = BrandGreen,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    useLightMode: Boolean
) {
    val surfaceColor = if (useLightMode) SurfaceLight else SurfaceDark
    val textColor = if (useLightMode) TextDarkLight else TextLight
    val mutedTextColor = if (useLightMode) TextMutedLight else TextMuted

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, color = textColor)
        },
        text = {
            Text(message, color = mutedTextColor, fontSize = 14.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = mutedTextColor)
            }
        },
        containerColor = surfaceColor,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp
    )
}

@Composable
fun InfoItem(label: String, value: String?, modifier: Modifier = Modifier, useLightMode: Boolean) {
    val formattedValue = if (value.isNullOrBlank() || value.equals("null", ignoreCase = true)) "—" else value
    
    Column(modifier = modifier.padding(end = 4.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (useLightMode) TextMutedLight else TextMuted,
            letterSpacing = 0.5.sp
        )
        Text(
            text = formattedValue,
            fontSize = 12.sp,
            color = if (useLightMode) TextDarkLight else TextLight,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DetailRow(label: String, value: String?, useLightMode: Boolean) {
    val formattedValue = if (value.isNullOrBlank() || value.equals("null", ignoreCase = true)) "—" else value
    
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(text = "$label: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (useLightMode) TextDarkLight else TextDark, modifier = Modifier.width(110.dp))
        Text(text = formattedValue, fontSize = 13.sp, color = if (useLightMode) TextMutedLight else TextMuted)
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    useLightMode: Boolean
) {
    Surface(
        modifier = Modifier.border(
            1.dp,
            if (useLightMode) BrandGreen.copy(alpha = 0.18f) else BrandGreen.copy(alpha = 0.24f),
            RoundedCornerShape(24.dp)
        ),
        color = if (useLightMode) SurfaceLight.copy(alpha = 0.20f) else SurfaceDark.copy(alpha = 0.24f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                enabled = currentPage > 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous page",
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "Page $currentPage of $totalPages",
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                color = if (useLightMode) TextDarkLight else TextLight
            )

            IconButton(
                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next page",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
