# Implementation Tasks: Omit Redundant Next Diagram Labels

## Task Breakdown

### Task 1: Hide exact next labels in both diagram formats

**Status:** Completed  
**Estimated Effort:** S  
**Dependencies:** None  
**Priority:** Medium  
**IssueID:** None  
**Blocker:** None

**Description:**
Remove the exact `next` text from rendered Mermaid and PlantUML arrows while preserving all graph data and meaningful outcomes.

**Implementation Steps:**

1. Add a small display-label rule to each renderer.
2. Add focused assertions for exact and prefixed outcomes.
3. Update the four diagram snapshots and the self-tracing example.
4. Run focused and repository verification.

**Acceptance Criteria:**

- [x] Exact `next` labels are absent from Mermaid and PlantUML.
- [x] `next item` and other meaningful outcomes remain visible.
- [x] Graph data and execution-path behavior are unchanged.

**Files to Modify:**

- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/mermaid/MermaidRenderer.java`
- `fachtracing-engine/src/main/java/at/gepardec/fachtracing/plantuml/PlantUmlRenderer.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/mermaid/MermaidRendererTest.java`
- `fachtracing-engine/src/test/java/at/gepardec/fachtracing/plantuml/PlantUmlRendererTest.java`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-structure.mmd`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-execution.mmd`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-structure.puml`
- `fachtracing-engine/src/test/resources/snapshots/eligibility-execution.puml`
- `docs/self-tracing.md`

**Tests Required:**

- [x] Mermaid renderer executable contract passes.
- [x] PlantUML renderer executable contract passes.
- [x] Self-tracing verification passes.

## Implementation Order

1. Task 1

## Progress Tracking

- Total Tasks: 1
- Completed: 1
- In Progress: 0
- Blocked: 0
- Pending: 0
