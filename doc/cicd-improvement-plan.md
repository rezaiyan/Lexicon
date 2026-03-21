# CI/CD Improvement Plan

**Date:** 2026-03-21
**Scope:** `.github/workflows/`, `.github/actions/`, `fastlane/`, `iosApp/fastlane/`
**Status:** Planning

---

## Executive Summary

The current pipeline is solid for a KMP app of this scale — it has parallelism, caching,
version-gating, Maestro UI tests, coverage reporting, and tri-platform deployment. This plan
addresses **18 issues** found during a full audit, grouped by severity. Fixes are ordered
so the most impactful / least risky changes come first.

---

## Context: Track Name Change

The Google Play closed testing track was renamed from `"4.12.2025"` → `"early-access"`.
This is reflected throughout the plan. The `fastlane/Fastfile` `release` lane was promoting
from `"alpha"` which never received any builds — that path was silently broken. Both the
`beta` upload target and the `release` promotion source are corrected in Phase 1.

---

## Issues Index

| # | Issue | Phase | File(s) | Severity |
|---|-------|-------|---------|----------|
| 1 | `upload-artifact@v7` / `download-artifact@v8` don't exist | 1 | `ci.yml`, `release.yml` | Critical |
| 2 | Play Store track mismatch (`early-access` vs `alpha`) | 1 | `fastlane/Fastfile` | Critical |
| 3 | Detekt skipped on PRs | 1 | `ci.yml` | High |
| 4 | PRs skip `wasmJsBrowserTest` — lighter test coverage than `main` | 1 | `ci.yml` | High |
| 5 | APK built twice (`build-android` + `maestro` both run `assembleDebug`) | 2 | `ci.yml` | High |
| 6 | `StrictHostKeyChecking=no` undermines `ssh-keyscan` | 2 | `ci.yml` | High |
| 7 | Hardcoded credentials in `generate-all-secrets-auto.sh` | 2 | `scripts/` | High |
| 8 | iOS KMP cache keyed on `iosArm64` but `build-ios` builds `iosSimulatorArm64` | 2 | `ci.yml` | Medium |
| 9 | Coverage threshold at 5% — never blocks a PR | 2 | `ci.yml` | Medium |
| 10 | No path filtering — full CI on doc/README commits | 2 | `ci.yml` | Medium |
| 11 | `GoogleService-Info.plist` written to disk, never cleaned up | 2 | `ci.yml`, `release.yml` | Medium |
| 12 | Provisioning profile name: `ci.yml` uses secret, `release.yml` uses action output | 2 | both | Medium |
| 13 | GitHub Environments not configured (no deploy gates/audit trail) | 3 | `ci.yml`, `release.yml` | Medium |
| 14 | No failure notifications | 3 | `ci.yml` | Low |
| 15 | `.p8` key written to disk without cleanup | 3 | `ci.yml`, `release.yml`, `release-appstore.yml` | Medium |
| 16 | Deprecated `xcrun altool` in `validate` lane | 3 | `iosApp/fastlane/Fastfile` | Low |
| 17 | Dead `setup_certificates` lane in iOS Fastfile | 3 | `iosApp/fastlane/Fastfile` | Low |
| 18 | macOS runner cost: `test-ios` runs on every PR regardless of changes | 3 | `ci.yml` | Low |

---

## Phase 1 — Critical Fixes (no behaviour change, just correctness)

These are pure bug fixes. Zero risk, do immediately.

### 1.1 — Fix artifact action versions

**Problem:** `actions/upload-artifact@v7` and `actions/download-artifact@v8` do not exist.
The latest versions are `@v4` for both.

**Files:** `ci.yml` lines 228, 238, 369, 497, 588, 609; `release.yml` lines 101, 219, 259, 265

**Change:** Replace all occurrences:
```diff
- uses: actions/upload-artifact@v7
+ uses: actions/upload-artifact@v4

- uses: actions/download-artifact@v8
+ uses: actions/download-artifact@v4
```

---

### 1.2 — Fix Play Store track names in `fastlane/Fastfile`

**Problem:** The `beta` lane uploads to `"4.12.2025"` (now renamed `"early-access"`).
The `release` lane promotes from `"alpha"` — which has never received any builds — so
promoting to production has always been a no-op/error.

