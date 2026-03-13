#!/bin/bash
# Script to export iOS Distribution certificate for CI/CD

set -e

# Auto-detect distribution certificate from keychain
CERT_NAME=$(security find-identity -v -p codesigning 2>/dev/null | grep '"Apple Distribution' | head -1 | sed 's/.*"\(Apple Distribution[^"]*\)".*/\1/' || "")
if [[ -z "$CERT_NAME" ]]; then
    echo " No Apple Distribution certificate found in keychain"
    exit 1
fi
OUTPUT_DIR="$(pwd)/ios-deployment-files"
P12_FILE="$OUTPUT_DIR/certificate.p12"

echo " iOS Certificate Export Helper"
echo "================================"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Check if certificate exists
if ! security find-identity -v -p codesigning | grep -q "$CERT_NAME"; then
    echo " Error: Distribution certificate not found in keychain"
    echo "Expected: $CERT_NAME"
    exit 1
fi

echo " Found certificate in keychain"
echo ""

# Prompt for password
echo "Enter a password for the P12 file (remember this for GitHub secrets):"
read -s P12_PASSWORD
echo ""
echo "Confirm password:"
read -s P12_PASSWORD_CONFIRM
echo ""

if [ "$P12_PASSWORD" != "$P12_PASSWORD_CONFIRM" ]; then
    echo " Passwords don't match"
    exit 1
fi

# Find the certificate and export
echo " Exporting certificate to P12..."

# This will prompt for your Mac login password
security export -k login.keychain -t identities -f pkcs12 \
    -P "$P12_PASSWORD" \
    -o "$P12_FILE" \
    2>/dev/null || {
        echo " Export failed. Try using Keychain Access app instead:"
        echo "   1. Open Keychain Access"
        echo "   2. Find: $CERT_NAME"
        echo "   3. Right-click → Export"
        echo "   4. Save as: $P12_FILE"
        exit 1
    }

if [ -f "$P12_FILE" ]; then
    echo " Certificate exported to: $P12_FILE"
    echo ""

    # Generate base64
    echo " Generating base64 for GitHub secrets..."
    BASE64_FILE="$OUTPUT_DIR/certificate.p12.base64.txt"
    base64 -i "$P12_FILE" > "$BASE64_FILE"

    echo " Base64 saved to: $BASE64_FILE"
    echo ""
    echo " Next steps:"
    echo "   1. Copy certificate password: '$P12_PASSWORD'"
    echo "   2. Copy base64 content: cat $BASE64_FILE | pbcopy"
    echo "   3. Add to GitHub as IOS_CERTIFICATES_P12_BASE64"
    echo "   4. Add password as IOS_CERTIFICATES_PASSWORD"
    echo ""

    # Copy to clipboard if pbcopy available
    if command -v pbcopy &> /dev/null; then
        cat "$BASE64_FILE" | pbcopy
        echo " Base64 copied to clipboard!"
    fi
else
    echo " Export failed"
    exit 1
fi

echo ""
echo " Certificate Password: $P12_PASSWORD"
echo "   (Save this for GitHub secret IOS_CERTIFICATES_PASSWORD)"
