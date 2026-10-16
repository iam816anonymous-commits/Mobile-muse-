# GitHub Actions CI Workflow Setup & Governance

## 1. CI Workflow Overview

The LocalAgent continuous integration pipeline is defined in `.github/workflows/ci.yml`. It validates repository files and legacy archive integrity, compiles active Kotlin code across `:core` and `:app`, runs all automated unit and Robolectric integration tests, and publishes the debug APK artifact (`mobile-muse-debug-apk`).

---

## 2. Triggers & Concurrency

- **Triggers:**
  - `push`: Triggered on every push to any branch (`**`).
  - `pull_request`: Triggered on every pull request targeting any branch (`**`).
  - `workflow_dispatch`: Permits manual workflow triggers from the GitHub Actions UI.
- **Concurrency Group:** `${{ github.workflow }}-${{ github.ref }}` with `cancel-in-progress: true` to prevent redundant builds on rapid pushes.

---

## 3. Workflow Jobs & Validation Tasks

1. **`validate-repository` (Job A):**
   - Validates existence of `PHASE_PLAN.md`, `docs/PHASE_MAP.md`, `gradlew`, and `legacy/FILE_HASHES.sha256`.
   - Runs `sha256sum -c legacy/FILE_HASHES.sha256` to ensure legacy archive integrity.
2. **`build-and-test` (Job B-E):**
   - Environment: `ubuntu-latest` with JDK 17 (Temurin) and Gradle dependency caching.
   - Command 1: `./gradlew test --stacktrace` (Executes all 157 unit/integration tests).
   - Command 2: `./gradlew assembleDebug` (Builds debug APK).
   - Artifact Publication:
     - `test-results`: Uploads test HTML/XML reports.
     - `mobile-muse-debug-apk`: Uploads `app/build/outputs/apk/debug/app-debug.apk`.

---

## 4. Execution Evidence & Known Limitations

- **Local Execution:**
  - `sha256sum -c legacy/FILE_HASHES.sha256` PASSED (67/67 files OK).
  - `./gradlew test --stacktrace` PASSED (157/157 tests OK).
  - `./gradlew assembleDebug` PASSED (`app-debug.apk` built).
- **Physical Device Execution:** Physical touch gesture and ADB hardware testing is explicitly marked `NOT RUN` in headless sandboxed execution environments.