**File:** `fastlane/Fastfile`

**Change:**
```diff
  lane :beta do
    build
    upload_to_play_store(
      json_key_data: ENV["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"],
-     track: "4.12.2025",
+     track: ENV["GOOGLE_PLAY_TRACK"] || "early-access",
      ...
    )
  end

  lane :release do
    upload_to_play_store(
      json_key_data: ENV["GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"],
-     track: "alpha",
+     track: ENV["GOOGLE_PLAY_TRACK"] || "early-access",
      track_promote_to: "production",
      ...
    )
  end
```

Add `GOOGLE_PLAY_TRACK: early-access` to the `deploy-android` job env in `ci.yml`
so it is explicit and changeable without touching Ruby code.

---

### 1.3 — Run detekt on PRs

**Problem:** `ci.yml:201` — detekt only runs on `main`. Static analysis should fail PRs
before they merge, not annotate main after the fact.

**File:** `ci.yml`

**Change:**
```diff
- - name: Run detekt
-   if: github.ref == 'refs/heads/main'
-   run: ./gradlew detekt --stacktrace
-   continue-on-error: true
-
- - name: Upload detekt SARIF to GitHub Code Scanning
-   if: github.ref == 'refs/heads/main' && always()
+ - name: Run detekt
+   run: ./gradlew detekt --stacktrace
+   continue-on-error: true
+
+ - name: Upload detekt SARIF to GitHub Code Scanning
+   if: always()
    uses: github/codeql-action/upload-sarif@v4
    with:
      sarif_file: build/reports/detekt/detekt.sarif
      category: detekt
```

---

### 1.4 — Run full test suite on PRs

**Problem:** `ci.yml:212–218` — PRs only run `testDebugUnitTest`. `wasmJsBrowserTest` and
`cleanAllTests` are skipped until after merge. A PR can land and break `main`.

**File:** `ci.yml`

**Change:** Remove the conditional split; always run the full suite:
```diff
- - name: Run unit tests
-   if: github.ref != 'refs/heads/main'
-   run: ./gradlew composeApp:testDebugUnitTest --stacktrace
-
  - name: Run all tests
-   if: github.ref == 'refs/heads/main'
    run: ./gradlew composeApp:cleanAllTests composeApp:testDebugUnitTest composeApp:wasmJsBrowserTest --stacktrace
```

---

## Phase 2 — Performance & Security

### 2.1 — Eliminate duplicate APK build

**Problem:** `build-android` (line 356) and `maestro` (line 415) both run
`./gradlew composeApp:assembleDebug` independently. Two full Android builds per push.

**File:** `ci.yml`

**Change:**

1. Add `needs: [build-android]` to the `maestro` job (it already needs `test`).
2. Replace the build step in `maestro` with a download:

```diff
  maestro:
    name: Maestro UI Tests
    runs-on: ubuntu-latest
-   needs: [test, check-version]
+   needs: [test, build-android, check-version]
    ...
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      # Remove: Cache Git LFS, Pull LFS, Initialize configuration, Set up JDK, Setup Gradle, Build debug APK

+     - name: Download APK
+       uses: actions/download-artifact@v4
+       with:
+         name: debug-apk
+         path: composeApp/build/outputs/apk/debug/

      - name: Install Maestro CLI
        ...
```

This means the APK tested by Maestro is the exact same binary uploaded as the `debug-apk`
artifact — no divergence risk.

---

### 2.2 — Harden SSH deployment

**Problem:** `ci.yml:643` — `StrictHostKeyChecking=no` means the `ssh-keyscan` step
is bypassed entirely. A failed keyscan is silently ignored.

**File:** `ci.yml`

