# Evaluation Report: Normalize nested map transfer labels

## Spec Evaluation

### Iteration 1

**Evaluated at:** 2026-08-19T21:05:59Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | --- | --- | --- |
| Criteria Testability | The spec gives an exact generic failing label and expected output. | External generation also verifies the real label shape. | 10 | 7 | Pass |
| Criteria Completeness | Current, expected, and unchanged behavior cover normalization and guard safety. | Only the proved repeated-subject form is in scope. | 9 | 7 | Pass |
| Design Coherence | The correction stays in the existing language-normalization component. | The strict guard remains unchanged. | 10 | 7 | Pass |
| Task Coverage | One task covers reproduction, correction, repository tests, and requested outputs. | No new dependency or public contract is required. | 10 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.

## Implementation Evaluation

### Iteration 1

**Evaluated at:** 2026-08-19T21:19:42Z
**Threshold:** 7/10

| Dimension | Evidence | Findings | Score | Threshold | Pass/Fail |
| --- | --- | --- | --- | --- | --- |
| Root Cause Accuracy | The focused pre-fix test and pinned Hogarama run failed on the same nested map form. | No guard rule caused the source label. | 10 | 7 | Pass |
| Fix Completeness | The focused test and both pinned Hogarama graphs pass. | The rule intentionally requires one repeated subject. | 10 | 7 | Pass |
| Regression Safety | The strict guard is unchanged and the full PR gate passed. | PostgreSQL was skipped because no connection was configured; this label change has no JDBC path. | 9 | 7 | Pass |
| Test Verification | Focused, Mega, Hogarama, PetClinic, Jakarta EE, load, and repository gates passed. | All seven delivered graph documents are complete V1 JSON. | 10 | 7 | Pass |

**Verdict:** PASS — 4 of 4 dimensions passed.
