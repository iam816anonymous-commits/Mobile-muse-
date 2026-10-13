# External Repository Research, Reusable Architecture Extraction, and Integration Assessment

**Document Version:** 1.0.0
**Target Platform:** Android 8.1 / API 27 (Tecno Camon, 4 GB RAM, 64 GB Storage)
**Governing Documents:** `PHASE_PLAN.md`, `docs/PHASE_MAP.md`, `docs/PHASE_SCOPE_GOVERNANCE_RULE.md`

---

## Executive Summary

This research report evaluates six open-source agent and knowledge repositories at the source-code level to identify reusable implementation techniques, algorithms, schemas, workflows, and test patterns for **LocalAgent**.

### Critical Constraint
Direct copying of heavy Python frameworks, Node.js runtimes, browser automation engines (Playwright/Selenium), or external graph databases is **strictly rejected** for on-device Android deployment to preserve native performance, battery efficiency, and memory heap constraints (<35 MB heap on 4 GB RAM hardware). Reusable concepts are selectively adapted into native Kotlin domain models and Room SQLite entities (`agent.db`).

---

## 1. Source-Level Inspection Findings

### Repository A — SAKU
- **URL:** `https://github.com/derkodex-repo-curator/saku`
- **Stack & Architecture:** TypeScript / Node.js agent framework built around procedural skills (`skills/`), task journaling (`journals/`), reflection cycles, and Markdown persistent memory.
- **Licensing:** MIT License.
- **Source Paths Inspected:** `src/skills/`, `src/journal/`, `src/reflection/`, `src/memory/`
- **Key Capabilities & Findings:**
  - Skill format utilizes Markdown files with metadata headers defining triggers, prerequisites, and step execution graphs.
  - Reflection cycle analyzes journal entries (`journal_id`) post-execution to compare expected vs actual outcomes.
- **Android API 27 / 4 GB RAM Feasibility:**
  - *Direct Code Reuse:* **REJECTED** (Node.js runtime requires >150 MB RAM and cannot run natively in Android app APK).
  - *Conceptual Adaptation:* **ADAPTED** into native Kotlin Markdown parser (`SkillPackageParser`) and Room skill entity (`SkillEntity`) for Phase 15 (`Episodic Memory & Learning`).

---

### Repository B — Termux Assistant AI
- **URL:** `https://github.com/CR4CODE/Termux-Assistant-AI`
- **Stack & Architecture:** Python CLI assistant running inside Termux environment, calling `termux-api` shell scripts (`termux-toast`, `termux-vibrate`, `termux-telephony`).
- **Licensing:** GPL-3.0 License.
- **Source Paths Inspected:** `assistant/`, `tools/`, `commands/`
- **Key Capabilities & Findings:**
  - Intent-to-Tool Registry mapping natural language requests to command strings.
  - Command execution boundary validating shell strings against blacklisted dangerous commands.
- **Android API 27 / 4 GB RAM Feasibility:**
  - *Direct Code Reuse:* **REJECTED** (Requires Termux application installation and GPL-3.0 copyleft risk).
  - *Conceptual Adaptation:* **REIMPLEMENTED** in native Kotlin using Android system services (`AudioManager`, `Vibrator`, `CameraManager`) in Phase 12 (`Hardware & System Controls`).

---

### Repository C — Skill Seekers
- **URL:** `https://github.com/yusufkaraaslan/Skill_Seekers`
- **Stack & Architecture:** Python web/repository scraper using `BeautifulSoup4`, `PyPDF2`, `git`, and `requests` to chunk and synthesize web docs into `.skill.json` packages.
- **Licensing:** MIT License.
- **Source Paths Inspected:** `ingestors/`, `extractors/`, `chunker.py`, `provenance.py`
- **Key Capabilities & Findings:**
  - Unified ingestion interface (`BaseIngestor`) with concrete adapters for Web, GitHub, and PDF documents.
  - Content provenance computing SHA-256 hashes over raw text chunks alongside origin URLs and timestamps.
