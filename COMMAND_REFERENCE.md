# COMMAND_REFERENCE.md — Canonical Command Language Reference

## 1. Overview

This reference defines the canonical command grammar recognized by **LocalAgent**'s `CommandNormalizer`. All input surfaces (Console, Movable Overlay, Voice Input, Workflows, Test Harness, and AI Planners) translate user or agent intent into these exact normalized command forms before dispatching through the universal production pipeline.

---

## 2. Recognized Command Grammar

| Category | Input Command Syntax | Canonical ActionType | TargetSelector | Result in Phase 1 Baseline |
|---|---|---|---|---|
| **Navigation** | `back` | `GLOBAL_BACK` | `TargetSelector.None` | `ACCESSIBILITY_UNAVAILABLE` |
| **Navigation** | `home` | `GLOBAL_HOME` | `TargetSelector.None` | `ACCESSIBILITY_UNAVAILABLE` |
| **Navigation** | `recents` | `GLOBAL_RECENTS` | `TargetSelector.None` | `ACCESSIBILITY_UNAVAILABLE` |
| **UI Control** | `click <target>` | `UI_CLICK` | `TargetSelector.ByText(target)` | `ACCESSIBILITY_UNAVAILABLE` |
| **UI Control** | `long click <target>` | `UI_LONG_CLICK` | `TargetSelector.ByText(target)` | `ACCESSIBILITY_UNAVAILABLE` |
| **UI Control** | `scroll down` / `scroll forward` | `UI_SCROLL_FORWARD` | `TargetSelector.None` | `ACCESSIBILITY_UNAVAILABLE` |
| **UI Control** | `scroll up` / `scroll backward` | `UI_SCROLL_BACKWARD` | `TargetSelector.None` | `ACCESSIBILITY_UNAVAILABLE` |
| **Observation** | `observe` / `observe current` / `test observe` | `UI_SCROLL_FORWARD` (intent = OBSERVE) | `TargetSelector.None` | `ACCESSIBILITY_UNAVAILABLE` |
| **Status** | `status` / `action status` | `UI_SCROLL_FORWARD` (intent = STATUS) | `TargetSelector.None` | `NO_EFFECT_EXPECTED` |
| **System** | `launch <appLabel>` | `APP_LAUNCH` | `TargetSelector.None` (params: appLabel) | `DISPATCHED_BUT_NOT_VERIFIED` |

---

## 3. Command Syntax Rejection Rules (`UNKNOWN_COMMAND`)

Input syntax that does not match the canonical grammar is rejected immediately at the `CommandNormalizer` layer as `ResultCode.UNKNOWN_COMMAND` with an explicit reason. Unknown commands **never enter** the `GoalDispatcher` queue, `ActionPolicyEngine`, or execution layers.

### Examples of Rejected Inputs
- `not real cmd` -> `UNKNOWN_COMMAND` ("Unrecognized command syntax")
- `xyz abc 123` -> `UNKNOWN_COMMAND` ("Unrecognized command syntax")
- `click` (missing target) -> `UNKNOWN_COMMAND` ("Click command requires a target parameter")
- `long click` (missing target) -> `UNKNOWN_COMMAND` ("Long click command requires a target parameter")
- `scroll` (missing direction) -> `UNKNOWN_COMMAND` ("Scroll command requires a direction parameter")
- `launch` (missing app label) -> `UNKNOWN_COMMAND` ("Launch command requires an application label parameter")
- `""` / `"   "` (empty string) -> `UNKNOWN_COMMAND` ("Command input cannot be empty")

---

## 4. Distinction Between UNKNOWN_COMMAND and CAPABILITY_UNAVAILABLE

- **`UNKNOWN_COMMAND`**: Syntax is unidentifiable or malformed. The agent does not understand what is being requested.
- **`CAPABILITY_UNAVAILABLE` / `ACCESSIBILITY_UNAVAILABLE`**: Syntax is valid and understood, but the required execution service or capability is not available in the current phase or device state.
