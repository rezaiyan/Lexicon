# iOS Build Troubleshooting Guide

## Common Issues and Solutions

### 1. Missing XCFramework Errors

**Error:**
```
error: There is no XCFramework found at '.../GoogleAppMeasurement.xcframework'
error: There is no XCFramework found at '.../GoogleAdsOnDeviceConversion.xcframework'
```

**Cause:** Swift Package Manager failed to download binary XCFrameworks.

**Solutions (try in order):**

#### Solution A: Clean and Rebuild
```bash
cd iosApp

# Clean all caches
find ~/Library/Developer/Xcode/DerivedData/iosApp-* -delete 2>/dev/null
find ~/Library/Caches/org.swift.swiftpm -delete 2>/dev/null

# Clean project
xcodebuild -scheme iosApp -project ./iosApp.xcodeproj clean

# Resolve packages
xcodebuild -resolvePackageDependencies -scheme iosApp -project ./iosApp.xcodeproj

# Build again
CI=true bundle exec fastlane build
```

#### Solution B: Open in Xcode
Sometimes Xcode's GUI is better at downloading binary frameworks:

```bash
open iosApp.xcodeproj
```

Then in Xcode:
1. Product → Clean Build Folder (Cmd+Shift+K)
2. File → Packages → Reset Package Caches
3. File → Packages → Resolve Package Versions
4. Wait for packages to download (watch the progress in the toolbar)
5. Close Xcode
6. Run `CI=true bundle exec fastlane build`

#### Solution C: Reset All SPM State
```bash
cd iosApp

# Delete all SPM caches
rm -rf ~/Library/Caches/org.swift.swiftpm
rm -rf ~/Library/Developer/Xcode/DerivedData

# Delete project-specific SPM state
rm -rf .build
rm -rf iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm
rm -rf iosApp.xcodeproj/project.xcworkspace/xcuserdata

# Rebuild from scratch
CI=true bundle exec fastlane build
```

#### Solution D: Check Network/Firewall
Binary frameworks are downloaded from GitHub. Ensure:
- You have a stable internet connection
- No VPN/proxy is interfering
- GitHub is accessible: `curl -I https://github.com`

### 2. Xcode Build Settings Timeout

**Error:**
```
Command timed out after 10 seconds on try 1 of 7, trying again...
```

**Cause:** Xcode is slow to respond (common with Xcode 15).

**Solution:** This is normal and handled automatically. Fastlane retries up to 7 times with increasing timeouts. Just wait.

### 3. Git Repository is Dirty

**Error:**
```
Git repository is dirty! Please ensure the repo is in a clean state...
```

**Solution:** Use `CI=true` prefix to bypass the check:
```bash
CI=true bundle exec fastlane build
```

Or commit/stash your changes:
```bash
git stash
bundle exec fastlane build
git stash pop
```

### 4. Certificate/Provisioning Profile Issues

**Error:**
```
No valid code signing identity
No matching provisioning profiles found
```

**Solutions:**

#### Check Certificate
```bash
security find-identity -v -p codesigning | grep "Apple Distribution"
```

Should show: `Apple Distribution: Ali Rezaiyan (VFCFJC7Y5J)`

If missing, download from: https://developer.apple.com/account/resources/certificates/list

#### Check Provisioning Profile
```bash
ls -la ~/Library/MobileDevice/Provisioning\ Profiles/
```

Should show at least one `.mobileprovision` file.

If missing, download from: https://developer.apple.com/account/resources/profiles/list

#### Reinstall Profile
```bash
# Download profile
# Then install it
cp ~/Downloads/Vokab_App_Store.mobileprovision ~/Library/MobileDevice/Provisioning\ Profiles/

# Or double-click the .mobileprovision file to install
open ~/Downloads/Vokab_App_Store.mobileprovision
```

### 5. Framework Not Found (KMP)

**Error:**
```
framework not found 'composeApp'
```

**Cause:** The Kotlin Multiplatform framework wasn't built.

**Solution:**
```bash
# Build the KMP framework first
cd /Users/ali/AndroidStudioProjects/Vokab/VokabApp/Lexicon
./gradlew composeApp:linkReleaseFrameworkIosArm64 --stacktrace

# Then build iOS
cd iosApp
CI=true bundle exec fastlane build
```

### 6. Memory/Disk Space Issues

**Symptoms:**
- Build crashes midway
- "No space left on device" errors
- Slow builds

**Solutions:**

#### Check Disk Space
```bash
df -h ~
```

Need at least 10GB free for builds.

#### Clean Xcode Cache
```bash
# Check cache size
du -sh ~/Library/Developer/Xcode/DerivedData
du -sh ~/Library/Caches/org.swift.swiftpm

# Clean old builds
find ~/Library/Developer/Xcode/DerivedData -type d -mtime +7 -exec rm -rf {} +
```

## Debugging Tips

### View Full Build Log
```bash
# Fastlane log
tail -f /tmp/ios-build.log

# Xcode log
tail -f ~/Library/Logs/gym/Lexicon-iosApp.log

# While building, watch progress
tail -f ~/Library/Logs/gym/Lexicon-iosApp.log | grep -E "(error|warning|Compiling|Linking)"
```

### Check What's Running
```bash
# See if build is active
ps aux | grep -i xcodebuild | grep -v grep

# Kill hung build
killall xcodebuild
```

### Verify Setup Before Building
```bash
# Run the test script which checks everything
./test-ios-distribution.sh
# Choose option 4 to cancel after checks
```

## Best Practices

1. **Always clean first** if you haven't built in a while:
   ```bash
   find ~/Library/Developer/Xcode/DerivedData/iosApp-* -delete 2>/dev/null
   ```

2. **Let Xcode download packages first** if you're having SPM issues:
   ```bash
   open iosApp.xcodeproj
   # Wait for packages to resolve, then close
   ```

3. **Use the automated script** for consistent results:
   ```bash
   ./test-ios-distribution.sh
   ```

4. **Check network before building** - binary frameworks need stable connection

5. **Keep Xcode updated** - some SPM bugs are fixed in newer versions

## Getting Help

If issues persist:

1. Check the full Xcode log: `~/Library/Logs/gym/Lexicon-iosApp.log`
2. Search for the specific error on Stack Overflow
3. Check Fastlane issues: https://github.com/fastlane/fastlane/issues
4. Check Xcode SPM known issues: https://developer.apple.com/forums/tags/swift-packages

## Quick Reference

```bash
# Complete clean and rebuild
cd /Users/ali/AndroidStudioProjects/Vokab/VokabApp/Lexicon
find ~/Library/Developer/Xcode/DerivedData -delete 2>/dev/null
find ~/Library/Caches/org.swift.swiftpm -delete 2>/dev/null
./gradlew composeApp:linkReleaseFrameworkIosArm64
cd iosApp
CI=true bundle exec fastlane build
```
