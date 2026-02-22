#!/bin/bash
# Bump version and push commit + tag to trigger CI deployment.
#
# Usage:
#   ./scripts/deploy.sh --hotfix   # patch bump: 1.0.9 -> 1.0.10, commit, tag, push
#   ./scripts/deploy.sh --minor    # minor bump: 1.0.9 -> 1.1.0,  commit, tag, push
#   ./scripts/deploy.sh --major    # major bump: 1.0.9 -> 2.0.0,  commit, tag, push
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BUMP=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --hotfix|-h) BUMP="--hotfix"; shift ;;
    --minor|-m)  BUMP="--minor";  shift ;;
    --major|-M)  BUMP="--major";  shift ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Usage: $0 [--hotfix|--minor|--major]" >&2
      exit 1
      ;;
  esac
done

if [[ -z "${BUMP}" ]]; then
  echo "Error: bump type required." >&2
  echo "Usage: $0 [--hotfix|--minor|--major]" >&2
  exit 1
fi

# Bump version, sync iOS config, commit, and create tag
"${SCRIPT_DIR}/bump-version.sh" ${BUMP}

# Read the new version to derive the tag name
VERSIONING_FILE="${SCRIPT_DIR}/../versioning.properties"
version_name=$(grep -E "^versionName=" "${VERSIONING_FILE}" | tail -n 1 | cut -d= -f2 | tr -d ' \r')
tag_name="v${version_name}"

# Push commit and tag
GIT_ROOT="$(cd "${SCRIPT_DIR}/.." && git rev-parse --show-toplevel 2>/dev/null)"
(cd "${GIT_ROOT}" && git push)
(cd "${GIT_ROOT}" && git push origin "${tag_name}")

echo "Deployed ${tag_name}: commit and tag pushed."
