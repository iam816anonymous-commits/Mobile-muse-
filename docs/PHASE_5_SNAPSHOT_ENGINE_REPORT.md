# Phase 5 Snapshot Engine Report: Observation & Snapshot Architecture (Phase 5 Correction)

## 1. Executive Summary
This report details the architectural design, single-root extraction pipeline, node identity system, `SnapshotDiffEngine`, explicit STOP and CLEAR observation controls, and duplicate node investigation completed for **Phase 5 — Observation & Snapshot Engine**.

Phase 5 builds incrementally on top of the Phase 4 `AgentAccessibilityService` foundation, establishing a rigorous, deterministic, memory-safe, and read-only observation pipeline.

---

## 2. Duplicate Node Investigation & Architecture Verification

### 2.1 Investigation Findings
1. **Extraction Path:** `ObservationSnapshotExtractor.traverse()` performs a strict depth-first recursive tree traversal where each child node is fetched once via `nodeInfo.getChild(i)`, converted to a domain `ObservationNode`, and appended to `childrenList`.
2. **UI Renderer Path:** `renderNode()` recursively visits `node.children` exactly once per domain node.
3. **Apparent Duplication Context (Scenario D):** When observing LocalAgent's own UI (`CurrentObservationActivity`), the accessibility framework reports all active views in LocalAgent's window (e.g. TextViews, ScrollViews, Buttons). When LocalAgent captures its own window, the observation UI's own layout components naturally appear in the snapshot tree. This is legitimate accessibility hierarchy behavior (Scenario D/Current Observation contract) rather than duplicate traversal or double-rendering.
4. **External App Isolation (Scenario E/F):** `AgentAccessibilityService.selectBestExternalWindow()` selects candidates with valid external package names (`isValidExternalApplicationPackage`), strictly excluding LocalAgent (`com.localagent.app`) and system UI candidates. Thus, LocalAgent UI nodes are never merged into external application snapshots.

---

## 3. UI Cleanup & Clear Controls

1. **Permission Center Navigation Cleanup:** Removed direct `btnOpenPermissionCenter` buttons from `CurrentObservationActivity`, `ExternalObservationActivity`, and `EvidenceActivity`. Direct navigation to Permission Center is centralized via `MainActivity`.
2. **Clear Control Implementation:** Added `btnClearCurrentObservation` and `btnClearExternalObservation` buttons. Tapping Clear resets screen presentation to empty states ("No current/external observation captured.") without unbinding `AgentAccessibilityService` or modifying DB logs.
3. **Bounds Rendering:** Node tree renderers display bounds `bounds:[left,top,right,bottom]` and size `(WxH)` for every rendered node in `CurrentObservationActivity` and `ExternalObservationActivity`.

---

## 4. Single-Root Extraction Contract & Read-Only Guarantee

- **Single Root Contract:** Every snapshot cycle retrieves exactly one root hierarchy and recycles all acquired `AccessibilityNodeInfo` objects immediately in `finally` blocks.
- **Read-Only Guarantee:** Action execution queue size = 0. Zero dispatches performed.
- **Heap Footprint Target:** Bounded snapshots and immediate node recycling maintain low-RAM compliance on API 27 baseline.

```text
ENGINE STATUS: PASS
SINGLE ROOT CONTRACT: VERIFIED
NODE RECYCLING: VERIFIED
READ-ONLY GUARANTEE: VERIFIED (0 DISPATCHES)
UI CLEANUP & CLEAR CONTROLS: VERIFIED
```
