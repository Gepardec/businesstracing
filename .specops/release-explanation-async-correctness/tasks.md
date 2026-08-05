# Tasks: Release, Explanation, and Async Correctness

## Task 1: Freeze failing regression contracts

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** PR #5 review at `19e45d90bc3ce7e8b18b053f11fff117c0ecbd37`
**Blocker:** None

**Description:** Add independent failing-before-change tests for release status, operand evidence,
async rejection and cancellation, callback position, unsupported boundaries, and indexed-loop
vocabulary.

**Acceptance Criteria:**

- [x] Each reported root cause has one focused executable contract.
- [x] Caught and uncaught rejection are separate tests.
- [x] The evidence test includes a result-irrelevant identifier.
- [x] The loop test uses generic vocabulary and no Mega classes.

**Files to Modify:**

- `scripts/test-verify-release.sh`
- `fachtracing-agent/src/test/**`
- `fachtracing-engine/src/test/**`
- `conformance/mega-backend/src/test/**`

---

## Task 2: Make the release gate fail closed

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Preserve the producer status without a status-hiding pipeline and remove the
undeclared repository-verification dependency on `rg`.

**Acceptance Criteria:**

- [x] A failed producer returns non-zero and no success marker.
- [x] Failure output stays in the evidence file.
- [x] Repository integrity passes when `rg` is absent.

---

## Task 3: Generate and capture result-relevant evidence

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add evidence targets to the manifest and activation bundle. Generate them from
atomic predicates and the backward result slice. Remove blanket argument capture and attach typed
facts to predicate observations.

**Acceptance Criteria:**

- [x] Direct parameter operands are captured with a business-safe label.
- [x] Result-irrelevant parameters are not stored.
- [x] Explanations include operand value and exact edge outcome.
- [x] Old activation bundles remain readable.

---

## Task 4: Close async reservation lifecycles

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add atomic reservation states, submission rollback, and cancellation-before-run
release. Preserve the original application result or thrown object.

**Acceptance Criteria:**

- [x] Caught rejection publishes one successful record.
- [x] Uncaught rejection publishes one failed record.
- [x] Cancellation before execution publishes after the annotated method terminates.
- [x] Start, cancellation, and rejection races cannot underflow or publish twice.

---

## Task 5: Replace async heuristics with exact bindings

**Status:** Completed
**Estimated Effort:** L
**Dependencies:** Tasks 1 and 4
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Add the exact async invocation catalog and safe callback-position rewriting. Mark
unmatched async boundaries as incomplete.

**Acceptance Criteria:**

- [x] `thenCombine`, `thenAcceptBoth`, `runAfterBoth`, and `Thread(ThreadGroup,Runnable)` use the
  correct callback.
- [x] Existing executor, stage, platform-thread, and virtual-thread tests still pass.
- [x] An unmatched boundary adds one coverage gap.

---

## Task 6: Lower indexed iteration and remove ordinal candidates

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** Task 1
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:** Recognize canonical indexed collection loops as one iteration construct. Keep body
business logic and remove ordinal dispatch labels.

**Acceptance Criteria:**

- [x] Generic loop output has no counter initialization, size comparison, indexed item access, or
  counter update.
- [x] Dispatch output has no `candidate N` label or attribute.
- [x] A generic guard detects these technical patterns in exported graphs.

---

## Task 7: Run full conformance and release evidence

**Status:** In Progress
**Estimated Effort:** L
**Dependencies:** Tasks 2 through 6
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:** Run focused tests, standard verification, source-free external activation, Mega
conformance, PostgreSQL integration where configured, and the clean-clone release gate. Update only
reviewed generic graph oracles and SpecOps completion records.

**Acceptance Criteria:**

- [ ] Standard verification passes.
- [ ] Five Mega graphs are complete and pass the generic business-artifact guard.
- [ ] External activation uses no runtime source analysis.
- [ ] The release gate reports zero result changes, contamination, or silent loss.
- [ ] All tasks and definition-of-done items have recorded evidence.
