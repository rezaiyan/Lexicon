# iOS App Store Release Guide

This guide covers the complete process for releasing Lexicon to the App Store, both locally and via GitHub Actions CI/CD.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Release Process](#local-release-process)
4. [CI/CD Release Process](#cicd-release-process)
5. [Fastlane Lanes](#fastlane-lanes)
6. [Troubleshooting](#troubleshooting)

---

## Overview

The iOS release pipeline has been enhanced with:

- ✅ **Pre-flight checks** - Verify certificates, profiles, and configuration before building
- ✅ **Build validation** - Validate IPA with App Store Connect before upload
- ✅ **Automated TestFlight uploads** - Via `release.yml` workflow
- ✅ **Automated App Store uploads** - Via `release-appstore.yml` workflow
- ✅ **Local testing script** - Interactive script for local builds and releases

---

## Prerequisites

### 1. App Store Connect API Key

**Why?** API keys provide secure, automated authentication without needing your Apple ID password.

**How to get it:**

1. Go to [App Store Connect API Keys](https://appstoreconnect.apple.com/access/integrations/api)
2. Click **Generate API Key** (or use existing one)
3. Download the `.p8` file (e.g., `AuthKey_ABCD1234.p8`)
4. Note the **Key ID** and **Issuer ID**

**Where to store it:**

- **Local**: `~/.appstoreconnect/private_keys/AuthKey_ABCD1234.p8`
- **CI**: As GitHub secret `APP_STORE_CONNECT_API_KEY_CONTENT` (base64 encoded)

### 2. Distribution Certificate

**Type:** Apple Distribution

**How to get it:**

1. Go to [Apple Developer Certificates](https://developer.apple.com/account/resources/certificates/list)
2. Create or download "Apple Distribution" certificate
3. Install by double-clicking the `.cer` file

**Verify installation:**

```bash
security find-identity -v -p codesigning | grep "Apple Distribution"
```

Should show: `Apple Distribution: Ali Rezaiyan (VFCFJC7Y5J)`

### 3. App Store Provisioning Profile

**Type:** App Store Distribution Profile

**How to get it:**

1. Go to [Apple Developer Profiles](https://developer.apple.com/account/resources/profiles/list)
2. Download the App Store provisioning profile for `com.alirezaiyan.vokab`
3. Install by double-clicking the `.mobileprovision` file

**Verify installation:**

```bash
ls -la ~/Library/MobileDevice/Provisioning\ Profiles/
```

Should show at least one `.mobileprovision` file.

### 4. Environment Configuration

For **local testing**, create `iosApp/.env`:

```bash
# App Store Connect API
APP_STORE_CONNECT_API_KEY_ID=ABCD1234
APP_STORE_CONNECT_API_ISSUER_ID=xxxxx-xxxx-xxxx-xxxx-xxxxxxxxx
APP_STORE_CONNECT_API_KEY_PATH=~/.appstoreconnect/private_keys/AuthKey_ABCD1234.p8

# Apple Developer
APPLE_ID=your-apple-id@example.com
APPLE_TEAM_ID=VFCFJC7Y5J
APP_STORE_CONNECT_TEAM_ID=123456789

# App identifiers
IOS_BUNDLE_ID=com.alirezaiyan.vokab
```

For **CI/CD**, these are stored as GitHub Secrets (already configured).

---

## Local Release Process

### Using the Interactive Script (Recommended)

The easiest way to build and release locally:

```bash
./test-ios-distribution.sh
```

**Options:**

1. **Run pre-flight checks only** - Verify everything is set up correctly
2. **Build only** - Creates `.ipa` file without uploading
3. **Build and validate** - Build + validate with App Store Connect
4. **Build and upload to TestFlight** - Full TestFlight release
5. **Build and upload to App Store** - Full App Store release

### Using Fastlane Directly

#### Option 1: Pre-flight checks

Verify your setup before building:

```bash
cd iosApp
bundle exec fastlane preflight
```

This checks:
- ✓ App Store Connect API key
- ✓ Apple Distribution certificate
- ✓ Provisioning profiles
- ✓ Bundle ID and Team ID

#### Option 2: Build only

Build the IPA without uploading:

```bash
cd iosApp
CI=true bundle exec fastlane build
```

Output: `iosApp/build/Lexicon.ipa`

#### Option 3: Validate

Validate an existing IPA:

```bash
cd iosApp
bundle exec fastlane validate
```

This catches issues before upload (wrong bundle ID, expired certs, etc.).

#### Option 4: TestFlight release

Build, validate, and upload to TestFlight:

```bash
cd iosApp
CI=true bundle exec fastlane beta
```

#### Option 5: App Store release

Build, validate, and upload to App Store:

```bash
cd iosApp
CI=true bundle exec fastlane release
```

**Note:** This does **not** auto-submit for review. You'll need to:

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Select Lexicon
3. Fill in "What's New" and metadata
4. Submit for review manually

---

## CI/CD Release Process

### TestFlight Release (Automatic)

**Trigger:** Manual workflow dispatch

**Workflow:** `.github/workflows/release.yml`

**What it does:**

1. Builds Android APK (signed release)
2. Builds iOS framework (KMP)
3. Builds iOS IPA
4. Uploads to TestFlight
5. Creates GitHub Release with artifacts

**How to trigger:**

1. Go to **Actions** tab on GitHub
2. Select **Release** workflow
3. Click **Run workflow**
4. Select branch (usually `main`)
5. Click **Run workflow**

**After it completes:**

- Check [App Store Connect](https://appstoreconnect.apple.com) → TestFlight
- Build will process (10-30 minutes)
- Add to internal/external testers when ready

### App Store Release (Manual)

**Trigger:** Manual workflow dispatch

**Workflow:** `.github/workflows/release-appstore.yml`

**What it does:**

1. Builds iOS framework (KMP)
2. Runs pre-flight checks
3. Builds iOS IPA
4. Validates IPA with App Store Connect
5. Uploads to App Store Connect
6. Creates deployment summary

**How to trigger:**

1. Go to **Actions** tab on GitHub
2. Select **Release to App Store** workflow
3. Click **Run workflow**
4. Select branch (usually `main`)
5. Choose options:
   - Skip validation: `false` (recommended)
6. Click **Run workflow**

**After it completes:**

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Select Lexicon
3. Navigate to the new version
4. Fill in required metadata:
   - What's New
   - Version description
   - Keywords (if changed)
   - Screenshots (if needed)
5. Click **Submit for Review**

---

## Fastlane Lanes

### Core Lanes

| Lane | Description | When to Use |
|------|-------------|-------------|
| `preflight` | Run pre-flight checks | Before any build to verify setup |
| `build` | Build IPA only | When you just need the `.ipa` file |
| `validate` | Validate existing IPA | After building, before uploading |
| `beta` | Build + upload to TestFlight | TestFlight releases |
| `release` | Build + validate + upload to App Store | App Store releases |
| `deploy_appstore` | Alias for `release` | Same as `release` |

### Utility Lanes

| Lane | Description |
|------|-------------|
| `setup_certificates` | Import certificates (CI only) |
| `test` | Run Xcode tests |
| `screenshots` | Capture App Store screenshots |

### Lane Details

#### `preflight`

```bash
bundle exec fastlane preflight
```

Checks:
- ✓ App Store Connect API key configured
- ✓ Apple Distribution certificate exists
- ✓ Provisioning profiles installed
- ✓ Bundle ID set
- ✓ Team ID set

**Exit codes:**
- `0` - All checks passed
- `1` - One or more checks failed

#### `build`

```bash
CI=true bundle exec fastlane build
```

**What it does:**
1. Updates Xcode signing settings to manual
2. Sets team ID and provisioning profile
3. Builds and archives the iOS app
4. Exports as App Store IPA

**Output:** `iosApp/build/Lexicon.ipa`

**Note:** Requires KMP framework to be built first:

```bash
./gradlew composeApp:linkReleaseFrameworkIosArm64
```

#### `validate`

```bash
bundle exec fastlane validate
```

**What it does:**
1. Checks if IPA exists
2. Uploads IPA to App Store Connect for validation
3. Reports any errors (wrong bundle ID, expired certs, etc.)

**Does NOT upload the build** - validation only.

#### `beta`

```bash
CI=true bundle exec fastlane beta
```

**What it does:**
1. Runs `build` lane
2. Uploads to TestFlight
3. Does NOT distribute to testers (manual step)
4. Commits version bump (local only, not in CI)

**After completion:**
- Check App Store Connect → TestFlight
- Wait for build processing (10-30 min)
- Manually add to tester groups

#### `release`

```bash
CI=true bundle exec fastlane release
```

**What it does:**
1. Runs `preflight` checks
2. Runs `build` lane
3. Runs `validate` lane
4. Uploads to App Store Connect
5. Does NOT submit for review

**After completion:**
1. Go to App Store Connect
2. Fill in "What's New" and metadata
3. Submit for review manually

**Why no auto-submit?**
- Allows final metadata review
- Can add release notes tailored for the version
- Can verify screenshots and descriptions
- Safer for production releases

---

## Troubleshooting

### Common Issues

#### 1. "No API key configured"

**Error:**
```
⚠️ No App Store Connect API key configured
```

**Solution:**

Check that you have:

```bash
# Local
export APP_STORE_CONNECT_API_KEY_PATH=~/.appstoreconnect/private_keys/AuthKey_ABCD1234.p8
export APP_STORE_CONNECT_API_KEY_ID=ABCD1234
export APP_STORE_CONNECT_API_ISSUER_ID=xxxxx-xxxx-xxxx-xxxx-xxxxxxxxx

# Or use .env file in iosApp directory
```

#### 2. "No Apple Distribution certificate found"

**Error:**
```
❌ No Apple Distribution certificate found
```

**Solution:**

1. Download certificate from [Apple Developer](https://developer.apple.com/account/resources/certificates/list)
2. Double-click to install
3. Verify: `security find-identity -v -p codesigning | grep "Apple Distribution"`

#### 3. "No provisioning profiles found"

**Error:**
```
❌ No provisioning profiles found
```

**Solution:**

1. Download App Store profile from [Apple Developer](https://developer.apple.com/account/resources/profiles/list)
2. Double-click to install
3. Verify: `ls ~/Library/MobileDevice/Provisioning\ Profiles/`

#### 4. "IPA not found"

**Error:**
```
❌ IPA not found at ./build/Lexicon.ipa
```

**Solution:**

Run `fastlane build` first:

```bash
cd iosApp
CI=true bundle exec fastlane build
```

#### 5. Validation fails with "Invalid bundle"

**Possible causes:**

- Bundle ID mismatch (check `com.alirezaiyan.vokab`)
- Provisioning profile doesn't match bundle ID
- Certificate expired or revoked
- Missing required capabilities

**Solution:**

1. Run pre-flight checks: `bundle exec fastlane preflight`
2. Verify bundle ID in `Config.xcconfig` matches provisioning profile
3. Check certificate expiration: `security find-identity -v -p codesigning`
4. Re-download provisioning profile if needed

#### 6. "Authentication failed"

**Error:**
```
Authentication error with App Store Connect
```

**Solution:**

1. Verify API key file exists and is readable
2. Check API key ID and Issuer ID are correct
3. Ensure API key has "App Manager" or "Admin" role
4. Check if API key is still active in App Store Connect

### Full Troubleshooting Guide

For comprehensive troubleshooting, see:

- [IOS_BUILD_TROUBLESHOOTING.md](./IOS_BUILD_TROUBLESHOOTING.md) - Build and compilation issues
- [LOCAL_IOS_TESTING.md](./LOCAL_IOS_TESTING.md) - Local testing setup

---

## Best Practices

### 1. Always run pre-flight checks first

```bash
cd iosApp
bundle exec fastlane preflight
```

This catches 90% of issues before you waste time building.

### 2. Use the test script for local testing

```bash
./test-ios-distribution.sh
```

It handles all the prerequisites and provides a nice UI.

### 3. Test with TestFlight before App Store

Always do a TestFlight release first:

1. `bundle exec fastlane beta`
2. Test with internal testers
3. Verify everything works
4. Then do `bundle exec fastlane release`

### 4. Keep certificates and profiles up to date

Check expiration:

```bash
# Certificate expiration
security find-identity -v -p codesigning

# Provisioning profile expiration
open ~/Library/MobileDevice/Provisioning\ Profiles/
# (View in Finder, Quick Look shows expiration)
```

Renew before they expire to avoid CI failures.

### 5. Use CI for releases

Manual releases are error-prone. Use GitHub Actions:

- **TestFlight**: `release.yml` workflow
- **App Store**: `release-appstore.yml` workflow

Benefits:
- Consistent environment
- Automated validation
- Artifact storage
- Audit trail

### 6. Never skip validation

Validation catches issues early (before App Store review):

- Wrong bundle ID
- Missing entitlements
- Invalid signatures
- Expired certificates

Always include validation in your release process.

---

## Quick Reference

### Local Release Checklist

- [ ] Run `./test-ios-distribution.sh`
- [ ] Choose option 1 (pre-flight checks)
- [ ] Verify all checks pass
- [ ] Run again and choose option 5 (App Store release)
- [ ] Wait for upload to complete
- [ ] Go to App Store Connect
- [ ] Fill in metadata and submit for review

### CI/CD Release Checklist

- [ ] Verify version in `versioning.properties` is correct
- [ ] Push all changes to `main` branch
- [ ] Go to GitHub Actions
- [ ] Run **Release to App Store** workflow
- [ ] Wait for workflow to complete
- [ ] Check workflow summary for success
- [ ] Go to App Store Connect
- [ ] Fill in metadata and submit for review

### Emergency Rollback

If you need to rollback a release:

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Select Lexicon
3. Go to **App Store** tab
4. Select the problematic version
5. Click **Remove from Review** (if in review)
6. Select previous version
7. Click **Re-submit for Review**

---

## Additional Resources

- [Fastlane Documentation](https://docs.fastlane.tools)
- [App Store Connect Help](https://developer.apple.com/help/app-store-connect/)
- [iOS Distribution Guide](https://developer.apple.com/documentation/xcode/distributing-your-app-for-beta-testing-and-releases)
- [Code Signing Guide](https://developer.apple.com/support/code-signing/)

---

## Version History

- **2026-02-15**: Initial version with automated validation and App Store release workflow
