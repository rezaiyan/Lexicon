package com.alirezaiyan.vokab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import data.auth.state.IAuthenticationStateManager
import data.auth.token.ITokenManager
import data.storage.SecureStorage
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Debug-only BroadcastReceiver that injects CI authentication tokens into the app.
 * This receiver is registered only in the androidDebug manifest and is completely
 * absent from release builds.
 *
 * Usage via adb:
 *   adb shell am broadcast \
 *     -a com.alirezaiyan.vokab.CI_INJECT_TOKENS \
 *     -n com.alirezaiyan.vokab/com.alirezaiyan.vokab.CiTokenReceiver \
 *     --es accessToken "<token>" \
 *     --es refreshToken "<token>" \
 *     --el expiresInMs 86400000
 */
class CiTokenReceiver : BroadcastReceiver(), KoinComponent {

    private val tokenManager: ITokenManager by inject()
    private val authStateManager: IAuthenticationStateManager by inject()
    private val secureStorage: SecureStorage by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val accessToken = intent.getStringExtra("accessToken")
        val refreshToken = intent.getStringExtra("refreshToken")
        val expiresInMs = intent.getLongExtra("expiresInMs", 0L)

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            Log.e(TAG, "Missing accessToken or refreshToken extras")
            return
        }

        Log.i(TAG, "Injecting CI tokens (expiresInMs=$expiresInMs)")

        // runBlocking is acceptable here: EncryptedSharedPreferences I/O is fast
        // and BroadcastReceiver.onReceive must complete synchronously.
        runBlocking {
            tokenManager.saveTokens(accessToken, refreshToken, expiresInMs)
            secureStorage.markOnboardingCompleted()
            authStateManager.setAuthenticated(true)
        }

        Log.i(TAG, "CI tokens injected successfully")
    }

    companion object {
        private const val TAG = "CiTokenReceiver"
    }
}
