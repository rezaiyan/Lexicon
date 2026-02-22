package com.alirezaiyan.vokab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.androidx.viewmodel.ext.android.viewModel
import presentation.model.AppUiState
import presentation.ui.LexiconApp
import presentation.viewmodel.AppNavigationViewModel

class MainActivity : ComponentActivity() {

    private val appNavigationViewModel: AppNavigationViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            appNavigationViewModel.appUiState.value is AppUiState.Splash
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LexiconApp()
        }
    }
    
}
