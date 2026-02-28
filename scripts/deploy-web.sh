#!/bin/bash
# Deploy the KMP WasmJs web app to the VPS.
#
# Builds locally and rsyncs the production dist to the server.
# The VPS (1 CPU, 3.8GB RAM) is too small for Gradle builds.
#
# Usage:
#   ./scripts/deploy-web.sh                  # Build production + deploy
#   ./scripts/deploy-web.sh --build-only     # Build production without deploying
#   ./scripts/deploy-web.sh --deploy-only    # Deploy existing build (skip Gradle)
#   ./scripts/deploy-web.sh --dev            # Build & deploy development build
#   ./scripts/deploy-web.sh --dry-run        # Show what would be deployed, don't transfer
#   ./scripts/deploy-web.sh --clean          # Clean build before building
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ── Configuration ──────────────────────────────────────────────────────────────
VPS_HOST="root@lexicon.alirezaiyan.com"
VPS_PATH="/var/www/vokab/kmp"

PROD_DIST="${PROJECT_ROOT}/composeApp/build/dist/wasmJs/productionExecutable"
DEV_DIST="${PROJECT_ROOT}/composeApp/build/kotlin-webpack/wasmJs/developmentExecutable"

# ── Defaults ───────────────────────────────────────────────────────────────────
BUILD=true
DEPLOY=true
DEV_MODE=false
DRY_RUN=false
CLEAN=false

# ── Parse flags ────────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-only)
      DEPLOY=false
      shift
      ;;
    --deploy-only)
      BUILD=false
      shift
      ;;
    --dev)
      DEV_MODE=true
      shift
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --clean)
      CLEAN=true
      shift
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --build-only   Build without deploying"
      echo "  --deploy-only  Deploy existing build (skip Gradle)"
      echo "  --dev          Use development build (larger, with source maps)"
      echo "  --dry-run      Show what would be deployed without transferring"
      echo "  --clean        Run Gradle clean before building"
      echo "  --help         Show this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Run '$0 --help' for usage."
      exit 1
      ;;
  esac
done

# ── Resolve dist directory ─────────────────────────────────────────────────────
if [[ "$DEV_MODE" == true ]]; then
  DIST_DIR="$DEV_DIST"
  BUILD_TASK="composeApp:wasmJsBrowserDevelopmentExecutableDistribution"
  echo " Mode: development"
else
  DIST_DIR="$PROD_DIST"
  BUILD_TASK="composeApp:wasmJsBrowserDistribution"
  echo " Mode: production"
fi

# ── Build ──────────────────────────────────────────────────────────────────────
if [[ "$BUILD" == true ]]; then
  echo " Building wasmJs distribution..."

  if [[ "$CLEAN" == true ]]; then
    echo "  Cleaning previous build..."
    "${PROJECT_ROOT}/gradlew" -p "$PROJECT_ROOT" clean
  fi

  "${PROJECT_ROOT}/gradlew" -p "$PROJECT_ROOT" "$BUILD_TASK"

  echo " Build complete"
fi

# ── Verify dist exists ─────────────────────────────────────────────────────────
if [[ ! -d "$DIST_DIR" ]]; then
  echo " Distribution directory not found: $DIST_DIR"
  echo "  Run the build first (without --deploy-only)."
  exit 1
fi

FILE_COUNT=$(find "$DIST_DIR" -type f | wc -l | tr -d ' ')
TOTAL_SIZE=$(du -sh "$DIST_DIR" | cut -f1)
echo " Distribution: ${FILE_COUNT} files, ${TOTAL_SIZE}"

# ── Deploy ─────────────────────────────────────────────────────────────────────
if [[ "$DEPLOY" == true ]]; then
  echo " Deploying to ${VPS_HOST}:${VPS_PATH}..."

  RSYNC_OPTS=(
    -avz
    --delete
    --checksum
    --progress
  )

  if [[ "$DRY_RUN" == true ]]; then
    RSYNC_OPTS+=(--dry-run)
    echo "  (dry run — no files will be transferred)"
  fi

  # Ensure target directory exists
  if [[ "$DRY_RUN" == false ]]; then
    ssh "$VPS_HOST" "mkdir -p ${VPS_PATH}"
  fi

  rsync "${RSYNC_OPTS[@]}" "${DIST_DIR}/" "${VPS_HOST}:${VPS_PATH}/"

  if [[ "$DRY_RUN" == false ]]; then
    echo " Deployed to ${VPS_HOST}:${VPS_PATH}"
    echo "  https://lexicon.alirezaiyan.com/"
  else
    echo " Dry run complete"
  fi
else
  echo " Skipping deploy (--build-only)"
fi
