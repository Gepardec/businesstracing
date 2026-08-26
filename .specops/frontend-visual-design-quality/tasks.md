# Implementation Tasks: Graph Viewer Visual Design Quality

## Spec-Level Dependencies

| Dependent Spec | Reason | Required | Status |
| --- | --- | --- | --- |
| `frontend-flow-explorer` | This bugfix revises the completed viewer design without changing its contracts. | Yes | Completed |

## Task Breakdown

### Task 1: Build visual state fixtures and baseline harness

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Create generated fixtures for every required state and add approved image comparison. Keep all diagrams generated from graph documents or runtime records.

**Implementation Steps:**

1. Add generated dashboard, run, node-state, long-label, failure, incomplete, missing-evidence, repeated-visit, loading, and error fixtures.
2. Capture the complete visual reference matrix in the pinned Playwright browser.
3. Add image comparison with a documented threshold and narrow masks.
4. Add a review checklist for manual design sign-off.

**Acceptance Criteria:**

- [ ] Every required reference state exists in light or dark and at its specified viewport.
- [ ] Visual comparison fails when a reference image changes beyond the threshold.
- [ ] No hard-coded product diagram or graph position is added.

**Files to Modify:**

- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/e2e/visual-fixtures.ts`
- `fachtracing-viewer/playwright.config.ts`
- `fachtracing-viewer/README.md`

**Tests Required:**

- [ ] Playwright reference generation and comparison
- [ ] Fixture source audit for generated topology only

---

### Task 2: Establish semantic tokens and page composition

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Implement the semantic color roles, type scale, spacing system, application shell, dashboard, compact detail header, and loaded preview toolbar.

**Implementation Steps:**

1. Replace overlapping state tokens with separate status, interaction, focus, graph, and node-type roles.
2. Add focused presentation components for the decision header and loaded graph toolbar.
3. Simplify the dashboard heading and search form.
4. Add the stacked phone result layout.
5. Verify all truncation, reveal, tooltip, and copy behavior.

**Acceptance Criteria:**

- [ ] IA-01 through IA-05, LY-05 through LY-07, AC-01, AC-02, AC-06, RS-01, and RS-04 pass.
- [ ] The phone dashboard has no horizontal page scroll.
- [ ] Status colors appear only in status and error roles.

**Files to Modify:**

- `fachtracing-viewer/src/app.css`
- `fachtracing-viewer/src/lib/layout/AppShell.svelte`
- `fachtracing-viewer/src/lib/runs/DecisionHeader.svelte`
- `fachtracing-viewer/src/lib/runs/RunFilters.svelte`
- `fachtracing-viewer/src/lib/runs/RunList.svelte`
- `fachtracing-viewer/src/routes/runs/+page.svelte`
- `fachtracing-viewer/src/routes/runs/[executionId]/+page.svelte`
- `fachtracing-viewer/src/routes/graphs/+page.svelte`
- `fachtracing-viewer/src/lib/graph/GraphUpload.svelte`

**Tests Required:**

- [ ] Light and dark token contrast tests
- [ ] Dashboard desktop and phone visual tests
- [ ] Detail header and preview loaded-state visual tests

---

### Task 3: Rebuild node and edge grammar

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Apply the restrained node family, non-status run states, and measurable edge routing rules without changing graph topology.

**Implementation Steps:**

1. Update node shapes, type rails, icons, labels, borders, and state precedence.
2. Remove the predicate diamond marker and success-like outcome styling.
3. Expose four-side layout ports and preserve the selected source and target port for each rendered edge.
4. Update edge width, color, label mapping, label clearance, and arrowheads.
5. Add shortest-valid-route selection, stable route corridors, and simple-fixture route quality checks.
6. Add the monochrome and interaction-state galleries.

**Acceptance Criteria:**

- [ ] GR-01 through GR-09 and ED-01 through ED-08 pass.
- [ ] Every node kind remains identifiable in monochrome.
- [ ] Current, path, focus, status, and coverage-gap states cannot be confused by color or border role.
- [ ] All existing route intrusion and parallel-route tests pass.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/BusinessNode.svelte`
- `fachtracing-viewer/src/lib/graph/BusinessEdge.svelte`
- `fachtracing-viewer/src/lib/graph/edge-label.ts`
- `fachtracing-viewer/src/lib/graph/edge-label.test.ts`
- `fachtracing-viewer/src/lib/graph/edge-route.ts`
- `fachtracing-viewer/src/lib/graph/edge-route.test.ts`
- `fachtracing-viewer/src/lib/graph/layout-definition.ts`
- `fachtracing-viewer/src/lib/graph/graph.test.ts`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`

**Tests Required:**

- [ ] Node grammar unit and visual tests
- [ ] Edge label mapping tests
- [ ] Four-side port selection and shortest-valid-route tests
- [ ] Route bend, segment, reversal, clearance, collision, and parallel-path tests

---

### Task 4: Add reading, overview, search, and current-step viewport modes

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 3
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Stop using full fit as the only initial view. Add explicit reading and overview modes and keep graph controls in one toolbar.

**Implementation Steps:**

1. Add reading view with the 0.78 minimum zoom and effective text floor.
2. Add explicit Overview and Current step actions.
3. Make search focus include local predecessor and successor context.
4. Group graph controls for desktop and phone.
5. Preserve viewport and selection across inspector changes.

**Acceptance Criteria:**

- [ ] LY-01 through LY-04, LY-06, GR-08, AC-03, RS-03, and RS-05 pass.
- [ ] A real seven-node graph opens with readable labels.
- [ ] A 250-node search result retains local structural context.
- [ ] Overview remains available for complete topology inspection.

**Files to Modify:**

- `fachtracing-viewer/src/lib/graph/FlowCanvas.svelte`
- `fachtracing-viewer/src/lib/graph/FitGraph.svelte`
- `fachtracing-viewer/src/lib/graph/FocusCurrent.svelte`
- `fachtracing-viewer/src/lib/graph/GraphToolbar.svelte`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`

