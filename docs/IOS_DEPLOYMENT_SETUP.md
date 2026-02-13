# iOS Deployment Setup Guide

Quick start guide for setting up automated iOS deployment.

## 1. Create App Store Connect API Key

**Recommended method** - more secure than using Apple ID password.

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Navigate to: Users and Access → Keys
3. Click "+" to create a new key
4. Name: "GitHub Actions CI/CD"
5. Access: "Admin" or "App Manager"
6. Download the `.p8` file
7. **Save the Key ID and Issuer ID** - you can't retrieve them later

## 2. Export Code Signing Certificate

### Using Xcode:

1. Open Xcode → Preferences → Accounts
2. Select your Apple ID → Select Team
3. Click "Manage Certificates"
4. Right-click your "Apple Distribution" certificate → Export
5. Save as `certificate.p12` with a strong password
6. Remember this password for the secrets

### Using Keychain Access:

1. Open Keychain Access
2. Find "Apple Distribution: Your Name (Team ID)"
3. Right-click → Export
4. Save as `certificate.p12` with a strong password

## 3. Download Provisioning Profile

1. Go to [Apple Developer Portal](https://developer.apple.com/account)
2. Navigate to: Certificates, Identifiers & Profiles → Profiles
3. Create or download your "App Store" profile for your app's Bundle ID
4. Save as `profile.mobileprovision`

## 4. Encode Secrets for GitHub

```bash
# Encode certificate (macOS)
base64 -i certificate.p12 | pbcopy
# Paste this as IOS_CERTIFICATES_P12_BASE64 secret

# Encode provisioning profile (macOS)
base64 -i profile.mobileprovision | pbcopy
# Paste this as IOS_PROVISIONING_PROFILE_BASE64 secret

# Copy API key content
cat AuthKey_XXXXXXXXXX.p8 | pbcopy
# Paste this as APP_STORE_CONNECT_API_KEY_CONTENT secret
```

## 5. Add Secrets to GitHub

Go to your repository: Settings → Secrets and variables → Actions → New repository secret

Add each of these:

| Secret Name | Value | How to Get |
|------------|-------|------------|
| `IOS_CERTIFICATES_P12_BASE64` | Base64-encoded P12 | Step 2 + 4 above |
| `IOS_CERTIFICATES_PASSWORD` | P12 password | From step 2 |
| `IOS_PROVISIONING_PROFILE_BASE64` | Base64-encoded profile | Step 3 + 4 above |
| `APPLE_ID` | your.email@example.com | Your Apple ID |
| `APPLE_APP_SPECIFIC_PASSWORD` | xxxx-xxxx-xxxx-xxxx | [Generate here](https://appleid.apple.com/account/manage) |
| `APPLE_TEAM_ID` | XXXXXXXXXX | [Find here](https://developer.apple.com/account) |
| `APP_STORE_CONNECT_TEAM_ID` | XXXXXXXXXX | Usually same as Team ID |
| `APP_STORE_CONNECT_API_KEY_ID` | Key ID from step 1 | From step 1 |
| `APP_STORE_CONNECT_API_ISSUER_ID` | UUID from step 1 | From step 1 |
| `APP_STORE_CONNECT_API_KEY_CONTENT` | Content of .p8 file | Step 4 above |

## 6. Test Locally (Optional)

```bash
cd iosApp

# Install dependencies
bundle install

# Create .env file with your credentials
cat > .env << EOF
APPLE_ID="your.email@example.com"
APPLE_TEAM_ID="XXXXXXXXXX"
APP_STORE_CONNECT_TEAM_ID="XXXXXXXXXX"
IOS_BUNDLE_ID="com.yourcompany.lexicon"
APP_STORE_CONNECT_API_KEY_ID="XXXXXXXXXX"
APP_STORE_CONNECT_API_ISSUER_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
EOF

# Copy your .p8 file
mkdir -p ~/.appstoreconnect/private_keys
cp AuthKey_XXXXXXXXXX.p8 ~/.appstoreconnect/private_keys/

# Test build (won't upload)
bundle exec fastlane build
```

## 7. Deploy

Push a git tag to trigger deployment:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The GitHub Action will:
- ✅ Build the Android APK
- ✅ Build the iOS IPA
- ✅ Upload iOS to TestFlight
- ✅ Create a GitHub Release

## Maintenance

### When Certificates Expire

Apple Distribution certificates expire after 1 year. When this happens:

1. Create a new certificate in Apple Developer Portal
2. Export as P12
3. Update `IOS_CERTIFICATES_P12_BASE64` and `IOS_CERTIFICATES_PASSWORD` secrets
4. Update provisioning profile if needed

### When Provisioning Profiles Expire

1. Regenerate profile in Apple Developer Portal
2. Download new profile
3. Update `IOS_PROVISIONING_PROFILE_BASE64` secret

## Troubleshooting

**"No valid signing identity found"**
- Certificate or profile is expired or invalid
- Check expiration dates in Apple Developer Portal

**"Authentication failed"**
- App Store Connect API key is invalid
- Check Key ID, Issuer ID, and .p8 content

**"Build failed during framework linking"**
- Kotlin framework compilation failed
- Check Gradle build logs
- Ensure all dependencies are compatible

## Next Steps

- [ ] Set up certificates (steps 1-3)
- [ ] Add secrets to GitHub (step 5)
- [ ] Test locally (step 6)
- [ ] Create and push a tag (step 7)
- [ ] Monitor the deployment in GitHub Actions
- [ ] Check TestFlight for the build

For detailed information, see `iosApp/fastlane/README.md`.
