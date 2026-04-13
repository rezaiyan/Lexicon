# Android Security — Lexicon

Security rules and checks specific to Lexicon's KMP/Android codebase.

**Full audit command:** `/android-security-scan`
**Reference:** [android-security-awesome](https://github.com/ashishb/android-security-awesome)

---

## Non-Negotiable Rules

These are hard requirements — never ship code that violates them.

| Rule | Why |
|------|-----|
| **No secrets in source** | `local.properties` is gitignored; never put keys in `BuildConfig` or `strings.xml` |
| **HTTPS everywhere** | `usesCleartextTraffic="false"`, no `http://` backend URLs in production |
| **No custom TrustManager that accepts all certs** | Silent MITM vulnerability |
| **No PII in logs** | Emails, tokens, word lists — none of it in `Log.*` |
| **EncryptedSharedPreferences for sensitive local data** | Tokens, user preferences with PII |
| **`android:debuggable="false"` in release** | Verified by `build.yml` CI |

---

## Auth Token Storage

Lexicon uses JWT access tokens. Rules:

```kotlin
// ✅ Store in EncryptedSharedPreferences or Keystore-backed storage
// The platforms/ module handles this via expect/actual SecureStorage

// ❌ Never store tokens in:
// - Plain SharedPreferences
// - SQLDelight database (unencrypted)
// - External storage
// - Memory-cached in a companion object (survives process restart, accessible)
```

The `SecureStorage` expect/actual bridge in `platforms/` is the ONLY permitted location for auth tokens.

---

## Network Security Config

`res/xml/network_security_config.xml` must:
- Restrict cleartext to `localhost` / `10.0.2.2` (emulator) only — never production domains
- Define certificate pins for the production backend domain (add when cert is known)

```xml
<!-- ✅ Production domain — HTTPS only, no cleartext -->
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">your-backend.com</domain>
</domain-config>

<!-- ✅ Dev/emulator only — cleartext allowed locally -->
<domain-config cleartextTrafficPermitted="true">
    <domain>localhost</domain>
    <domain>10.0.2.2</domain>
</domain-config>
```

---

## Ktor Interceptors — Security Rules

`PlatformHeaderInterceptor` and any future interceptors:

- **Never log `Authorization` header** — even at `DEBUG` level
- **Never log request body** for auth endpoints (`/api/v1/auth/*`)
- `X-Platform` and `X-App-Version` headers: safe to log (non-sensitive)

```kotlin
// ✅ Safe to log
logger.d { "Platform: ${request.headers["X-Platform"]}" }

// ❌ Never log
logger.d { "Auth: ${request.headers["Authorization"]}" }
logger.d { "Body: ${request.body}" }  // may contain tokens/passwords
```

---

## AndroidManifest Checklist

Before every release, verify:

- [ ] `android:debuggable` absent or `false` in release variant
- [ ] `android:allowBackup="false"` (Lexicon data should not appear in ADB backups)
- [ ] All `<activity>` / `<service>` / `<receiver>` that are internal have `android:exported="false"`
- [ ] Deep link `<intent-filter>` activities validate incoming URIs before processing
- [ ] Permissions declared are actually used — remove unused permissions

---

## WebView (if introduced)

Lexicon does not currently use WebView. If one is added:

```kotlin
// MANDATORY if a WebView is ever added
webView.settings.apply {
    javaScriptEnabled = false          // enable only if strictly required
    allowFileAccessFromFileURLs = false
    allowUniversalAccessFromFileURLs = false
    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
}
// Do NOT call addJavascriptInterface() with untrusted content
```

---

## Dependency Security

When bumping dependencies in `gradle/libs.versions.toml`:

1. Check [Android Security Bulletins](https://source.android.com/security/bulletin/) for affected versions
2. Prefer even minor version bumps — security patches are usually patch releases
3. Firebase, Ktor, RevenueCat — subscribe to their security advisories
4. Run `./gradlew dependencyUpdates` to surface outdated dependencies

---

## ProGuard / R8

Release builds must:
- Enable `minifyEnabled = true` + `shrinkResources = true`
- Mapping files uploaded to Play Console for crash de-obfuscation
- No `-keep class *` wildcard rules — only keep what reflection requires

---

## Sensitive Data in Domain Models

- Domain models (value classes, data classes) must **not** include raw passwords or plaintext tokens as fields
- DTOs that map to/from network must only include what the backend contract requires — no "extra" sensitive fields added speculatively
- `@SerialName` fields on DTOs: audit before adding new ones that may expose internal identifiers

---

## CI Security Gates

`build.yml` and `test.yml` should verify:
- APK is not debuggable: `aapt dump badging app-release.apk | grep debuggable` (should return nothing)
- No `http://` in production string resources: `grep -r "http://" composeApp/src/main/res/` (should be empty)

---

## Quick Scan Commands

```bash
# Secrets in source
probe search "password OR api_key OR apiKey OR secret OR token" ./ --max-results 10 --max-tokens 2000

# Cleartext traffic
probe search "http://" ./ --max-results 10 --max-tokens 2000

# Trust-all TLS patterns
probe search "TrustAllCerts OR ALLOW_ALL_HOSTNAME OR checkServerTrusted" ./ --max-results 5 --max-tokens 1000

# Exported components
probe search "android:exported" ./ --max-results 10 --max-tokens 2000

# Unsafe crypto
probe search "DES OR MD5 OR ECB OR Math.random OR new Random" ./ --max-results 10 --max-tokens 2000

# Log leaks
probe search "Log.d OR Log.v OR Log.i" ./ --max-results 10 --max-tokens 2000
```

---

## iOS Security (KMP `actual` implementations)

### Non-Negotiable Rules

| Rule | Why |
|------|-----|
| **Keychain for all tokens** | `UserDefaults` is unencrypted plist — visible in unencrypted backups |
| **`kSecAttrAccessibleWhenUnlockedThisDeviceOnly`** | Blocks iCloud Keychain sync exfiltration of auth tokens |
| **`NSAllowsArbitraryLoads = false`** | ATS exceptions must be documented; never allow for production domains |
| **Universal Links over custom URL schemes** | URL schemes can be hijacked by other apps; Universal Links are HTTPS-verified |
| **No sensitive data in `UIPasteboard.general`** | iOS 14+ allows other apps to read the general pasteboard |

### iOS `actual` SecureStorage

The `SecureStorage` expect/actual bridge must use Keychain on iOS:

```swift
// ✅ Correct — Keychain with ThisDeviceOnly to block iCloud sync
let query: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrAccount as String: key,
    kSecValueData as String: data,
    kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
]

// ❌ Never — UserDefaults for sensitive data
UserDefaults.standard.set(token, forKey: "auth_token")
```

### iOS Checklist (before release)

- [ ] `NSAllowsArbitraryLoads = false` in production `Info.plist`
- [ ] Auth tokens stored via Keychain `actual`, not `UserDefaults`
- [ ] Keychain items use `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`
- [ ] `kSecAttrSynchronizable = false` on auth tokens (no iCloud sync)
- [ ] Password/card fields use `isSecureTextEntry = true`
- [ ] PIE + stack canary + ARC enabled (verified via `otool`)
- [ ] Deep link parameters validated before use (no open redirect)
- [ ] OAuth redirects use Universal Links, not custom schemes

---

## Web / WasmJS Security

### Non-Negotiable Rules

| Rule | Why |
|------|-----|
| **Never `localStorage` for auth tokens** | XSS can read `localStorage`; use `httpOnly` cookies |
| **CSP header on every HTML response** | First line of defense against XSS |
| **`textContent` not `innerHTML` for user content** | `innerHTML` executes script; `textContent` is always safe |
| **CSRF tokens or `SameSite=Strict` on all state-changing requests** | Prevents cross-origin form submissions |
| **No `client_secret` in WasmJS bundle** | Compiled output is public — secrets are extractable |
| **OAuth PKCE, never implicit flow** | Implicit flow puts tokens in URL fragment — visible in logs |

### WasmJS `actual` SecureStorage

The `SecureStorage` web actual must NOT use `localStorage`:

```kotlin
// ✅ Web actual — use sessionStorage (cleared on tab close) or in-memory only
actual object SecureStorage {
    actual fun store(key: String, value: String) {
        // sessionStorage for ephemeral; in-memory Map for access tokens
        // NEVER localStorage for tokens
    }
}

// ❌ Never
actual fun store(key: String, value: String) {
    window.localStorage.setItem(key, value)  // XSS-readable
}
```

### JWT & OAuth Rules (Web)

```kotlin
// ✅ Access token: short-lived, in-memory only
// ✅ Refresh token: httpOnly cookie set by backend
// ❌ Never: JWT in localStorage, implicit OAuth flow
```

- `alg: none` rejected server-side — never trust client-supplied algorithm
- OAuth `state` parameter validated to prevent CSRF
- `redirect_uri` exact match on backend (not prefix)
- `client_secret` only in backend (`lexicon.server`) — never in WasmJS

### Required HTTP Headers (Caddy / CDN)

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'; script-src 'self'; frame-ancestors 'none'
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

### Web Quick Scan Commands

```bash
# localStorage/sessionStorage usage
probe search "localStorage OR sessionStorage OR indexedDB" ./ --max-results 10 --max-tokens 2000

# innerHTML (XSS risk)
probe search "innerHTML OR outerHTML OR dangerouslySetInnerHTML OR document.write" ./ --max-results 10 --max-tokens 2000

# Open redirect patterns
probe search "redirect OR window.location OR location.href" ./ --max-results 10 --max-tokens 2000

# OAuth/JWT patterns
probe search "implicit OR client_secret OR alg.*none OR localStorage.*token" ./ --max-results 10 --max-tokens 2000
```

---

## Full Audit Command

Run `/android-security-scan [platform: android|ios|web|all]` for a complete structured audit across all applicable phases.

---

## Resources

- [OWASP Mobile Security Testing Guide](https://github.com/OWASP/owasp-mstg)
- [Android Security Bulletins](https://source.android.com/security/bulletin/)
- [android-security-awesome](https://github.com/ashishb/android-security-awesome) — curated mobile tools list
- [awesome-web-security](https://github.com/qazbnm456/awesome-web-security) — curated web security resources
- [SEI CERT Android Secure Coding Standard](https://wiki.sei.cmu.edu/confluence/display/android/Android+Secure+Coding+Standard)
- [OWASP Web Security Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)