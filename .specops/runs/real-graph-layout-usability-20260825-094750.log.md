---
specId: "real-graph-layout-usability"
startedAt: "2026-08-25T09:47:50Z"
completedAt: "2026-08-25T09:57:29Z"
finalStatus: "draft"
phases:
  - phase-1
  - phase-2
---

## Phase 1: Understand Context

### [09:47:50] Step 1: Load configuration

- Action: Read the SpecOps configuration and inspect the working tree.
- Result: `.specops.json` is absent, so SpecOps defaults apply. The working tree was clean.

### [09:48:10] Step 2: Recover related work

- Read: `.specops/index.json`
- Read: `.specops/static-graph-layout-quality/bugfix.md`
- Read: `.specops/static-graph-layout-quality/design.md`
- Read: `.specops/frontend-visual-design-quality/bugfix.md`
- Result: The completed static-layout fix is the direct dependency. The broad visual-design draft remains related but is not continued because this request is limited to static real-graph usability.

### [09:48:30] Step 3: Load project context

- Read: `.specops/steering/product.md`
- Read: `.specops/steering/tech.md`
- Read: `.specops/steering/structure.md`
- Read: `.specops/steering/reference-application.md`
- Read: `.specops/steering/dependencies.md`
- Read: `.specops/steering/repo-map.md`
- Read: `.specops/memory/context.md`
- Read: `.specops/memory/decisions.json`
- Read: `.specops/memory/patterns.json`
- Result: Loaded six steering files, 52 indexed specs, 131 decisions, and 19 recurring patterns. No production-learnings file exists.

### [09:49:00] Decision: Keep one focused bug-fix spec

- Choice: Create `real-graph-layout-usability` as one specification.
- Rationale: Loading, viewport policy, placement, routing, and search focus form one static-graph acceptance slice and share one layout pipeline. Run state, database behavior, and CI are excluded.

## Phase 2: Create Specification

### [09:50:00] Step 1: Define real-graph evidence

- Action: Record the three supplied business graph identities, hashes, measured viewport scales, layout times, and route-quality failures.
- Result: The evidence set contains 19-, 45-, and 55-node complete business graphs without copying or hard-coding their topology into product code.

### [09:51:00] Step 2: Create specification artifacts

- Write: `.specops/real-graph-layout-usability/bugfix.md`
- Write: `.specops/real-graph-layout-usability/design.md`
- Write: `.specops/real-graph-layout-usability/tasks.md`
- Write: `.specops/real-graph-layout-usability/implementation.md`
- Write: `.specops/real-graph-layout-usability/spec.json`
- Write: `.specops/real-graph-layout-usability/dependency-audit.md`
- Result: Created an implementation-ready high-severity frontend bug-fix specification.

### [09:56:25] Step 3: Verify coherence and evaluate the specification

- Action: Cross-check requirements, design, tasks, dependency decisions, numeric limits, and requirement coverage.
- Result: Coherence passed. Spec evaluation passed with scores 9, 8, 8, and 9.
- Write: `.specops/real-graph-layout-usability/evaluation.md`
- Edit: `.specops/real-graph-layout-usability/spec.json`
- Edit: `.specops/index.json`
