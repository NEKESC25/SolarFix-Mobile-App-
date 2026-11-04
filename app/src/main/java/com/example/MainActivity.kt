package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.SolarfixApp
import com.example.ui.theme.SolarfixTheme
import com.example.util.MobileSecurityAndMonitoringManager
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 0: Validate & sanitize any incoming deep-link Intent data before processing
        MobileSecurityAndMonitoringManager.validateAndSanitizeActivityIntent(intent)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize UI with SolarfixTheme
        setContent {
            SolarfixTheme {
                SolarfixApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        MobileSecurityAndMonitoringManager.validateAndSanitizeActivityIntent(intent)
    }
}

