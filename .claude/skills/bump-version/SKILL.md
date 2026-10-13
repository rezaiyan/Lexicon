---
name: bump-version
description: Bump the app version (hotfix/minor/major), commit versioning.properties, and confirm the new version
argument-hint: "hotfix|minor|major"
user-invocable: true
allowed-tools: ["Bash", "Read"]
---

Bump the Lexicon app version using the provided argument (default: hotfix).

## Steps

1. Run `./scripts/bump-version.sh --$ARGUMENTS` (use `--hotfix` if no argument given).
   On an up-to-date, clean `main` it bumps, commits, tags `vX.Y.Z` and pushes branch + tag in one
   atomic push. That push is the release: the CI run on main sees the new tag has no
   `ci-deployed/<platform>/vX.Y.Z` marker and ships Google Play (early-access), TestFlight and web.
   It refuses on another branch, a dirty tree, or a main behind origin.
2. Read `versioning.properties` to confirm the new version number and build code.
3. Show a summary: "Bumped to vX.Y.Z (build N), CI deploying" with `gh run list --workflow ci.yml -L 1`.

Flags: `--no-push` commits and tags locally only (push later with
`git push --atomic origin HEAD:main vX.Y.Z`); `--no-commit` only edits the version files.
Never push the branch and tag separately: if main lands first, CI skips every deploy.
