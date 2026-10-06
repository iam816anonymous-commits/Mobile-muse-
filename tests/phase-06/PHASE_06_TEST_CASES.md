# Phase 6 Test Cases — Universal Action Engine Target Resolution

## Tier A: Core Pure Unit Tests (`TargetResolverTest.kt`)

| Test ID | Test Description | Target Action | Input Structure | Expected Result | Pass/Fail |
|---|---|---|---|---|---|
| `P6-RESOLVE-001` | Direct target resolution | `CLICK` | Single clickable `Button` | `status = RESOLVED`, `strategy = SELF_ACTIONABLE`, `ancestorDepth = 0`, `confidence = EXACT` | PASS |
| `P6-RESOLVE-002` | Exact target identity resolution | `CLICK` | Node matching target identity string | `status = RESOLVED`, `resolvedNodeId = btn_7`, `strategy = SELF_ACTIONABLE` | PASS |
| `P6-RESOLVE-003` | Clickable child to clickable ancestor | `CLICK` | `TextView "7"` (non-clickable) inside `MaterialButton` (clickable) | `status = RESOLVED`, `resolvedNodeId = btn_7`, `strategy = CLICKABLE_ANCESTOR`, `ancestorDepth = 1`, `confidence >= HIGH` | PASS |
| `P6-RESOLVE-004` | Long-clickable child to long-clickable ancestor | `LONG_CLICK` | Non-long-clickable `View` inside long-clickable `CardView` | `status = RESOLVED`, `resolvedNodeId = parent_card`, `strategy = LONG_CLICKABLE_ANCESTOR` | PASS |
| `P6-RESOLVE-005` | Scrollable child to scrollable ancestor | `SCROLL` | Non-scrollable `TextView` inside `RecyclerView` (scrollable) | `status = RESOLVED`, `resolvedNodeId = recycler_view`, `strategy = SCROLLABLE_ANCESTOR` | PASS |
| `P6-RESOLVE-006` | Editable child to editable ancestor | `EDITABLE` | `EditText` (editable) | `status = RESOLVED`, `resolvedNodeId = input_field`, `strategy = EDITABLE_SELF` | PASS |
| `P6-RESOLVE-007` | Ancestor traversal respects maximum depth | `CLICK` | Hierarchy exceeding maxDepth = 1 | `status = NO_ACTIONABLE_TARGET`, `strategy = NO_TARGET` | PASS |
| `P6-RESOLVE-008` | No matching actionable ancestor returns failure | `CLICK` | Non-clickable `TextView` inside non-clickable `LinearLayout` | `status = NO_ACTIONABLE_TARGET`, `strategy = NO_TARGET`, `resolvedNodeId = null` | PASS |
| `P6-RESOLVE-009` | Capability mismatch target rejected | `SCROLL` / `EDITABLE` | Clickable-only `Button` | `status = NO_ACTIONABLE_TARGET` | PASS |
| `P6-RESOLVE-010` | Correct resolution strategy and result metadata returned | `CLICK` | Clickable `Button` | `status = RESOLVED`, `reacquired = false`, JSON serializable | PASS |

---

## Tier B: App Live Target Re-acquisition & Lifecycle Tests (`LiveTargetResolverTest.kt`, `TargetResolutionLoggerTest.kt`, `ExternalObservationLifecycleTest.kt`)

| Test ID | Test Description | Target Action | Live State | Expected Result | Pass/Fail |
|---|---|---|---|---|---|
| `P6-LIVE-001` | Live target can be re-acquired from current accessibility hierarchy | `CLICK` | Active matching live `AccessibilityNodeInfo` | `reacquired = true`, `liveNode != null`, `isClickable = true` | PASS |
| `P6-LIVE-002` | Re-acquired node matches intended target identity | `CLICK` | Matching viewIdResourceName & text | `reacquired = true`, `resolvedNodeIdentity = id:digit_7_text:7` | PASS |
| `P6-LIVE-003` | Re-acquired node is capability checked again | `CLICK` | Live view matches identity but `isClickable = false` | `reacquired = false`, `liveNode = null`, `failureReason` notes non-actionable | PASS |
| `P6-LIVE-004` | Stale or missing target is rejected safely | `CLICK` | Live UI modified (different button "8") | `reacquired = false`, `liveNode = null`, `failureReason != null` | PASS |
| `P6-LIVE-005` | Re-acquisition correctly handles actionable ancestor relationships | `CLICK` | Non-matching root view | `reacquired = false`, `liveNode = null` | PASS |
| `P6-LIVE-006` | Resolution and re-acquisition are READ-ONLY | `CLICK` | Active clickable live node | `reacquired = true`, `liveNode != null`, `shadowNode.performedActions.size == 0` | PASS |
| `P6-LOG-001` | Diagnostic logger passes valid eventId & deduplicates | `CLICK` | Diagnostic target resolution result | `eventId` non-blank, single event logged per deduplication key | PASS |
| `P6-OBS-LIFE-001` | Start external observation and process event creates snapshot | `OBSERVE` | Active external app (`com.android.calculator2`) | `lastExternalPackageName = com.android.calculator2` | PASS |
| `P6-OBS-LIFE-002` | Stop observation rejects subsequent events | `OBSERVE` | Session `STOPPED`, event emitted | Last snapshot preserved, zero new snapshot published | PASS |
| `P6-OBS-LIFE-003` | Stop observation ignores multiple subsequent events | `OBSERVE` | Session `STOPPED`, 5 events emitted | `lastExternalObservationSnapshot = null`, 0 events processed | PASS |
| `P6-OBS-LIFE-004` | Start after Stop resumes normal event processing | `OBSERVE` | Session `STOPPED` -> `OBSERVING` | New events processed normally | PASS |
| `P6-OBS-LIFE-005` | Stopped session cannot publish snapshot from in-flight event | `OBSERVE` | Event in-flight when session transitions to `STOPPED` | Evaluation aborted, 0 snapshot published | PASS |
| `P6-OBS-LIFE-006` | AccessibilityService remains bound after Stop Observation | `OBSERVE` | Service bound, session `STOPPED` | `isBound = true`, session `STOPPED` | PASS |
