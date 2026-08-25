# Evaluation Report: Real Graph Layout Usability

## Spec Evaluation

### Iteration 1

**Evaluated at:** 2026-08-25T09:56:25Z  
**Threshold:** 7/10

### Criteria Testability

**Evidence:** `bugfix.md` records the evidence hashes and current measurements, then defines EARS criteria for busy state, effective text size, neighborhood context, aspect-ratio candidate rejection, zero-collision gates, crossing density, candidate-relative detour, selection, safe-control clearance, compatibility, and local responsiveness. `design.md` defines the formulas for reading zoom, detour ratio, crossing density, and placement score.

**Finding:** Final visual acceptance still includes a human task. The spec contains this subjective part with four explicit operator actions and does not let it replace the objective geometry, timing, identity, and accessibility gates.

**Score:** 9/10 — Pass

### Criteria Completeness

**Evidence:** The requirements cover pending, ready, replaced, failed, Reading, Overview, low-detail overview, exact search, duplicate search, no match, resize, missing optional evidence, dense cycles, convergence, long routes, and both V1 formats. The regression inventory includes the shared run canvas even though run behavior is out of scope.

**Finding:** The four-second POC gate depends on local hardware. The spec reduces this risk by tying it to the existing acceptance environment and requiring an immediate busy state, but implementation evidence must record the machine and browser context with the result.

**Score:** 8/10 — Pass

### Design Coherence

**Evidence:** One state model controls worker lifecycle, view mode, selection, and focus revision. Placement profiles preserve ELK coordinates and use one lexicographic score. Route refinement uses shortest valid obstacle-aware candidates and a bounded three-pass set-level improvement. Component responsibilities keep geometry outside Svelte. No dependency or contract change is introduced.

**Finding:** Profile option values are intentionally bounded but not fully enumerated. Task 3 must record the final small profile set and its stable IDs in the implementation journal so that later tuning cannot silently change deterministic output.

**Score:** 8/10 — Pass

### Task Coverage

**Evidence:** Task 1 owns metrics and the review harness. Task 2 owns state and viewport modes. Task 3 owns placement. Task 4 owns route sets. Task 5 owns selection. Task 6 owns complete verification. The requirement coverage table maps RB-01 through RB-08, and dependencies form a straight acyclic order.

**Finding:** Task 6 has broad verification responsibility. It must remain verification and documentation only; geometry corrections found during final review must return to Tasks 3 or 4 and be recorded as remediation, not be added to browser test code.

**Score:** 9/10 — Pass

| Dimension | Score | Threshold | Result |
| --- | ---: | ---: | --- |
| Criteria Testability | 9 | 7 | Pass |
| Criteria Completeness | 8 | 7 | Pass |
| Design Coherence | 8 | 7 | Pass |
| Task Coverage | 9 | 7 | Pass |

**Verdict:** PASS — all four dimensions meet the threshold.

**Implementation review points:** Record final placement profile IDs, record the local timing environment, keep geometry repair out of Svelte and tests, and require human approval after objective gates pass.

## Implementation Evaluation

### Iteration 1

**Evaluated at:** 2026-08-25T10:53:52Z
**Threshold:** 7/10

### Root Cause Accuracy

**Evidence:** Reading and Overview now use separate viewport policies. Full ELK coordinates replace flat global-rank reconstruction. The static topology spine uses a memoized longest-forward-path calculation instead of exponential path enumeration. Route-set metrics detect crossing, detour, branch, collision, and corridor defects. Search now owns persistent selection.

**Score:** 10/10 — Pass

### Fix Completeness

**Evidence:** RB-01 through RB-08 are checked. Both V1 formats pass. The three real graphs pass the same objective review command as generated fixtures. Reading, selected Reading, Overview, light theme, dark theme, resize, duplicate search, no match, and stale replacement have browser coverage.

**Finding:** External PostgreSQL and generated dogfood browser fixtures were not available. They are unchanged behavior and have unit or static-canvas coverage, but five external-fixture journeys were skipped.

**Score:** 9/10 — Pass

### Regression Safety

**Evidence:** No graph contract, SQL, HTTP, persistence, dependency, lockfile, or CI file changed. All original nodes and edges stay in the Svelte Flow model. Context emphasis is static-preview-only when no run highlight is present. The full unit and graph-only production browser suites pass.

**Score:** 9/10 — Pass

### Test Verification

**Evidence:** 58 unit tests pass. Svelte diagnostics report zero errors and warnings. The production build passes. Eleven applicable full-suite Playwright journeys pass, including real-file display and stale-layout replacement. All three graph-review results are below 0.5 crossings per edge and four seconds, with zero objective hard defects.

