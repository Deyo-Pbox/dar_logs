package com.example.darlogs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.darlogs.ui.CustomBottomNavigation
import com.example.darlogs.ui.NavItem
import com.example.darlogs.ui.theme.BackgroundDark
import com.example.darlogs.ui.theme.BackgroundLight
import com.example.darlogs.ui.theme.DarDarkColorScheme
import com.example.darlogs.ui.theme.DarLightColorScheme
import com.example.darlogs.ui.theme.ThemeManager

import com.example.darlogs.ui.theme.NavigationBarUnderlineLight

class DashboardActivity : AppCompatActivity() {
    private val navigationStack = ArrayDeque<Int>()
    private var currentNavItemIndex by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize()
        ThemeManager.initialize(this)
        
        // Edge-to-edge support
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_dashboard)

        val mainLayout = findViewById<LinearLayout>(R.id.mainDashboardLayout)
        val bottomNavCompose = findViewById<ComposeView>(R.id.bottomNavCompose)
        
        bottomNavCompose.setContent {
            val useLightMode = ThemeManager.useLightMode
            
            // Sync Android View background and System Bars with Compose Theme
            LaunchedEffect(useLightMode) {
                val appBgColor = if (useLightMode) BackgroundLight.toArgb() else BackgroundDark.toArgb()
                val navUnderlineColor = if (useLightMode) NavigationBarUnderlineLight.toArgb() else BackgroundDark.toArgb()
                
                // Update all container backgrounds
                // We set mainLayout to the footer color so the area under the nav bar matches
                if (useLightMode) {
                    mainLayout?.setBackgroundResource(R.drawable.light_mode_nav_area_bg)
                } else {
                    mainLayout?.setBackgroundColor(navUnderlineColor)
                }
                findViewById<View>(R.id.fragmentContainer)?.setBackgroundColor(appBgColor)
                
                // Update System Bar colors
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                
                // Set system bar icons color
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = useLightMode
                controller.isAppearanceLightNavigationBars = useLightMode
            }

            MaterialTheme(colorScheme = if (useLightMode) DarLightColorScheme else DarDarkColorScheme) {
                CustomBottomNavigation(
                    selectedItem = currentNavItemIndex,
                    onItemSelected = { index ->
                        if (index != currentNavItemIndex) {
                            navigationStack.addLast(currentNavItemIndex)
                            currentNavItemIndex = index
                            navigateToSection(index)
                        }
                    },
                    useLightMode = useLightMode
                )
            }
        }

        if (savedInstanceState == null) {
            currentNavItemIndex = 0
            navigateToSection(0)
        }
    }

    private fun navigateToSection(index: Int) {
        val fragment = when (index) {
            0 -> DashboardComposeFragment()
            1 -> MyWorkLogsComposeFragment()
            2 -> PendingRecordsComposeFragment()
            3 -> CompletedRecordsComposeFragment()
            4 -> ArchiveComposeFragment()
            else -> DashboardComposeFragment()
        }
        
        val titleRes = when (index) {
            0 -> R.string.nav_dashboard
            1 -> R.string.nav_my_work_logs
            2 -> R.string.nav_pending
            3 -> R.string.nav_completed
            4 -> R.string.nav_archive
            else -> R.string.nav_dashboard
        }

        currentNavItemIndex = index
        showSection(fragment, titleRes, true)
    }

    override fun onBackPressed() {
        if (navigationStack.isNotEmpty()) {
            val previousIndex = navigationStack.removeLast()
            if (previousIndex >= 0) {
                navigateToSection(previousIndex)
            } else {
                // This shouldn't happen based on current logic but for safety
                currentNavItemIndex = 0
                navigateToSection(0)
            }
        } else if (currentNavItemIndex != 0) {
            currentNavItemIndex = 0
            navigateToSection(0)
        } else {
            super.onBackPressed()
        }
    }

    fun logout() {
        ApiClient.clearCookies()
        val intent = Intent(this, NewLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    fun showSection(fragment: Fragment, titleRes: Int, isMainNav: Boolean = false) {
        if (!isMainNav) {
            // Push current nav item to stack before clearing highlight
            if (currentNavItemIndex != -1) {
                navigationStack.addLast(currentNavItemIndex)
            }
            currentNavItemIndex = -1
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