- **Android API 27 / 4 GB RAM Feasibility:**
  - *Direct Code Reuse:* **REJECTED** on-device (PyPDF2 and BS4 scraping consume excessive CPU/RAM during parsing).
  - *Conceptual Adaptation:* **ADAPTED** (Worker Ingestion Pipeline): Heavy ingestion runs on external workstation/cloud worker; compiled SHA-256 verified `.skill.zip` packages are ingested into LocalAgent via Android SAF document picker in Phase 17 (`External Knowledge Ingestion`).

---

### Repository D — GPT Researcher
- **URL:** `https://github.com/assafelovic/gpt-researcher`
- **Stack & Architecture:** Autonomous research agent in Python using `playwright`/`selenium`, vector embeddings (`chromadb`/`langchain`), parallel web search, and Markdown synthesis.
- **Licensing:** Apache-2.0 License.
- **Source Paths Inspected:** `gpt_researcher/planner/`, `gpt_researcher/scrapers/`, `gpt_researcher/context/`
- **Key Capabilities & Findings:**
  - Multi-query research goal decomposition splitting complex goals into 3–5 sub-questions.
  - Claim-to-source citation mapping in synthesized research reports.
- **Android API 27 / 4 GB RAM Feasibility:**
  - *Direct Code Reuse:* **REJECTED** (Playwright browser automation and ChromaDB require >1.5 GB RAM).
  - *Conceptual Adaptation:* **ADAPTED** into Phase 19 (`Trip & General Research Engine`) native Kotlin decomposer using lightweight `OkHttp` and native JSON parsing.

---

### Repository E — Hermes Agent
- **URL:** `https://github.com/NousResearch/hermes-agent`
- **Stack & Architecture:** Agent framework centered on tool execution feedback, versioned skill creation, and execution telemetry.
- **Licensing:** MIT License.
- **Source Paths Inspected:** `hermes/skills/`, `hermes/telemetry/`, `hermes/agent.py`
- **Key Capabilities & Findings:**
  - 5-stage versioned skill lifecycle: `Draft` -> `Candidate` -> `Validated` -> `Promoted` -> `Deprecated`.
  - Controlled skill promotion requiring 3+ consecutive `SUCCESS_VERIFIED` executions.
- **Android API 27 / 4 GB RAM Feasibility:**
  - *Direct Code Reuse:* **ADAPTED** domain models directly into Phase 15 (`Episodic Memory & Learning`) native Kotlin classes (`SkillEntity`, `SkillVersion`, `SkillConfidenceScore`).

---

### Repository F — LLM Wiki Agent
- **URL:** `https://github.com/SamurAIGPT/llm-wiki-agent`
- **Stack & Architecture:** Knowledge management agent constructing interlinked Markdown wiki pages, source ledgers, and contradiction records.
- **Licensing:** MIT License.
- **Source Paths Inspected:** `wiki/ledger.py`, `wiki/pages.py`, `wiki/contradictions.py`
- **Key Capabilities & Findings:**
  - Incremental knowledge page appending without full wiki regeneration.
  - Explicit contradiction ledger tracking conflicting claims across sources (`[CONTRADICTION] Claim A vs Claim B`).
- **Android API 27 / 4 GB RAM Feasibility:**
  - *Direct Code Reuse:* **REIMPLEMENTED** in native Kotlin (`KnowledgeBaseManager` using Room `agent.db` + local `.md` storage). Extremely low RAM footprint (<5 MB).

---

## 2. Feature-to-Project Capability Matrix