**Change:**
```diff
  - name: Setup SSH for deployment
    run: |
      ...
-     ssh-keyscan -p "$VPS_SSH_PORT" -T 30 -H "$VPS_HOST" >> ~/.ssh/known_hosts 2>/dev/null || \
-       ssh-keyscan -p "$VPS_SSH_PORT" -T 30 "$VPS_HOST" >> ~/.ssh/known_hosts 2>/dev/null || \
-       echo "Warning: ssh-keyscan failed — host key will not be pre-verified"
+     ssh-keyscan -p "$VPS_SSH_PORT" -T 30 -H "$VPS_HOST" >> ~/.ssh/known_hosts 2>&1 || {
+       echo "::error::ssh-keyscan failed for $VPS_HOST — aborting to prevent MITM"
+       exit 1
+     }

  - name: Deploy to VPS
    run: |
-     SSH_OPTS="-i ~/.ssh/deploy_key -o StrictHostKeyChecking=no -o ConnectTimeout=30 -p ${VPS_SSH_PORT}"
+     SSH_OPTS="-i ~/.ssh/deploy_key -o StrictHostKeyChecking=yes -o ConnectTimeout=30 -p ${VPS_SSH_PORT}"
```

---

### 2.3 — Remove hardcoded credentials from scripts

**Problem:** `scripts/generate-all-secrets-auto.sh` and `scripts/generate-all-secrets.sh`
contain real Apple ID, Team ID, API Key ID, and Issuer ID hardcoded in source control.

**Fix options (choose one):**
- **Option A (preferred):** Delete both scripts. The values are already in GitHub Secrets
  and in the developer's `local.properties`. The scripts served a one-time setup purpose.
- **Option B:** Replace hardcoded values with prompts:
  ```bash
  read -p "Apple ID email: " APPLE_ID
  read -p "Team ID: " TEAM_ID
  ```

Add a `.gitignore` entry for any generated `GITHUB_SECRETS.txt` files:
```
GITHUB_SECRETS.txt
ios-deployment-files/
```

---

### 2.4 — Fix iOS KMP framework cache

**Problem:** `deploy-ios` caches/restores `composeApp/build/bin/iosArm64` (arm64),
but `build-ios` builds `iosSimulatorArm64`. The cache is always a cold miss in `deploy-ios`.

**File:** `ci.yml`

Two approaches:

**Option A — Remove the misleading cache** (simplest):
```diff
- - name: Cache KMP Framework
-   uses: actions/cache@v4
-   with:
-     path: |
-       composeApp/build/bin/iosArm64
-       composeApp/build/classes/kotlin/iosArm64
-     key: kmp-ios-release-${{ runner.os }}-${{ hashFiles('**/*.kt', 'gradle/libs.versions.toml') }}
-     restore-keys: kmp-ios-release-${{ runner.os }}-
```

**Option B — Upload arm64 framework as artifact from build-ios** (better long-term):
- Change `build-ios` to also build `linkReleaseFrameworkIosArm64`
- Upload `composeApp/build/bin/iosArm64` as an artifact
- In `deploy-ios`, download the artifact instead of rebuilding
- Add `needs: [build-ios]` already exists so the dependency is already correct

---

### 2.5 — Raise coverage threshold

**Problem:** `ci.yml:249` — `min-coverage-overall: 5` will never block a PR.

**File:** `ci.yml`

**Change:** Set a realistic enforced floor. Check current actual coverage first:
```bash
./gradlew koverHtmlReport && open build/reports/kover/html/index.html
```

Then set the threshold to current coverage − 5% as a ratchet. A reasonable starting
target for an active KMP codebase is **40%**, rising to 70%+ over time:

```diff
  - name: Add coverage summary to PR
    uses: mi-kas/kover-report@v1
    with:
      path: build/reports/kover/report.xml
      title: Code Coverage
      update-comment: true
-     min-coverage-overall: 5
+     min-coverage-overall: 40
```

Also update `koverVerify` thresholds in `build.gradle.kts` to match.

---

### 2.6 — Add path filtering

**Problem:** Every push — including README and doc changes — triggers macOS runners and
the full 45-minute Maestro suite.

**File:** `ci.yml`

**Change:** Add `paths-ignore` to the push trigger:
```yaml
on:
  push:
    branches: [main]
    paths-ignore:
      - '**.md'
      - 'doc/**'
      - '.github/ISSUE_TEMPLATE/**'
      - '.github/PULL_REQUEST_TEMPLATE.md'
  pull_request:
    branches: [main]
    paths-ignore:
      - '**.md'
      - 'doc/**'
      - '.github/ISSUE_TEMPLATE/**'
      - '.github/PULL_REQUEST_TEMPLATE.md'
```

