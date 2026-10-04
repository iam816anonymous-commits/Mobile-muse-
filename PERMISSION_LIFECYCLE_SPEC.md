# PERMISSION_LIFECYCLE_SPEC.md — Permission Lifecycle & State Machine Specification

## 1. Executive Summary

Permissions and special accesses in **LocalAgent** are managed through an explicit, deterministic state machine. The agent never assumes a permission is granted, never forces premature requests for dormant capabilities, and never pretends an action succeeded when its required permission is unavailable.

---

## 2. Capability Permission Lifecycle State Machine

Every capability transitions through explicit states:

```text
  [DECLARED]
      │
      ▼
  [CHECK_AVAILABILITY]
      ├─► Granted ────────────────────────────────────────► [READY]
      └─► Denied / Unbound
            │
            ▼
  [PERMISSION_REQUIRED]
            │
            ▼ (User Clicks Enable / Action Triggered)
  [USER_REQUESTED]
            │
            ├─► User Grants Access ──► [GRANTED] ─────────► [READY]
            └─► User Denies Access ──► [DENIED] ──────────► [BLOCKED]
                                                              │
                                                              ▼
                                                     [PASSIVE_DEGRADATION]
```

---

## 3. Legal Lifecycle States

| Lifecycle State | Description | Pipeline Result Code |
|---|---|---|
| `DECLARED` | Capability registered in `CapabilityRegistry`; permissions unchecked. | `CAPABILITY_UNAVAILABLE` |
| `AVAILABLE` | Platform API available on current Android version (minApi check passed). | `NO_EFFECT_EXPECTED` |
| `UNAVAILABLE` | Capability unsupported on current hardware or API version. | `CAPABILITY_UNAVAILABLE` |
| `PERMISSION_REQUIRED` | Capability supported but requires user runtime grant or special access. | `PERMISSION_REQUIRED` |
| `USER_REQUESTED` | Intent or permission dialog presented to user. | `WAITING` |
| `GRANTED` | User granted required permission or SAF document tree URI. | `READY` |
| `DENIED` | User explicitly denied permission request. | `PERMISSION_REQUIRED` |
| `READY` | Permission active, service bound, and capability fully operational. | `SUCCESS_VERIFIED` |
| `BLOCKED` | Capability blocked by policy engine or revoked access. | `POLICY_BLOCKED` |

---

## 4. Passive Degradation Protocol

When a permission or special access service is denied or unbound:
1. **No Application Crash:** The application remains 100% active.
2. **Explicit Diagnostic Reporting:** The UI displays `PERMISSION_REQUIRED` or `ACCESSIBILITY_UNAVAILABLE`.
3. **Pipeline Rejection:** Command requests targeting blocked capabilities return clear result codes (`ACCESSIBILITY_UNAVAILABLE`, `PERMISSION_REQUIRED`, `HARDWARE_UNAVAILABLE`) rather than generic `UNKNOWN_COMMAND`.
4. **Independent Non-Restricted Subsystems:** Subsystems not requiring the missing permission (e.g. command parsing, offline math solvers, local database queries) continue operating normally.

---

## 5. Storage Access Framework (SAF) URI Persistence Rules

For durable memory storage across Android API versions:
1. On API 29+, SAF URIs returned from `ACTION_OPEN_DOCUMENT_TREE` are persisted using `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)`.
2. Granted URIs are saved to `agent_storage_prefs` SharedPreferences.
3. Upon process restart or app reinstall, `DurableMemoryStorageManager` checks persisted URIs and verifies read/write access.
