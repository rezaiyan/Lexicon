---
name: bump-version
description: Bump the app version (hotfix/minor/major), commit versioning.properties, and confirm the new version
argument-hint: "hotfix|minor|major"
user-invocable: true
allowed-tools: ["Bash", "Read"]
---

Bump the Lexicon app version using the provided argument (default: hotfix).

## Steps

1. Run `./scripts/bump-version.sh --$ARGUMENTS` (use `--hotfix` if no argument given)
2. Read `versioning.properties` to confirm the new version number and build code
3. Show a summary: "Bumped to vX.Y.Z (build N)"
4. Remind the user to run `git add versioning.properties && git commit` if they want to commit the bump

Do NOT commit or push automatically — just run the script and report the result.
