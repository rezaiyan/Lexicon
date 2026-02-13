# iOS Deployment Checklist

Quick checklist for first-time iOS deployment setup.

## Prerequisites
- [ ] Apple Developer Account (enrolled, $99/year)
- [ ] App created in App Store Connect
- [ ] Bundle ID registered in Apple Developer Portal
- [ ] Xcode installed locally

## One-Time Setup

### 1. Certificates & Profiles
- [ ] Create App Store Connect API Key
  - Go to App Store Connect → Users and Access → Keys
  - Create key with "Admin" or "App Manager" access
  - Download .p8 file and note Key ID and Issuer ID
- [ ] Export iOS Distribution Certificate
  - From Xcode Preferences → Accounts → Manage Certificates
  - Export as .p12 with password
- [ ] Download App Store Provisioning Profile
  - From Apple Developer Portal → Profiles
  - Download "App Store" profile for your Bundle ID

### 2. Encode for GitHub Actions
```bash
# In terminal:
base64 -i certificate.p12 | pbcopy          # Copy to clipboard
base64 -i profile.mobileprovision | pbcopy  # Copy to clipboard
cat AuthKey_XXXXXXXXXX.p8 | pbcopy          # Copy to clipboard
```

### 3. Add GitHub Secrets
Go to: Repository Settings → Secrets and variables → Actions

- [ ] `IOS_CERTIFICATES_P12_BASE64` - Base64 of P12 certificate
- [ ] `IOS_CERTIFICATES_PASSWORD` - Password for P12
- [ ] `IOS_PROVISIONING_PROFILE_BASE64` - Base64 of provisioning profile
- [ ] `APPLE_ID` - Your Apple ID email
- [ ] `APPLE_APP_SPECIFIC_PASSWORD` - App-specific password from appleid.apple.com
- [ ] `APPLE_TEAM_ID` - Your Team ID (10 characters)
- [ ] `APP_STORE_CONNECT_TEAM_ID` - Usually same as APPLE_TEAM_ID
- [ ] `APP_STORE_CONNECT_API_KEY_ID` - Key ID from step 1
- [ ] `APP_STORE_CONNECT_API_ISSUER_ID` - Issuer ID from step 1
- [ ] `APP_STORE_CONNECT_API_KEY_CONTENT` - Content of .p8 file

### 4. Local Setup (Optional)
- [ ] Install Ruby dependencies: `cd iosApp && bundle install`
- [ ] Copy `.env.example` to `.env` and fill in values
- [ ] Copy .p8 file to `~/.appstoreconnect/private_keys/`
- [ ] Test build: `bundle exec fastlane build`

## Deployment Process

### Manual TestFlight Upload
```bash
cd iosApp
bundle exec fastlane beta
```

### Automated via Git Tag
```bash
# Bump version first
./scripts/bump-version.sh --minor

# Create and push tag
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will automatically:
1. Build Android APK
2. Build iOS IPA
3. Upload iOS to TestFlight
4. Create GitHub Release with artifacts

## Post-Deployment

- [ ] Check GitHub Actions workflow status
- [ ] Verify build appears in TestFlight (can take 5-10 minutes)
- [ ] Add beta testers in App Store Connect
- [ ] Distribute build to testers
- [ ] Monitor crash reports and feedback

## Maintenance

### Certificate Renewal (yearly)
- [ ] Generate new certificate in Apple Developer Portal
- [ ] Export as P12
- [ ] Update `IOS_CERTIFICATES_P12_BASE64` secret
- [ ] Regenerate provisioning profile
- [ ] Update `IOS_PROVISIONING_PROFILE_BASE64` secret

### Profile Renewal
- [ ] Regenerate profile in Apple Developer Portal
- [ ] Update `IOS_PROVISIONING_PROFILE_BASE64` secret

## Troubleshooting

**Build fails with signing error:**
- Check certificate hasn't expired
- Verify provisioning profile is valid
- Ensure Bundle ID matches

**TestFlight upload fails:**
- Verify App Store Connect API credentials
- Check API key has correct permissions
- Ensure .p8 file content is correct

**Framework linking error:**
- Check Gradle build succeeded
- Verify KMP framework is compatible
- Clean and rebuild: `./gradlew clean`

## Resources

- [Detailed Setup Guide](../docs/IOS_DEPLOYMENT_SETUP.md)
- [Fastlane Documentation](./fastlane/README.md)
- [Apple Developer Portal](https://developer.apple.com/account)
- [App Store Connect](https://appstoreconnect.apple.com)
