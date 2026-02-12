#!/bin/bash
# Bump app version (versioning.properties + iOS Config.xcconfig) and optionally create a git tag.
#
# Usage:
#   ./bump-version.sh                    # Sync only (no bump, no tag)
#   ./bump-version.sh --hotfix            # Bump patch: 1.0.9 -> 1.0.10, sync, commit, tag
#   ./bump-version.sh --minor             # Bump minor: 1.0.9 -> 1.1.0, sync, commit, tag
#   ./bump-version.sh --major             # Bump major: 1.0.9 -> 2.0.0, sync, commit, tag
#   ./bump-version.sh --hotfix --no-commit # Bump and sync only; you commit and tag manually
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
VERSIONING_FILE="${PROJECT_ROOT}/versioning.properties"
CONFIG_FILE="${PROJECT_ROOT}/iosApp/Configuration/Config.xcconfig"

BUMP=""
NO_COMMIT=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --hotfix|-h)
      BUMP=hotfix
      shift
      ;;
    --minor|-m)
      BUMP=minor
      shift
      ;;
    --major|-M)
      BUMP=major
      shift
      ;;
    --no-commit)
      NO_COMMIT=true
      shift
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Usage: $0 [--hotfix|--minor|--major] [--no-commit]" >&2
      exit 1
      ;;
  esac
done

if [[ ! -f "${VERSIONING_FILE}" ]]; then
  echo "Missing versioning.properties at ${VERSIONING_FILE}" >&2
  exit 1
fi
if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "Missing Config.xcconfig at ${CONFIG_FILE}" >&2
  exit 1
fi

get_prop() {
  local key="$1"
  local line
  line=$(grep -E "^${key}=" "${VERSIONING_FILE}" | tail -n 1) || return 1
  echo "${line#*=}" | sed 's/\r$//' | tr -d ' '
}

set_prop() {
  local key="$1"
  local value="$2"
  if sed --version 2>/dev/null | grep -q GNU; then
    sed -i "s/^${key}=.*/${key}=${value}/" "${VERSIONING_FILE}"
  else
    sed -i '' "s/^${key}=.*/${key}=${value}/" "${VERSIONING_FILE}"
  fi
}

# Parse versionName (major.minor.patch) into parts
parse_semver() {
  local v="$1"
  if [[ ! "$v" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "Invalid versionName (expected major.minor.patch): ${v}" >&2
    exit 1
  fi
  echo "${BASH_REMATCH[1]} ${BASH_REMATCH[2]} ${BASH_REMATCH[3]}"
}

version_name="$(get_prop "versionName")" || { echo "Missing versionName in versioning.properties" >&2; exit 1; }
version_code="$(get_prop "versionCode")" || { echo "Missing versionCode in versioning.properties" >&2; exit 1; }

if [[ -n "${BUMP}" ]]; then
  read -r major minor patch <<< "$(parse_semver "${version_name}")"
  case "${BUMP}" in
    hotfix)
      patch=$((patch + 1))
      ;;
    minor)
      minor=$((minor + 1))
      patch=0
      ;;
    major)
      major=$((major + 1))
      minor=0
      patch=0
      ;;
  esac
  version_name="${major}.${minor}.${patch}"
  version_code=$((version_code + 1))
  set_prop "versionName" "${version_name}"
  set_prop "versionCode" "${version_code}"
  echo "Bumped to versionName=${version_name} versionCode=${version_code}"
fi

# Update Config.xcconfig in place (macOS and GNU sed compatible)
if sed --version 2>/dev/null | grep -q GNU; then
  sed -i "s/^MARKETING_VERSION = .*/MARKETING_VERSION = ${version_name}/" "${CONFIG_FILE}"
  sed -i "s/^CURRENT_PROJECT_VERSION = .*/CURRENT_PROJECT_VERSION = ${version_code}/" "${CONFIG_FILE}"
else
  sed -i '' "s/^MARKETING_VERSION = .*/MARKETING_VERSION = ${version_name}/" "${CONFIG_FILE}"
  sed -i '' "s/^CURRENT_PROJECT_VERSION = .*/CURRENT_PROJECT_VERSION = ${version_code}/" "${CONFIG_FILE}"
fi

echo "Synced to iOS Config.xcconfig: ${version_name} (${version_code})"

# Git commit and tag (only when we bumped and not --no-commit)
if [[ -n "${BUMP}" && "${NO_COMMIT}" != true ]]; then
  GIT_ROOT="$(cd "${PROJECT_ROOT}" && git rev-parse --show-toplevel 2>/dev/null)" || true
  if [[ -z "${GIT_ROOT}" || ! -d "${GIT_ROOT}/.git" ]]; then
    echo "Not in a git repo; skipping commit and tag." >&2
    exit 0
  fi
  tag_name="v${version_name}"
  if (cd "${GIT_ROOT}" && git rev-parse "${tag_name}" >/dev/null 2>&1); then
    echo "Tag ${tag_name} already exists; skipping commit and tag." >&2
    exit 0
  fi
  # Paths relative to git root for staging
  rel_versioning="$(cd "${GIT_ROOT}" && realpath --relative-to=. "${VERSIONING_FILE}" 2>/dev/null)" || \
    rel_versioning="${VERSIONING_FILE#${GIT_ROOT}/}"
  rel_config="$(cd "${GIT_ROOT}" && realpath --relative-to=. "${CONFIG_FILE}" 2>/dev/null)" || \
    rel_config="${CONFIG_FILE#${GIT_ROOT}/}"
  if [[ "${rel_versioning}" == "${VERSIONING_FILE}" ]]; then
    rel_versioning="VokabApp/versioning.properties"
    rel_config="VokabApp/iosApp/Configuration/Config.xcconfig"
  fi
  (cd "${GIT_ROOT}" && git add "${rel_versioning}" "${rel_config}")
  if (cd "${GIT_ROOT}" && git diff --staged --quiet); then
    echo "No changes to commit (versions already up to date)." >&2
    exit 0
  fi
  (cd "${GIT_ROOT}" && git commit -m "Bump version to ${version_name} (${version_code})")
  (cd "${GIT_ROOT}" && git tag "${tag_name}")
  echo "Committed and created tag ${tag_name}. Push with: git push && git push origin ${tag_name}"
fi

if [[ -n "${BUMP}" && "${NO_COMMIT}" == true ]]; then
  echo "Skipped commit and tag (--no-commit). Commit versioning.properties and Config.xcconfig then: git tag v${version_name}"
fi
