#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}iOS Distribution Local Testing Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# 1. Check Ruby
if ! command -v ruby &> /dev/null; then
    echo -e "${RED} Ruby not found. Please install Ruby 3.0+${NC}"
    exit 1
fi
echo -e "${GREEN} Ruby $(ruby --version | cut -d' ' -f2) found${NC}"

# 2. Check Bundler
if ! command -v bundle &> /dev/null; then
    echo -e "${RED} Bundler not found. Installing...${NC}"
    gem install bundler
fi
echo -e "${GREEN} Bundler $(bundle --version | cut -d' ' -f3) found${NC}"

# 3. Check .env file
if [ ! -f "iosApp/.env" ]; then
    echo -e "${RED} .env file not found in iosApp directory${NC}"
    echo -e "${YELLOW}A template has been created at iosApp/.env${NC}"
    echo -e "${YELLOW}Please review and update it with your credentials${NC}"
    exit 1
fi
echo -e "${GREEN} .env file found${NC}"

# 4. Check App Store Connect API key
source iosApp/.env
API_KEY_PATH="${APP_STORE_CONNECT_API_KEY_PATH/#\~/$HOME}"
if [ ! -f "$API_KEY_PATH" ]; then
    echo -e "${RED} App Store Connect API key not found at: $API_KEY_PATH${NC}"
    echo -e "${YELLOW}Download it from:${NC}"
    echo -e "${YELLOW}https://appstoreconnect.apple.com/access/integrations/api${NC}"
    echo -e "${YELLOW}Then save it to: $API_KEY_PATH${NC}"

    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$API_KEY_PATH")"
    echo -e "${YELLOW}Created directory: $(dirname "$API_KEY_PATH")${NC}"
    exit 1
fi
echo -e "${GREEN} App Store Connect API key found${NC}"

# 5. Check provisioning profile
PROFILE_DIR="$HOME/Library/MobileDevice/Provisioning Profiles"
if [ ! -d "$PROFILE_DIR" ] || [ -z "$(ls -A "$PROFILE_DIR" 2>/dev/null)" ]; then
    echo -e "${YELLOW}  No provisioning profiles found${NC}"
    echo -e "${YELLOW}Download the App Store provisioning profile from:${NC}"
    echo -e "${YELLOW}https://developer.apple.com/account/resources/profiles/list${NC}"
    echo -e "${YELLOW}Then double-click to install it${NC}"
else
    PROFILE_COUNT=$(ls -1 "$PROFILE_DIR" | wc -l | tr -d ' ')
    echo -e "${GREEN} Found $PROFILE_COUNT provisioning profile(s)${NC}"
fi

# 6. Check code signing identity
if ! security find-identity -v -p codesigning | grep -q "Apple Distribution"; then
    echo -e "${RED} No Apple Distribution certificate found${NC}"
    echo -e "${YELLOW}Download and install it from:${NC}"
    echo -e "${YELLOW}https://developer.apple.com/account/resources/certificates/list${NC}"
    exit 1
fi
echo -e "${GREEN} Apple Distribution certificate found${NC}"

echo ""
echo -e "${GREEN}All prerequisites met!${NC}"
echo ""

# Install dependencies
echo -e "${YELLOW}Installing Ruby dependencies...${NC}"
cd iosApp
bundle install
cd ..
echo -e "${GREEN} Dependencies installed${NC}"
echo ""

# Build KMP framework
echo -e "${YELLOW}Building Kotlin Multiplatform framework...${NC}"
./gradlew composeApp:linkReleaseFrameworkIosArm64 --stacktrace
echo -e "${GREEN} KMP framework built${NC}"
echo ""

# Clean Xcode derived data
echo -e "${YELLOW}Cleaning Xcode derived data...${NC}"
find ~/Library/Developer/Xcode/DerivedData -mindepth 1 -delete 2>/dev/null || true
find ~/Library/Caches/org.swift.swiftpm -mindepth 1 -delete 2>/dev/null || true
echo -e "${GREEN} Xcode cache cleaned${NC}"
echo ""

# Ask user what to do
echo -e "${BLUE}What would you like to do?${NC}"
echo "1) Run pre-flight checks only"
echo "2) Build only (creates .ipa file locally)"
echo "3) Build and validate (checks for errors before upload)"
echo "4) Build and upload to TestFlight"
echo "5) Build and upload to App Store (includes validation)"
echo "6) Cancel"
echo ""
read -p "Enter your choice (1-6): " choice

case $choice in
    1)
        echo -e "${YELLOW}Running pre-flight checks...${NC}"
        cd iosApp
        CI=true bundle exec fastlane preflight
        echo ""
        echo -e "${GREEN} Pre-flight checks complete!${NC}"
        ;;
    2)
        echo -e "${YELLOW}Building iOS app for App Store...${NC}"
        cd iosApp
        CI=true bundle exec fastlane build
        echo ""
        echo -e "${GREEN} Build complete!${NC}"
        echo -e "${GREEN}IPA file location: iosApp/build/Lexicon.ipa${NC}"
        ;;
    3)
        echo -e "${YELLOW}Building and validating iOS app...${NC}"
        cd iosApp
        CI=true bundle exec fastlane build
        CI=true bundle exec fastlane validate
        echo ""
        echo -e "${GREEN} Build and validation complete!${NC}"
        echo -e "${GREEN}IPA file location: iosApp/build/Lexicon.ipa${NC}"
        ;;
    4)
        echo -e "${YELLOW}Building and uploading to TestFlight...${NC}"
        cd iosApp
        CI=true bundle exec fastlane beta
        echo ""
        echo -e "${GREEN} Build uploaded to TestFlight!${NC}"
        echo -e "${YELLOW}Check App Store Connect for processing status${NC}"
        ;;
    5)
        echo -e "${YELLOW}Building and uploading to App Store...${NC}"
        cd iosApp
        CI=true bundle exec fastlane release
        echo ""
        echo -e "${GREEN} Build uploaded to App Store!${NC}"
        echo -e "${YELLOW}Log in to App Store Connect to submit for review${NC}"
        ;;
    6)
        echo -e "${YELLOW}Cancelled${NC}"
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Done!${NC}"
echo -e "${GREEN}========================================${NC}"
