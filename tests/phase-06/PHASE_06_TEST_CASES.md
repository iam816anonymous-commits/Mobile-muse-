# Phase 6 Test Cases — Universal Action Engine Target Resolution

## Tier A: Core Pure Unit Tests (`TargetResolverTest.kt`)

| Test ID | Test Description | Target Action | Input Structure | Expected Result | Pass/Fail |
|---|---|---|---|---|---|
| `P6-RESOLVE-001` | Self clickable node resolves to itself | `CLICK` | Single clickable `Button` | `status = RESOLVED`, `strategy = SELF_ACTIONABLE`, `ancestorDepth = 0`, `confidence = EXACT` | PASS |
| `P6-RESOLVE-002` | Non-clickable child TextView resolves to clickable parent MaterialButton | `CLICK` | `TextView "7"` (non-clickable) inside `MaterialButton` (clickable) | `status = RESOLVED`, `resolvedNodeId = btn_7`, `strategy = CLICKABLE_ANCESTOR`, `ancestorDepth = 1`, `confidence >= HIGH` | PASS |
| `P6-RESOLVE-003` | Nearest valid clickable ancestor wins | `CLICK` | Leaf `TextView` -> Inner `LinearLayout` (clickable) -> Outer `FrameLayout` (clickable) | `status = RESOLVED`, `resolvedNodeId = inner_clickable`, `ancestorDepth = 1` | PASS |
| `P6-RESOLVE-004` | No clickable ancestor returns `NO_TARGET` safely | `CLICK` | Non-clickable `TextView` inside non-clickable `LinearLayout` | `status = NO_ACTIONABLE_TARGET`, `strategy = NO_TARGET`, `resolvedNodeId = null` | PASS |
| `P6-RESOLVE-005` | Long-clickable child & ancestor resolution | `LONG_CLICK` | Non-long-clickable `View` inside long-clickable `CardView` | `status = RESOLVED`, `resolvedNodeId = parent_card`, `strategy = LONG_CLICKABLE_ANCESTOR` | PASS |
| `P6-RESOLVE-006` | Scrollable child resolves to scrollable ancestor | `SCROLL` | Non-scrollable `TextView` inside `RecyclerView` (scrollable) | `status = RESOLVED`, `resolvedNodeId = recycler_view`, `strategy = SCROLLABLE_ANCESTOR` | PASS |
| `P6-RESOLVE-007` | Editable target resolution | `EDITABLE` | `EditText` (editable) | `status = RESOLVED`, `resolvedNodeId = input_field`, `strategy = EDITABLE_SELF` | PASS |
| `P6-RESOLVE-008` | Identity confidence is preserved & derived correctly | `CLICK` | Low-confidence child inside Exact-confidence parent `Button` | `status = RESOLVED`, `confidence = EXACT` | PASS |
| `P6-RESOLVE-009` | Resolution handles missing node ID safely | `CLICK` | Snapshot with non-existent requested node ID | `status = TARGET_NOT_FOUND`, `strategy = NO_TARGET` | PASS |
| `P6-RESOLVE-010` | Resolution never performs action execution | `CLICK` | Clickable `Button` | `status = RESOLVED`, `reacquired = false`, zero framework side effects | PASS |

---

## Tier B: App Live Target Re-acquisition Tests (`LiveTargetResolverTest.kt` & `TargetResolutionLoggerTest.kt`)

| Test ID | Test Description | Target Action | Live State | Expected Result | Pass/Fail |
|---|---|---|---|---|---|
| `P6-LIVE-001` | Snapshot node re-acquires matching live node | `CLICK` | Active matching live `AccessibilityNodeInfo` | `reacquired = true`, `liveNode != null`, `isClickable = true` | PASS |
| `P6-LIVE-002` | Re-acquisition fails safely when target disappears | `CLICK` | Live UI modified (target missing) | `reacquired = false`, `liveNode = null`, `failureReason != null` | PASS |
| `P6-LIVE-003` | Re-acquisition rejects stale or mismatched identity | `CLICK` | Live UI contains stale non-matching view | `reacquired = false`, `liveNode = null` | PASS |
| `P6-LIVE-004` | Re-acquired target capability is verified on live node | `CLICK` | Live view matches identity but `isClickable = false` | `reacquired = false`, `liveNode = null`, `failureReason` notes non-actionable | PASS |
| `P6-LIVE-005` | Acquired `AccessibilityNodeInfo` objects are correctly recycled | `CLICK` | Unmatched live hierarchy traversal | `reacquired = false`, `liveNode = null`, all traversed nodes recycled | PASS |
| `P6-LIVE-006` | Zero action dispatch occurs during resolution and re-acquisition | `CLICK` | Active clickable live node | `reacquired = true`, `liveNode != null`, `shadowNode.performedActions.size == 0` | PASS |
| `P6-LOG-001` | Diagnostic logger passes valid `eventId` & deduplicates | `CLICK` | Diagnostic target resolution result | `eventId` non-blank, single event logged per deduplication key | PASS |