**Finding:** The registry-backed npm advisory refresh was denied by the restricted environment. No dependency changed.

**Score:** 9/10 — Pass

| Dimension | Score | Threshold | Result |
| --- | ---: | ---: | --- |
| Root Cause Accuracy | 10 | 7 | Pass |
| Fix Completeness | 9 | 7 | Pass |
| Regression Safety | 9 | 7 | Pass |
| Test Verification | 9 | 7 | Pass |

**Verdict:** PASS — the implementation meets the specification and the real-graph usability acceptance slice.

### Iteration 2 — User rejection

**Evaluated at:** 2026-08-25T11:20:33Z
**Verdict:** FAIL

- **Root Cause Accuracy: 4/10.** The router treats topology-long edges as outer-corridor routes and filters the detour baseline by the selected crossing score. This permits a visibly wrong route to report a 1.0 detour.
- **Fix Completeness: 3/10.** RG-MEGA-DIRECTION still has a 665-pixel wrong-way loop and a detached branch label.
- **Regression Safety: 5/10.** Static focus hides most unrelated topology, and Overview removes node text at low zoom.
- **Test Verification: 4/10.** Tests prove internal metrics but do not reject the supplied screenshot defects. The earlier manual approval was incorrect.

**Remediation:** Complete Tasks 7 and 8. Use full valid-candidate detour evidence, reject wrong-way boundary exits, remove detached label leaders, remove static context dimming, and keep node text present at all supported zoom levels.

### Iteration 3 — Final direct graph review

**Evaluated at:** 2026-08-25T19:30:00Z
**Threshold:** 7/10

### Root Cause Accuracy

**Evidence:** The final correction treats the problem as two separate user tasks. Explore uses a new local layout and cannot inherit remote corridors from the complete graph. Overview retains the complete topology. A reversible presentation transform removes safe repetition before either layout. Directed entry traversal identifies real feedback edges without using node-ID order.

**Score:** 10/10 — Pass

### Fix Completeness

**Evidence:** RB-01 through RB-09 are checked. The default view reaches the first material split, shows a clear continuation instruction, and keeps selected-node context local. Readable groups safe action, rule, guard, and parallel structures. Full detail restores exact source counts. The generic explanation uses no more than three actual sentences. The three supplied files have verified Explore, selected, Overview, light, and dark states.

**Score:** 10/10 — Pass

### Regression Safety

**Evidence:** The complete layout remains cached for Overview. A pending Overview request is not lost when the worker returns. Compact local layout retries with standard spacing when route constraints reject it. Original IDs remain in search, accessibility, and run-highlight mappings. No graph contract, Java, SQL, HTTP, persistence, dependency, lockfile, or CI file changed.

**Score:** 10/10 — Pass

### Test Verification

**Evidence:** 71 unit tests pass across 16 files. Svelte diagnostics report no errors or warnings. The production build passes. Eleven applicable Playwright journeys pass and five external-fixture journeys skip. All three complete graph-review reports pass with zero hard defects. Browser geometry checks find zero node intrusion and label collisions in each default real-file Explore view and require every visible card to be at least 160 by 60 CSS pixels.

**Finding:** The skipped journeys require PostgreSQL or a generated dogfood directory. They are not configured in this local verification environment.

**Score:** 10/10 — Pass

| Dimension | Score | Threshold | Result |
| --- | ---: | ---: | --- |
| Root Cause Accuracy | 10 | 7 | Pass |
| Fix Completeness | 10 | 7 | Pass |
| Regression Safety | 10 | 7 | Pass |
| Test Verification | 10 | 7 | Pass |

**Verdict:** PASS — the static graph preview now has a readable local task, a complete overview task, reversible source detail, and objective plus visual evidence on all supplied graphs.

### Iteration 4 — Explanation workspace after user rejection

**Evaluated at:** 2026-08-25T19:31:00Z
**Threshold:** 7/10

| Dimension | Score | Evidence |
| --- | ---: | --- |
| Root Cause Accuracy | 10 | The review identified the remaining defect as missing explanation structure, invented route meaning, and poor use of canvas space, not as another geometry-only problem. |
| Fix Completeness | 10 | RB-10 passes. Explore identifies the current step, all sequence members, incoming context, and every immediate continuation. Overview is explicitly a topology map. |
| Regression Safety | 10 | The guide is static-preview-only. Run views, both V1 contracts, search, Readable, Full detail, and source ID mappings remain unchanged. |
| Test Verification | 10 | 76 unit tests, Svelte check, production build, 11 applicable browser journeys, all three file reviews, and desktop, dark, Overview, selected, and narrow screenshots pass. |

**Verdict:** PASS — the preview is now an explanation workspace. It does not invent outcomes, and it uses supplied business labels for navigation.
