@file:OptIn(BetaInteropApi::class)

package data.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytes
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Keychain-backed iOS implementation.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IOSKeychainSecureStorage : IOSPlatformSecureStorage {
    
    private val keychainHelper = KeychainHelper(SERVICE_NAME)
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val preferencesHelper = PreferencesHelper(userDefaults, PREFERENCES_PREFIX)


    private val migrationHelper = MigrationHelper(
        keychainHelper = keychainHelper,
        preferencesHelper = preferencesHelper,
        migrationFlagKey = MIGRATION_FLAG_KEY
    )
    
    override suspend fun saveAccessToken(token: String) {
        ensureMigrationCompleted()
        keychainHelper.save(KEY_ACCESS_TOKEN, token)
    }
    
    override suspend fun saveRefreshToken(token: String) {
        ensureMigrationCompleted()
        keychainHelper.save(KEY_REFRESH_TOKEN, token)
    }
    
    override fun getAccessToken(): String? {
        ensureMigrationCompleted()
        return keychainHelper.load(KEY_ACCESS_TOKEN)
    }
    
    override suspend fun getRefreshToken(): String? {
        ensureMigrationCompleted()
        return keychainHelper.load(KEY_REFRESH_TOKEN)
    }
    
    override suspend fun clearTokens() {
        keychainHelper.delete(KEY_ACCESS_TOKEN)
        keychainHelper.delete(KEY_REFRESH_TOKEN)
        preferencesHelper.remove(KEY_TOKEN_EXPIRES_AT)
        preferencesHelper.remove(KEY_DAILY_INSIGHT_ID)
        preferencesHelper.remove(KEY_DAILY_INSIGHT_DATE)
        preferencesHelper.remove(KEY_DAILY_INSIGHT_TIMESTAMP)
    }

    override suspend fun saveTokenExpiresAt(expiresAtMs: Long) {
        preferencesHelper.putLong(KEY_TOKEN_EXPIRES_AT, expiresAtMs)
    }

    override fun getTokenExpiresAt(): Long {
        return preferencesHelper.getLong(KEY_TOKEN_EXPIRES_AT)
    }
    
    override suspend fun storeDailyInsightData(
        insightId: String,
        date: String,
        timestamp: Long
    ) {
        preferencesHelper.putString(KEY_DAILY_INSIGHT_ID, insightId)
        preferencesHelper.putString(KEY_DAILY_INSIGHT_DATE, date)
        preferencesHelper.putLong(KEY_DAILY_INSIGHT_TIMESTAMP, timestamp)
    }
    
    override suspend fun getDailyInsightData(): DailyInsightData? {
        val insightId = preferencesHelper.getString(KEY_DAILY_INSIGHT_ID)
        val date = preferencesHelper.getString(KEY_DAILY_INSIGHT_DATE)
        val timestamp = preferencesHelper.getLong(KEY_DAILY_INSIGHT_TIMESTAMP)
        
        return if (insightId != null && date != null && timestamp > 0) {
            DailyInsightData(
                insightId = insightId,
                date = date,
                timestamp = timestamp
            )
        } else {
            null
        }
    }
    
    override suspend fun clearDailyInsightData() {
        preferencesHelper.remove(KEY_DAILY_INSIGHT_ID)
        preferencesHelper.remove(KEY_DAILY_INSIGHT_DATE)
        preferencesHelper.remove(KEY_DAILY_INSIGHT_TIMESTAMP)
    }

    override suspend fun hasCompletedOnboarding(): Boolean {
        return preferencesHelper.getString(KEY_ONBOARDING_COMPLETED) != null
    }

    override suspend fun markOnboardingCompleted() {
        preferencesHelper.putString(KEY_ONBOARDING_COMPLETED, "true")
    }

    private fun ensureMigrationCompleted() {
        migrationHelper.runIfNeeded()
    }

    companion object {
        private const val SERVICE_NAME = "com.alirezaiyan.vokab"
        private const val PREFERENCES_PREFIX = "com.alirezaiyan.vokab.secure."
        private const val MIGRATION_FLAG_KEY = "com.alirezaiyan.vokab.keychain_migrated"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_DAILY_INSIGHT_ID = "daily_insight_id"
        private const val KEY_DAILY_INSIGHT_DATE = "daily_insight_date"
        private const val KEY_DAILY_INSIGHT_TIMESTAMP = "daily_insight_timestamp"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private class KeychainHelper(
    private val serviceName: String
) {
    fun save(key: String, value: String): Boolean {
        val dataCF = value.encodeToByteArray().toCFData()

        return memScoped {
            // Build base query (CoreFoundation dictionary)
            val baseQuery: CFMutableDictionaryRef = CFDictionaryCreateMutable(
                kCFAllocatorDefault, 0, null, null
            )!!
            CFDictionarySetValue(baseQuery, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(baseQuery, kSecAttrService, serviceName.toCFString())
            CFDictionarySetValue(baseQuery, kSecAttrAccount, key.toCFString())

            // Try update first
            val attrsToUpdate: CFMutableDictionaryRef = CFDictionaryCreateMutable(
                kCFAllocatorDefault, 0, null, null
            )!!
            CFDictionarySetValue(attrsToUpdate, kSecValueData, dataCF)
            val updateStatus = SecItemUpdate(baseQuery, attrsToUpdate)

            if (updateStatus == errSecSuccess) {
                CFRelease(attrsToUpdate)
                CFRelease(baseQuery)
                true
            } else if (updateStatus == errSecItemNotFound) {
                // Not found: add
                CFDictionarySetValue(baseQuery, kSecValueData, dataCF)
                val addStatus = SecItemAdd(baseQuery, null)
                CFRelease(attrsToUpdate)
                CFRelease(baseQuery)
                addStatus == errSecSuccess
            } else {
                CFRelease(attrsToUpdate)
                CFRelease(baseQuery)
                false
            }
        }
    }
    
    fun load(key: String): String? {
        return memScoped {
            val query: CFMutableDictionaryRef = CFDictionaryCreateMutable(
                kCFAllocatorDefault, 0, null, null
            )!!
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceName.toCFString())
            CFDictionarySetValue(query, kSecAttrAccount, key.toCFString())
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            CFRelease(query)
            if (status == errSecSuccess) {
                result.value?.toByteArray()?.decodeToString()
            } else {
                null
            }
        }
    }
    
    fun delete(key: String): Boolean {
        val query: CFMutableDictionaryRef = CFDictionaryCreateMutable(
            kCFAllocatorDefault, 0, null, null
        )!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceName.toCFString())
        CFDictionarySetValue(query, kSecAttrAccount, key.toCFString())
        val status = SecItemDelete(query)
        CFRelease(query)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}

private class PreferencesHelper(
    private val userDefaults: NSUserDefaults,
    private val prefix: String
) {
    fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = prefixed(key))
    }
    
    fun getString(key: String): String? {
        return userDefaults.stringForKey(prefixed(key))
    }
    
    fun putLong(key: String, value: Long) {
        userDefaults.setDouble(value.toDouble(), forKey = prefixed(key))
    }
    
    fun getLong(key: String): Long {
        return userDefaults.doubleForKey(prefixed(key)).toLong()
    }
    
    fun remove(key: String) {
        userDefaults.removeObjectForKey(prefixed(key))
    }
    
    private fun prefixed(key: String): String = prefix + key
}

