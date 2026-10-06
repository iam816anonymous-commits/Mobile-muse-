# PHYSICAL_DEVICE_TEST_PLAN.md — Physical Hardware Manual Verification Plan

## 1. Overview
This document specifies physical hardware verification procedures for LocalAgent across frozen phases. Automated unit/Robolectric test suites in CI verify domain logic and simulated framework contracts; physical device verification must be performed manually on physical phone hardware (minSdk 27+) before marking physical test IDs as PASS.

---

## 2. Phase 2 Procedures (`P2-DEV-001`)
- **Procedure:** Install debug APK, trigger log events, clear app data / uninstall app, reinstall debug APK.
- **Verification:** Confirm durable memory storage located under SAF/external storage survives uninstall with intact SHA-256 integrity checksums.
- **Status:** **NOT_RUN**

---

## 3. Phase 3 & 4 Procedures (`P3.3-DEV-PERM-001`, `P4-DEV-001`)
- **Procedure:** Open Permission Center (`PermissionActivity`), request required/optional permissions, toggle Accessibility Service in Android Settings.
- **Verification:** Verify passive degradation events logged to `agent.db` upon unbinding without app crash or unauthorized settings launches.
- **Status:** **NOT_RUN**

---

## 4. Phase 5 Procedures (`P5-DEV-SNAP-001`)
- **Procedure:** Open `CurrentObservationActivity`, trigger active observation, tap `[ Stop Observation ]`, tap `[ Clear ]`.
- **Verification:** Verify single-root retrieval, single node recycling per cycle, clean tree replacement on consecutive observe calls, and zero un-recycled nodes in heap dump.
- **Status:** **NOT_RUN**

---

## 5. Phase 6 Procedures (`P6-DEV-TARGET-001`)
- **Procedure:**
  1. Open Calculator on physical phone.
  2. Open External Observation in LocalAgent.
  3. Capture Calculator UI snapshot.
  4. Select a digit `TextView "7"` (non-clickable).
  5. Inspect target resolution output.
  6. Confirm resolver identifies `MaterialButton` parent as clickable ancestor with strategy `CLICKABLE_ANCESTOR` and confidence `>= HIGH`.
  7. Modify Calculator state (navigate away).
  8. Execute live target re-acquisition.
  9. Confirm stale target is safely rejected (`TARGET_NOT_FOUND`).
  10. Confirm ZERO action execution (clicks/scrolls/typing) occurs during target resolution.
- **Status:** **NOT_RUN**
