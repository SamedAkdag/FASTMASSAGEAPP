package com.aistudio.pingring.pgrng

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.aistudio.pingring.pgrng.data.model.AppLanguage
import com.aistudio.pingring.pgrng.ui.PingRingViewModel
import com.aistudio.pingring.pgrng.ui.screens.AuthScreen
import com.aistudio.pingring.pgrng.ui.screens.EmergencyAlertScreen
import com.aistudio.pingring.pgrng.ui.screens.MainScreen
import com.aistudio.pingring.pgrng.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: PingRingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val currentLocale = remember(currentLanguage) {
                Locale(currentLanguage.code)
            }
            val baseContext = LocalContext.current
            val localizedContext = remember(currentLocale, baseContext) {
                val config = Configuration(baseContext.resources.configuration)
                config.setLocale(currentLocale)
                config.setLayoutDirection(currentLocale)
                baseContext.createConfigurationContext(config)
            }
            val configuration = remember(currentLocale, baseContext) {
                Configuration(baseContext.resources.configuration).apply {
                    setLocale(currentLocale)
                    setLayoutDirection(currentLocale)
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalActivityResultRegistryOwner provides this
            ) {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PingRingApp(
                            viewModel = viewModel,
                            currentLanguage = currentLanguage,
                            onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                            onEmergencyAlertActive = { setupLockScreenFlags() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val openAlertId = intent?.getStringExtra("OPEN_ALERT_ID")
        if (openAlertId != null) {
            setupLockScreenFlags()
            val alerts = viewModel.allAlerts.value
            val target = alerts.firstOrNull { it.id == openAlertId }
            if (target != null) {
                viewModel.openAlertInFullScreen(target)
            }
            intent.removeExtra("OPEN_ALERT_ID")
        }
    }

    private fun setupLockScreenFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                keyguardManager?.requestDismissKeyguard(this, null)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
            }
        } catch (e: Exception) {
            // Ignored on environments without lock screen permissions
        }
    }
}

@Composable
fun PingRingApp(
    viewModel: PingRingViewModel,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onEmergencyAlertActive: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val activeFullScreenAlert by viewModel.activeFullScreenAlert.collectAsState()

    // Request Notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(activeFullScreenAlert) {
        if (activeFullScreenAlert != null) {
            onEmergencyAlertActive()
        }
    }

    // High Priority: If there is an active emergency alert, display the Full Screen Red Emergency Alert Screen immediately!
    val currentAlert = activeFullScreenAlert
    if (currentAlert != null) {
        EmergencyAlertScreen(
            alert = currentAlert,
            onAcknowledge = { alertId -> viewModel.acknowledgeAlert(alertId) },
            onDismiss = { alertId -> viewModel.dismissAlertScreen(alertId) }
        )
    } else if (currentUser == null) {
        // Entrance Screen with Language Switcher & High Craft UI
        AuthScreen(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
            onLoginSuccess = { phone, name ->
                viewModel.registerOrLogin(phone, name)
            }
        )
    } else {
        // Main Dashboard Screen
        MainScreen(
            viewModel = viewModel,
            onOpenEmergencyAlert = { alert ->
                viewModel.openAlertInFullScreen(alert)
            }
        )
    }
}