private class MigrationHelper(
    private val keychainHelper: KeychainHelper,
    private val preferencesHelper: PreferencesHelper,
    private val migrationFlagKey: String
) {
    private var migrationCompleted = false
    
    fun runIfNeeded() {
        if (migrationCompleted) return
        
        val userDefaults = NSUserDefaults.standardUserDefaults
        if (userDefaults.boolForKey(migrationFlagKey)) {
            migrationCompleted = true
            return
        }
        
        performMigration()
        userDefaults.setBool(true, forKey = migrationFlagKey)
        migrationCompleted = true
    }
    
    private fun performMigration() {
        preferencesHelper.getString(KEY_ACCESS_TOKEN)?.let { token ->
            keychainHelper.save(KEY_ACCESS_TOKEN, token)
            preferencesHelper.remove(KEY_ACCESS_TOKEN)
        }
        
        preferencesHelper.getString(KEY_REFRESH_TOKEN)?.let { token ->
            keychainHelper.save(KEY_REFRESH_TOKEN, token)
            preferencesHelper.remove(KEY_REFRESH_TOKEN)
        }
    }
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    
    val result = ByteArray(size)
    result.usePinned { pinned ->
        getBytes(pinned.addressOf(0), size.toULong())
    }
    return result
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun String.toCFString(): CFStringRef? = memScoped {
    CFStringCreateWithCString(
        kCFAllocatorDefault,
        this@toCFString,
        kCFStringEncodingUTF8
    )
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun ByteArray.toCFData(): CFDataRef? = this.toUByteArray().usePinned { pinned ->
    CFDataCreate(
        kCFAllocatorDefault,
        pinned.addressOf(0),
        this.size.toLong()
    )
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun COpaquePointer?.toByteArray(): ByteArray {
    if (this == null) return ByteArray(0)
    val cfDataRef = this as CFDataRef
    val length = CFDataGetLength(cfDataRef)
    val out = UByteArray(length.toInt())
    out.usePinned { pinned ->
        CFDataGetBytes(cfDataRef, CFRangeMake(0, length), pinned.addressOf(0))
    }
    return out.toByteArray()
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this)
    val out = UByteArray(length.toInt())
    out.usePinned { pinned ->
        CFDataGetBytes(this, CFRangeMake(0, length), pinned.addressOf(0))
    }
    return out.toByteArray()
}