| Capability | Best Source Repository | Classification | Android API 27 Suitability | Target LocalAgent Phase |
|---|---|---|---|---|
| **Android UI Control** | LocalAgent Baseline | **LOCAL AGENT CORE** | Excellent (<15 MB RAM) | Phase 6 & 7 |
| **Tool Registry & Dispatch** | Termux Assistant AI | **REIMPLEMENT (Kotlin)** | Excellent (<1 MB RAM) | Phase 1 & 12 |
| **Goal Decomposition** | GPT Researcher | **ADAPT (Kotlin)** | Excellent (<2 MB RAM) | Phase 1 & 19 |
| **Web Research Extraction** | GPT Researcher | **ADAPT (Kotlin)** | Good (<20 MB RAM) | Phase 16 & 19 |
| **Document Ingestion** | Skill Seekers | **ADAPT (SAF Import)** | Good (<10 MB RAM) | Phase 17 |
| **Provenance Tracking** | Skill Seekers / LLM Wiki | **REUSE (Schema)** | Excellent (<1 MB RAM) | Phase 2 & 17 |
| **Contradiction Detection** | LLM Wiki Agent | **ADAPT (Kotlin)** | Excellent (<2 MB RAM) | Phase 17 & 19 |
| **Persistent Memory** | SAKU / LLM Wiki Agent | **REUSE / ADAPT** | Excellent (<5 MB RAM) | Phase 2 & 15 |
| **Episodic Task History** | Hermes Agent / SAKU | **LOCAL AGENT CORE** | Excellent (<10 MB RAM) | Phase 2 & 15 |
| **Procedural Skill Storage** | SAKU / Hermes Agent | **ADAPT (Kotlin)** | Excellent (<2 MB RAM) | Phase 15 |
| **Skill Discovery** | Hermes Agent | **ADAPT (Kotlin)** | Excellent (<1 MB RAM) | Phase 15 |
| **Reflection & Learning** | SAKU / Hermes Agent | **ADAPT (Kotlin)** | Good (<5 MB RAM) | Phase 15 |
| **Skill Versioning & Promotion** | Hermes Agent | **ADAPT (Kotlin)** | Excellent (<1 MB RAM) | Phase 15 |
| **Offline Operation** | LocalAgent Baseline | **LOCAL AGENT CORE** | Excellent (0 MB Net) | Phase 1–8 |
| **Low-RAM Heap Control** | LocalAgent Baseline | **LOCAL AGENT CORE** | Excellent (<35 MB Heap) | Phase 5–8, 21 |
| **Security & Policy Rules** | LocalAgent Baseline | **LOCAL AGENT CORE** | Excellent (<1 MB RAM) | Phase 1 & 3 |

---

## 3. Evidence-Driven Self-Improvement Lifecycle

For Phase 15 (`Episodic Memory & Learning`), procedural skills evolve through strict evidence gates:

```
[1. Task Execution]
       │
       ▼
[2. Postcondition Verification] ──► (Unverified / Failed?) ──► [Record Failure Telemetry & Halt]
       │
       ▼ (SUCCESS_VERIFIED)
[3. Immutable Evidence Capture] (SHA-256 Provenance Hash + Pre/Post Snapshots)
       │
       ▼
[4. Candidate Skill Creation] (Draft Status: version 1.0.0-draft)
       │
       ▼
[5. Regression Validation Gate] (Requires 3+ consecutive SUCCESS_VERIFIED runs)
       │
       ▼
[6. Versioned Skill Promotion] (Promoted Status: version 1.0.0-trusted)
       │
       ▼
[7. Controlled Skill Replay] (Re-validates live device state before re-executing steps)
```

### Safety & Governance Enforcements
1. **Zero Unverified Promotion:** A candidate skill is NEVER promoted without 3 consecutive `SUCCESS_VERIFIED` task executions.
2. **Rollback Control:** If a promoted skill fails live execution 2 consecutive times, its status is demoted to `CANDIDATE` and falls back to dynamic planning.
3. **Immutable Safety Policy:** Generated skills CANNOT modify system permissions, risk policies, or accessibility service bindings.

---

## 4. Integration Roadmap & Scope Enforcement

- **Current Active Work:** Phase 5–8 hardening (`AgentAccessibilityService`, `UiActionExecutor`, `AppControlEngine`, `ObservationActivity`).
- **Phase 9+ Handoff:** Concept adaptations from SAKU, Skill Seekers, GPT Researcher, Hermes Agent, and LLM Wiki Agent are strictly mapped to their designated future phases (Phases 12, 15, 16, 17, 19, 20) and **must not be implemented prematurely**.
