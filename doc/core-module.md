# Core Module

Core utilities in `core/src/`. Provides expect/actual abstractions for platform services.

## AppConfig (`AppConfig.kt`)
```kotlin
expect object AppConfig {
    val VOKAB_BACKEND_URL: String      // e.g., "https://host/api/v1"
    val GOOGLE_SERVER_CLIENT_ID: String // OAuth client ID
    val REVENUECAT_ANDROID_KEY: String
    val REVENUECAT_IOS_KEY: String
}
```

**Android**: Reads from BuildConfig (generated from gradle/local.properties)
**iOS**: Reads from NSBundle Info.plist (populated from Config.xcconfig)
**Web**: From Firebase config

## PlatformDetector (`PlatformDetector.kt`)
```kotlin
expect fun getPlatformName(): String   // "Android" / "iOS"
expect fun isDebugMode(): Boolean
```

## Expect/Actual Functions

| Function | File | Purpose |
|----------|------|---------|
| `BackHandler(enabled, onBack)` | `expects/BackHandler.kt` | System back button (Android: Activity, iOS: ComposeBackHandler) |
| `SystemBarsController(...)` | `expects/SystemBarsController.kt` | Status/nav bar colors |
| `isSystemInDarkTheme()` | `expects/SystemThemeDetector.kt` | System theme detection |
| `shareTextAsFile(title, text, filename)` | `expects/FileSharer.kt` | Share text file |
| `openUrl(url)` | `expects/UrlOpener.kt` | Open URL in browser |
| `logNetwork()`, `logPlatform()` | `expects/NetworkLogger.kt` | Platform logging |
| `getShareService()` | `expects/ShareService.kt` | Share service |

## Expect/Actual Classes

### AppleSignInHelper (`expects/AppleSignIn.kt`)
```kotlin
expect class AppleSignInHelper {
    fun signIn(
        onSuccess: (idToken: String, fullName: String?, appleUserId: String) -> Unit,
        onError: (String) -> Unit
    )
}
```
**iOS**: Posts NSNotification "StartAppleSignIn", listens for success/failure
**Android**: Returns "Apple Sign In is only available on iOS"

### ImagePicker (`expects/ImagePicker.kt`)
```kotlin
expect class ImagePicker {
    suspend fun pickImage(): ByteArray?
    suspend fun takePhoto(): ByteArray?
}
```
**Android**: ActivityResultRegistry + FileProvider
**iOS**: UIImagePickerController + UIImageJPEGRepresentation

## App Events

### OnEvents (`OnEvents.kt`)
```kotlin
@Composable
fun <T> OnEvents(flow: Flow<T>, handler: (T) -> Unit)
```
Validates flow type (not StateFlow, no replay cache on SharedFlow).

### VocabularyEffect (`AppEvents.kt`)
```kotlin
sealed class VocabularyEffect {
    data class ImportSuccess(val count: Int)
    data class ImportError(val message: String)
    data class ImageImportSuccess(val count: Int)
    data class ImageImportError(val message: String)
    data object ImageImportRequiresLogin
    data object ReviewSessionComplete
}
```
