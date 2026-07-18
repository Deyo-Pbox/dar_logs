package com.example.darlogs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.darlogs.ui.CustomBottomNavigation
import com.example.darlogs.ui.NavItem
import com.example.darlogs.ui.theme.*

class DashboardActivity : AppCompatActivity() {
    private val navigationStack = ArrayDeque<Int>()
    private var currentNavItemIndex by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ApiClient.initialize()
        ThemeManager.initialize(this)

        ViewModelProvider(this)[MainViewModel::class.java].refreshAll()

        setContentView(R.layout.activity_dashboard)

        val mainLayout = findViewById<LinearLayout>(R.id.mainDashboardLayout)
        val bottomNavCompose = findViewById<ComposeView>(R.id.bottomNavCompose)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        bottomNavCompose.setContent {
            val useLightMode = ThemeManager.useLightMode

            SideEffect {
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
        ApiClient.clearAuth()
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