**Tests Required:**

- [ ] Effective rendered text-size test
- [ ] Canvas-use composition test
- [ ] Reading, overview, search, and current-step journeys
- [ ] 250-node local-context visual test

---

### Task 5: Replace inferred prose with structured explanations

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Task 2
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Create one explanation adapter that presents recorded business facts without inventing causal text. Redesign the inspector around question, answer, evidence, and readable result.

**Implementation Steps:**

1. Add the `StepExplanation` presentation model and adapter.
2. Map Boolean outcomes to Yes and No only in presentation.
3. Show typed evidence as labeled display values.
4. Put raw outcomes, IDs, canonical values, and selected edge IDs in technical details.
5. Redesign selected, failed, missing-evidence, and repeated-visit states.
6. Make the inspector toolbar sticky and improve phone sheet hierarchy and focus.

**Acceptance Criteria:**

- [ ] EX-01 through EX-09 and RS-02 through RS-03 pass.
- [ ] No presentation code infers a causal sentence from arbitrary strings.
- [ ] The operator journey exposes question, answer, evidence, and final result without technical details.

**Files to Modify:**

- `fachtracing-viewer/src/lib/runs/step-explanation.ts`
- `fachtracing-viewer/src/lib/runs/step-explanation.test.ts`
- `fachtracing-viewer/src/lib/runs/run-highlight.ts`
- `fachtracing-viewer/src/lib/runs/run-highlight.test.ts`
- `fachtracing-viewer/src/lib/runs/RunInspector.svelte`
- `fachtracing-viewer/src/lib/components/ui/Sheet.svelte`
- `fachtracing-viewer/src/routes/runs/[executionId]/+page.svelte`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`

**Tests Required:**

- [ ] Structured explanation unit tests
- [ ] Failed, incomplete, missing-evidence, and repeated-visit journeys
- [ ] Phone sheet keyboard and focus tests

---

### Task 6: Enforce accessibility, motion, and complete visual quality

**Status:** Pending
**Estimated Effort:** L
**Dependencies:** Tasks 3, 4, 5
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Complete the state matrix, accessibility checks, reduced-motion behavior, full regression suite, and manual design approval.

**Implementation Steps:**

1. Add rendered contrast and zoom checks in both themes.
2. Add keyboard journeys for toolbar, graph nodes, inspector, disclosures, and sheet.
3. Add reduced-motion tests.
4. Run all unit, browser, build, database, dogfood, and repository CI gates.
5. Review and approve the complete visual reference matrix.
6. Update viewer documentation with design rules and proof commands.

**Acceptance Criteria:**

- [ ] AC-03 through AC-05 and QA-01 through QA-05 pass.
- [ ] All Must-Test unchanged behavior passes.
- [ ] The full reference matrix has human design approval.
- [ ] Hosted CI passes on the exact pushed commit.

**Files to Modify:**

- `fachtracing-viewer/src/app.css`
- `fachtracing-viewer/e2e/decision-explorer.spec.ts`
- `fachtracing-viewer/README.md`
- `.github/workflows/pr-gate.yml`

**Tests Required:**

- [ ] Unit tests
- [ ] Svelte type and accessibility checks
- [ ] Playwright visual, geometry, content, responsive, and keyboard tests
- [ ] PostgreSQL integration and generated Fachtracing dogfood journey
- [ ] Hosted PR CI on the exact commit

## Implementation Order

1. Task 1 creates the proof system before visual changes.
2. Task 2 establishes shared composition and tokens.
3. Tasks 3 and 5 can proceed after Task 2.
4. Task 4 follows the graph grammar from Task 3.
5. Task 6 integrates and verifies all work.

## Progress Tracking

- Total Tasks: 6
- Completed: 0
- In Progress: 0
- Blocked: 0
- Pending: 6