---

### 2.7 — Clean up `GoogleService-Info.plist` after build

**Problem:** `ci.yml:791` and `release.yml:171` — the plist is decoded to disk but never
removed. On shared/self-hosted runners this leaks Firebase credentials between runs.

**File:** `ci.yml`, `release.yml`, `release-appstore.yml`

Add a cleanup step after the Fastlane step in each iOS deploy job:
```yaml
- name: Clean up sensitive files
  if: always()
  run: |
    rm -f iosApp/iosApp/GoogleService-Info.plist
    rm -f ~/.appstoreconnect/private_keys/AuthKey_*.p8
```

This also handles the `.p8` key cleanup (Issue #15).

---

### 2.8 — Unify provisioning profile name source

**Problem:** `ci.yml` deploy-ios uses `${{ secrets.IOS_PROVISIONING_PROFILE_NAME }}`
(a separate secret), while `release.yml` correctly uses `${{ steps.ios-certs.outputs.profile_name }}`
from the composite action's output. They should be identical.

**File:** `ci.yml` deploy-ios job

**Change:** Add `id: ios-certs` to the cert setup step and use the output:
```diff
  - name: Set up certificates and provisioning profiles
+   id: ios-certs
    uses: ./.github/actions/ios-cert-setup
    with:
      certificate_base64: ${{ secrets.IOS_CERTIFICATES_P12_BASE64 }}
      certificate_password: ${{ secrets.IOS_CERTIFICATES_PASSWORD }}
      provisioning_profile_base64: ${{ secrets.IOS_PROVISIONING_PROFILE_BASE64 }}

  ...

-     IOS_PROVISIONING_PROFILE_NAME: ${{ secrets.IOS_PROVISIONING_PROFILE_NAME }}
+     IOS_PROVISIONING_PROFILE_NAME: ${{ steps.ios-certs.outputs.profile_name }}
```

This removes the need for the `IOS_PROVISIONING_PROFILE_NAME` secret — the value is
extracted directly from the provisioning profile by the composite action.

---

## Phase 3 — Enterprise Hardening

### 3.1 — Configure GitHub Environments

**Problem:** Deploy jobs run without approval gates, environment-scoped secrets, or
a deployment audit trail in the GitHub UI.

**Steps:**
1. In GitHub repo → Settings → Environments, create:
   - `staging` — for TestFlight and Google Play `early-access`
   - `production` — for App Store and Play Store production (with required reviewers)
2. Add `environment: staging` to `deploy-android`, `deploy-ios`, `deploy-web`
3. Add `environment: production` to any future production promotion jobs
4. Move deployment-specific secrets (keystore, Apple certs) to environment-scoped secrets

```yaml
  deploy-android:
    name: Deploy Android to Closed Testing
+   environment: staging
    runs-on: ubuntu-latest
    needs: [build-android, maestro, check-version]
```

---

### 3.2 — Add failure notifications

**Problem:** Silent failures on deploy jobs. No one knows a production deploy broke
unless they happen to check Actions.

**File:** `ci.yml` — add to each deploy job's `steps`:

```yaml
- name: Notify on failure
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": ":x: *${{ github.workflow }}* failed on `${{ github.ref_name }}`",
        "attachments": [{
          "color": "danger",
          "fields": [
            { "title": "Job", "value": "${{ github.job }}", "short": true },
            { "title": "Run", "value": "<${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}|View>", "short": true }
          ]
        }]
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
    SLACK_WEBHOOK_TYPE: INCOMING_WEBHOOK
```

Alternatively use GitHub's built-in email notifications if Slack is not available.

---

### 3.3 — Remove deprecated `xcrun altool` from iOS Fastfile

**Problem:** `iosApp/fastlane/Fastfile:131` — `altool --validate-app` was deprecated
in Xcode 14. Fastlane's `upload_to_testflight` handles validation natively via the
App Store Connect API.

**File:** `iosApp/fastlane/Fastfile`

**Change:** Replace the `validate` lane body:
```diff
  lane :validate do
-   UI.message("🔍 Validating IPA...")
-   ipa_path = File.expand_path("../build/Lexicon.ipa", __dir__)
-   unless File.exist?(ipa_path)
-     UI.user_error!("❌ IPA not found at #{ipa_path}. Please run 'fastlane build' first.")
-   end
-   if @api_key
-     UI.message("Validating with App Store Connect API key...")
-     sh("xcrun altool --validate-app -f \"#{ipa_path}\" -t ios --apiKey #{@api_key[:key_id]} --apiIssuer #{@api_key[:issuer_id]}")
-   else
-     UI.important("⚠️  No API key configured. Skipping App Store Connect validation.")
-   end
-   UI.success("✓ IPA validation successful!")
+   ipa_path = lane_context[SharedValues::IPA_OUTPUT_PATH]
+   UI.user_error!("No IPA found. Run 'fastlane build' first.") unless ipa_path && File.exist?(ipa_path)
+   # Validation is handled natively by upload_to_testflight / upload_to_app_store
+   # via the App Store Connect API key configured in before_all
+   UI.success("✓ IPA present at #{ipa_path}")
  end
```

---

### 3.4 — Remove dead `setup_certificates` lane

**Problem:** `iosApp/fastlane/Fastfile:203–227` — the lane is never called from CI.
Certificate setup is handled by the `ios-cert-setup` composite action. The lane
creates false impressions about the signing flow.

**File:** `iosApp/fastlane/Fastfile`

**Change:** Delete the `setup_certificates` lane entirely (lines 203–227).

---

### 3.5 — Limit `test-ios` to Kotlin-affecting changes

**Problem:** `test-ios` runs on every push/PR on an expensive macOS runner, even
for changes that only touch Android, web, or documentation files.

**File:** `ci.yml`

Option: use `dorny/paths-filter` to gate the job:
```yaml
  changes:
    runs-on: ubuntu-latest
    outputs:
      ios: ${{ steps.filter.outputs.ios }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            ios:
              - '**/*.kt'
              - 'platforms/**'
              - 'iosApp/**'
              - 'gradle/libs.versions.toml'

  test-ios:
    needs: [changes]
    if: needs.changes.outputs.ios == 'true' || github.ref == 'refs/heads/main'
    ...
```

---

## Implementation Order

```
Phase 1 (do now — zero risk, all correctness fixes)
  ├── 1.1  artifact action versions        ~5 min
  ├── 1.2  Play Store track names          ~10 min
  ├── 1.3  detekt on PRs                   ~2 min
  └── 1.4  full tests on PRs              ~2 min

Phase 2 (this sprint — performance + security)
  ├── 2.1  eliminate duplicate APK build   ~30 min
  ├── 2.2  harden SSH deployment           ~10 min
  ├── 2.3  remove hardcoded creds          ~15 min
  ├── 2.4  fix iOS KMP cache               ~20 min
  ├── 2.5  raise coverage threshold        ~15 min (measure first)
  ├── 2.6  path filtering                  ~10 min
  ├── 2.7  plist + .p8 cleanup             ~10 min
  └── 2.8  unify profile name source       ~10 min

Phase 3 (next sprint — enterprise hardening)
  ├── 3.1  GitHub Environments             ~30 min
  ├── 3.2  failure notifications           ~20 min
  ├── 3.3  remove xcrun altool             ~10 min
  ├── 3.4  remove dead lane               ~5 min
  └── 3.5  macOS path gating              ~20 min
```

---

## Non-Goals (acknowledged, deferred)

- **OIDC for Google Play** — Workload Identity Federation would replace the long-lived
  service account JSON key. High value but requires GCP IAM changes outside this repo.
- **Fastlane Match** — Centralised cert management via a private git repo or S3.
  Valuable for team scaling but complex; current manual cert setup works fine for now.
- **Automated changelog generation** — TestFlight and Play Store entries currently use
  `"New beta build from CI"`. Integration with conventional commits + `git-cliff` or
  `release-drafter` would generate real changelogs automatically.
- **SBOM generation** — Formal Software Bill of Materials artifact (e.g. CycloneDX)
  beyond the existing GitHub dependency graph submission.
