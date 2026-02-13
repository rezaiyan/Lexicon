package com.alirezaiyan.vokab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import presentation.ui.LexiconApp

/**
 * Main Activity for Lexicon
 * Handles:
 * - Splash screen
 * - Activity lifecycle analytics
 * - Compose UI setup
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var analytics: FirebaseAnalytics
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        analytics = Firebase.analytics
        
        // Log screen view
        analytics.logEvent("main_activity_opened", null)

        setContent {
            LexiconApp()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        analytics.logEvent("main_activity_closed", null)
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}
