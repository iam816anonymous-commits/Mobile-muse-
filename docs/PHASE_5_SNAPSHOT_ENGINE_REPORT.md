# Phase 5 Snapshot Engine Report: Observation & Snapshot Architecture

## 1. Executive Summary
This report details the architectural design, single-root extraction pipeline, node identity system, `SnapshotDiffEngine`, and explicit STOP observation controls implemented for **Phase 5 — Observation & Snapshot Engine**.

Phase 5 builds incrementally on top of the Phase 4 `AgentAccessibilityService` foundation, establishing a rigorous, deterministic, memory-safe, and read-only observation pipeline.

---

## 2. Architecture & Subsystem Specification

### 2.1 Single-Root Snapshot Retrieval Contract
Every snapshot cycle executes exactly **one** `rootInActiveWindow` or window root retrieval:
```text
Observation Request
       ↓
Verify Accessibility Service is bound
       ↓
ONE rootInActiveWindow retrieval
       ↓
Capture root hierarchy
       ↓
Generate bounded snapshot primitives (MAX_NODES=500, MAX_DEPTH=30)
       ↓
Assign node identity + NodeIdentityConfidence
       ↓
Capture Rect bounds (left, top, right, bottom, width, height)
       ↓
Serialize snapshot JSON
       ↓
Immediate .recycle() on every acquired AccessibilityNodeInfo
       ↓
Return immutable ObservationSnapshot
```

### 2.2 Node Identity & Confidence Classification
Node identity is deterministically computed by `ObservationNode.computeIdentity()` based on structural and semantic attributes available from Accessibility APIs:

| Confidence Level | Criteria | Example Identity |
|---|---|---|
| `EXACT` | Resource ID present + non-blank text or content description | `id:tvTitle_text:Welcome` |
| `HIGH` | Resource ID present without text | `id:imgLogo_idx:0` |
| `MEDIUM` | Class name + non-blank text/description without Resource ID | `cls:Button_lbl:Click Me_parent:parent_0` |
| `LOW` | Structural index / Class name only with parent identity | `cls:LinearLayout_idx:2_parent:root` |
| `EPHEMERAL` | Un-indexed / dynamic synthetic node | `ephemeral_cls:View_idx:0` |

### 2.3 Pure Domain `SnapshotDiffEngine`
The `SnapshotDiffEngine` in `:core` (`com.localagent.core.observation`) compares two immutable snapshots (`before` vs `after`) and categorizes primitive diffs into:
- `ADDED`: Node identity present in `after` but not `before`.
- `REMOVED`: Node identity present in `before` but not `after`.
- `CHANGED`: Node identity present in both, but attributes (`text`, `contentDescription`, `bounds`, `enabled`, `focused`, `checked`) differ.
- `UNCHANGED`: Node identity and all observed attributes are identical.

### 2.4 Observation State Machine & STOP Controls
Observation state is governed by `ObservationEngineState` (`IDLE`, `OBSERVING`, `STOPPING`, `STOPPED`, `UNAVAILABLE`).

Explicit `[ Stop Observation ]` controls on `CurrentObservationActivity` and `ExternalObservationActivity`:
- Stop active observation capture requests.
- Log `OBSERVATION_STOPPED` to `agent.db`.
- Preserve the last captured snapshot (`currentObservationSnapshot` / `lastExternalObservationSnapshot`).
- **Leave Accessibility Service running** (`AgentAccessibilityService.isBound == true`).

---

## 3. Read-Only & Resource Compliance

- **Read-Only Guarantee:** Action execution queue size = 0. Zero dispatches performed.
- **Node Recycling:** Every acquired `AccessibilityNodeInfo` instance is recycled in `finally` blocks during traversal.
- **Heap Footprint Target:** Bounded snapshots and immediate node recycling maintain low-RAM compliance on API 27 baseline.

```text
ENGINE STATUS: PASS
SINGLE ROOT CONTRACT: VERIFIED
NODE RECYCLING: VERIFIED
READ-ONLY GUARANTEE: VERIFIED (0 DISPATCHES)
```
