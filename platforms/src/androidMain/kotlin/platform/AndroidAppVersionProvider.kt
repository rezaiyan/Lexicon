package platform

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

/**
 * Android implementation of IAppVersionProvider
 * Retrieves version information from Android's PackageManager
 */
class AndroidAppVersionProvider(
    private val context: Context
) : IAppVersionProvider {
    
    override fun getVersion(): String {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: DEFAULT_VERSION
        } catch (e: Exception) {
            DEFAULT_VERSION
        }
    }
    
    companion object {
        private const val DEFAULT_VERSION = "1.0.0"
    }
}

