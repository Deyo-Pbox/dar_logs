package com.example.darlogs.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    private const val PREFS_NAME = "dar_logs_prefs"
    private const val KEY_USE_LIGHT_MODE = "use_light_mode"

    var useLightMode by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        useLightMode = prefs.getBoolean(KEY_USE_LIGHT_MODE, false)
    }

    fun toggleTheme(context: Context, isLight: Boolean) {
        useLightMode = isLight
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_LIGHT_MODE, isLight).apply()
    }
}
