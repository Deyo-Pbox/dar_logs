package com.example.darlogs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.darlogs.ui.LoginScreen
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager
import com.example.darlogs.utils.NetworkUtils
import kotlinx.coroutines.*
import org.json.JSONObject

class NewLoginActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val PREFS_NAME = "dar_logs_login_prefs"
        private const val KEY_LAST_USERNAME = "last_username"
        private const val KEY_LAST_IS_ADMIN = "last_is_admin"
        private const val KEY_LAST_USER_ROLE = "last_user_role"
    }

    private var currentBiometricPrompt: BiometricPrompt? = null

    private fun getUniqueHardwareId(): String = 
        android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)

    private fun getFingerprintKey(username: String): String = 
        "fingerprint_enabled_${username.trim().lowercase()}_${getUniqueHardwareId()}"

    private fun getFingerprintDeviceKey(username: String): String = 
        "fingerprint_device_${username.trim().lowercase()}"

    private fun getSavedPasswordKey(username: String): String = 
        "saved_password_${username.lowercase()}_${getUniqueHardwareId()}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize()
        ThemeManager.initialize(this)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val profilePrefs = getSharedPreferences("dar_logs_profile_prefs", MODE_PRIVATE)
        val savedUsername = prefs.getString(KEY_LAST_USERNAME, "").orEmpty()

        setContent {
            val useLightMode = ThemeManager.useLightMode
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            
            val isBiometricAvailable = remember(savedUsername) {
                savedUsername.isNotEmpty() && isFingerprintEnabledForUsername(profilePrefs, savedUsername) && 
                BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            }

            LaunchedEffect(isBiometricAvailable) {
                if (isBiometricAvailable) {
                    showBiometricLoginPrompt(savedUsername, prefs) { isLoading = it }
                }
            }

            androidx.compose.material3.MaterialTheme(
                colorScheme = if (useLightMode) DarLightColorScheme else DarDarkColorScheme
            ) {
                LoginScreen(
                    lastUsername = savedUsername,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onLogin = { u, p ->
                        if (u.isEmpty() || p.isEmpty()) {
                            errorMessage = getString(R.string.error_fill_fields)
                        } else {
                            errorMessage = null
                            startLoginRequest(u, p, prefs) { loading, error ->
                                isLoading = loading
                                errorMessage = error
                            }
                        }
                    },
                    onBiometricLogin = {
                        showBiometricLoginPrompt(savedUsername, prefs) { isLoading = it }
                    },
                    isBiometricAvailable = isBiometricAvailable
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun startLoginRequest(
        username: String,
        password: String,
        prefs: SharedPreferences,
        onResult: (Boolean, String?) -> Unit
    ) {
        onResult(true, null)

        if (!NetworkUtils.isOnline(this)) {
            activityScope.launch {
                try {
                    val repository = com.example.darlogs.data.RecordRepository(this@NewLoginActivity)
                    val user = withContext(Dispatchers.IO) {
                        repository.authenticateOffline(username, password)
                    }
                    
                    if (user != null) {
                        prefs.edit()
                            .putString(KEY_LAST_USERNAME, user.username)
                            .putBoolean(KEY_LAST_IS_ADMIN, user.role == "admin")
                            .putString(KEY_LAST_USER_ROLE, user.role)
                            .putString(getSavedPasswordKey(user.username), password)
                            .apply()

                        withContext(Dispatchers.IO) {
                            val repository = com.example.darlogs.data.RecordRepository(this@NewLoginActivity)
                            repository.refreshAll()
                        }
                        
                        onResult(false, null)
                        openDashboardForSavedSession(user.username, prefs)
                    } else {
                        onResult(false, "Offline: Account not found or password incorrect.")
                    }
                } catch (e: Exception) {
                    onResult(false, "System Error: ${e.message}")
                }
            }
            return
        }

        activityScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { performLogin(username, password) }
                if (result.success) {
                    val rememberedUsername = result.username ?: username
                    prefs.edit()
                        .putString(KEY_LAST_USERNAME, rememberedUsername)
                        .putBoolean(KEY_LAST_IS_ADMIN, result.isAdmin)
                        .putString(KEY_LAST_USER_ROLE, if (result.isAdmin) "admin" else "user")
                        .putString(getSavedPasswordKey(rememberedUsername), password)
                        .apply()

                    persistAuthenticatedUserLocally(
                        username = rememberedUsername,
                        password = password,
                        userId = result.userId,
                        role = if (result.isAdmin) "admin" else "user"
                    )

                    withContext(Dispatchers.IO) {
                        val repository = com.example.darlogs.data.RecordRepository(this@NewLoginActivity)
                        repository.refreshAll()
                    }

                    onResult(false, null)
                    openDashboardForSavedSession(rememberedUsername, prefs)
                } else {
                    onResult(false, result.message)
                }
            } catch (e: Exception) {
                onResult(false, "Network Error: ${e.message}")
            }
        }
    }

    private fun showBiometricLoginPrompt(
        username: String, 
        prefs: SharedPreferences,
        onLoadingChange: (Boolean) -> Unit
    ) {
        val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val savedPassword = prefs.getString(getSavedPasswordKey(username), null)
                if (savedPassword != null) {
                    startLoginRequest(username, savedPassword, prefs) { loading, error ->
                        onLoadingChange(loading)
                        if (error != null) Toast.makeText(this@NewLoginActivity, error, Toast.LENGTH_LONG).show()
                    }
                } else {
                    activityScope.launch {
                        try {
                            val user = withContext(Dispatchers.IO) {
                                val db = com.example.darlogs.data.AppDatabase.getDatabase(this@NewLoginActivity)
                                db.recordDao().getUserByUsername(username)
                            }
                            
                            if (user != null) {
                                openDashboardForSavedSession(user.username, prefs)
                            } else {
                                Toast.makeText(this@NewLoginActivity, "Biometric verified, but account not synced to this device yet.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@NewLoginActivity, "System Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(this@NewLoginActivity, errString, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(this@NewLoginActivity, "Biometric not recognized. Please try again.", Toast.LENGTH_SHORT).show()
            }
        })

        currentBiometricPrompt = prompt
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sign in with fingerprint")
            .setSubtitle("Continue as $username")
            .setAllowedAuthenticators(allowedAuthenticators)
            .setNegativeButtonText("Use password instead")
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun openDashboardForSavedSession(username: String, prefs: SharedPreferences) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("username", username)
        intent.putExtra("isAdmin", prefs.getBoolean(KEY_LAST_IS_ADMIN, false))
        intent.putExtra("userRole", prefs.getString(KEY_LAST_USER_ROLE, null) ?: if (prefs.getBoolean(KEY_LAST_IS_ADMIN, false)) "admin" else "user")
        startActivity(intent)
        finish()
    }

    private fun isFingerprintEnabledForUsername(profilePrefs: SharedPreferences, username: String): Boolean {
        val normalized = username.trim().lowercase()
        if (normalized.isEmpty()) return false

        val deviceId = getUniqueHardwareId()
        val key = getFingerprintKey(normalized)
        val registeredDeviceId = profilePrefs.getString(getFingerprintDeviceKey(normalized), null)

        return profilePrefs.getBoolean(key, false) && registeredDeviceId == deviceId
    }

    private fun persistAuthenticatedUserLocally(username: String, password: String, userId: Int, role: String) {
        activityScope.launch {
            try {
                val hashedPassword = at.favre.lib.crypto.bcrypt.BCrypt.withDefaults().hashToString(12, password.toCharArray())
                val entity = com.example.darlogs.data.UserEntity(
                    id = userId.takeIf { it > 0 } ?: 0,
                    username = username,
                    password = hashedPassword,
                    role = role,
                    approved = 1,
                    created_at = null,
                    updated_at = null,
                    last_activity = null
                )

                withContext(Dispatchers.IO) {
                    val db = com.example.darlogs.data.AppDatabase.getDatabase(this@NewLoginActivity)
                    db.recordDao().insertUsers(listOf(entity))
                }
            } catch (e: Exception) {
                Toast.makeText(this@NewLoginActivity, "Unable to save local login credentials: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(username: String, password: String): LoginResult {
        return try {
            val apiUrl = getString(R.string.login_api_url)
            val requestBody = JSONObject().apply {
                put("username", username)
                put("password", password)
            }.toString()

            val response = ApiClient.postJson(apiUrl, requestBody)
            if (!response.success || response.json == null) {
                return LoginResult(false, getString(R.string.error_network), null, false, 0)
            }

            val success = response.json.optBoolean("success", false)
            val message = response.json.optString("message", getString(R.string.error_login_failed))
            val user = response.json.optJSONObject("user")
            val responseUsername = user?.optString("username")
            val isAdmin = user?.optString("role") == "admin"
            val userId = user?.optInt("id", 0) ?: 0

            LoginResult(success, message, responseUsername, isAdmin, userId)
        } catch (ex: Exception) {
            LoginResult(false, getString(R.string.error_network), null, false, 0)
        }
    }

    private data class LoginResult(val success: Boolean, val message: String, val username: String?, val isAdmin: Boolean, val userId: Int)
}
