# Phase 7 Test Cases — Global & UI Action Execution

## Tier A: Core Pure Verification Strategy Unit Tests (`VerificationStrategyTest.kt`)

| Test ID | Test Description | Target Action | Input Structure | Expected Result | Pass/Fail |
|---|---|---|---|---|---|
| `P7-VERIFY-001` | Identical pre/post snapshot yields verification failure or unverified | `UI_CLICK` | Identical pre and post snapshots | `status = EXECUTED_BUT_NOT_VERIFIED`, `resultCode = DISPATCHED_BUT_NOT_VERIFIED` | PASS |
| `P7-VERIFY-002` | Expected target state change yields verification success | `UI_CLICK` | Target node `checked` attribute toggled false -> true | `status = EXECUTED_AND_VERIFIED`, `resultCode = SUCCESS_VERIFIED` | PASS |
| `P7-VERIFY-003` | Unrelated UI change yields UI change verification | `UI_CLICK` | Unrelated sibling text changed | `status = EXECUTED_AND_VERIFIED`, `resultCode = SUCCESS_VERIFIED` | PASS |
| `P7-VERIFY-004` | Expected navigation change yields navigation verification success | `GLOBAL_BACK` | Pre activity `MainActivity`, post activity `DetailActivity` | `status = EXECUTED_AND_VERIFIED`, `resultCode = SUCCESS_VERIFIED` | PASS |
| `P7-VERIFY-005` | No navigation change yields unverified navigation result | `GLOBAL_BACK` | Activity and package unchanged | `status = EXECUTED_BUT_NOT_VERIFIED`, `resultCode = DISPATCHED_BUT_NOT_VERIFIED` | PASS |
| `P7-VERIFY-006` | Target disappeared after action yields verification success | `UI_CLICK` | Target node present in pre, missing in post | `status = EXECUTED_AND_VERIFIED`, `resultCode = SUCCESS_VERIFIED` | PASS |
| `P7-VERIFY-007` | Target identity changed unexpectedly yields verification failure | `UI_CLICK` | Target identity changed from submit to cancel | `status = VERIFICATION_FAILED`, `resultCode = TARGET_STALE` | PASS |
| `P7-VERIFY-008` | Empty or invalid snapshot yields verification failure | `UI_CLICK` | Null pre or post snapshot | `status = VERIFICATION_FAILED`, `resultCode = ACTION_FAILED` | PASS |
| `P7-VERIFY-009` | Successful action with unchanged UI yields `EXECUTED_BUT_NOT_VERIFIED` | `UI_CLICK` | Pre and post snapshot identical | `status = EXECUTED_BUT_NOT_VERIFIED`, `resultCode = DISPATCHED_BUT_NOT_VERIFIED` | PASS |
| `P7-VERIFY-010` | Verification result contains pre/post diff information | `UI_CLICK` | Pre/post attribute change | `diff != null`, `changedNodes.size == 1` | PASS |

---

## Tier B: App Action Execution & Zero-Action Safety Tests (`ActionExecutionTest.kt`)

| Test ID | Test Description | Target Action | Execution State | Expected Result | Pass/Fail |
|---|---|---|---|---|---|
| `P7-ACTION-001` | `GLOBAL_BACK` contract execution | `GLOBAL_BACK` | AccessibilityService bound | `dispatchSuccess = true`, `actionType = GLOBAL_BACK` | PASS |
| `P7-ACTION-002` | `GLOBAL_HOME` contract execution | `GLOBAL_HOME` | AccessibilityService bound | `dispatchSuccess = true`, `actionType = GLOBAL_HOME` | PASS |
| `P7-ACTION-003` | `GLOBAL_RECENTS` contract execution | `GLOBAL_RECENTS` | AccessibilityService bound | `dispatchSuccess = true`, `actionType = GLOBAL_RECENTS` | PASS |
| `P7-ACTION-004` | `UI_CLICK` contract execution | `UI_CLICK` | Resolved live button target | `actionType = UI_CLICK`, `targetNodeId = submit` | PASS |
| `P7-ACTION-005` | `UI_LONG_CLICK` contract execution | `UI_LONG_CLICK` | Missing target node | `status = TARGET_NOT_FOUND` | PASS |
| `P7-ACTION-006` | `UI_TEXT_INPUT` contract execution | `UI_TEXT_INPUT` | Editable input field | `textInputPayload = "Hello Agent"` | PASS |
| `P7-ACTION-007` | `UI_SCROLL_FORWARD` contract execution | `UI_SCROLL_FORWARD` | Scrollable target | `actionType = UI_SCROLL_FORWARD` | PASS |
| `P7-ACTION-008` | `UI_SCROLL_BACKWARD` contract execution | `UI_SCROLL_BACKWARD` | Scrollable target | `actionType = UI_SCROLL_BACKWARD` | PASS |
| `P7-SAFE-001` | Target resolution failure yields zero action dispatch | `UI_CLICK` | Non-existent node ID | `dispatchSuccess = false`, `status = TARGET_NOT_FOUND` | PASS |
| `P7-SAFE-002` | Target stale/re-acquisition failure yields zero action dispatch | `UI_CLICK` | Stale identity string | `dispatchSuccess = false`, `status = TARGET_NOT_FOUND` | PASS |
| `P7-SAFE-003` | Target not actionable yields zero action dispatch | `UI_CLICK` | Non-clickable static TextView | `dispatchSuccess = false`, `status = TARGET_NOT_ACTIONABLE` | PASS |
| `P7-SAFE-004` | Invalid request missing ID & identity yields zero action dispatch | `UI_CLICK` | Target ID & identity null | `dispatchSuccess = false`, `status = TARGET_NOT_FOUND` | PASS |
| `P7-SAFE-005` | Global action dispatch only performed by global action executor | `GLOBAL_BACK` | Submitted to `UiActionExecutor` | `dispatchSuccess = false`, `status = ACTION_NOT_SUPPORTED` | PASS |
