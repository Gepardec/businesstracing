# Implementation Tasks: Self Runtime Tracing

## Spec-Level Dependencies

None.

## Task Breakdown

### Task 1: Add the Runtime Self-Trace Contract

**Status:** Completed
**Estimated Effort:** M
**Dependencies:** None
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Add one executable harness that runs the traced production policy through the current activation
bundle and Java agent.

**Implementation Steps:**

1. Load the activation bundle and select the developer-export decision.
2. Register a safe `Optional` value adapter and configure the collector and agent.
3. Invoke disabled, enabled, and invalid configuration scenarios.
4. Validate terminal status, business-safe result data, observations, selected edges, record count,
   declared evidence gaps, and agent diagnostics.

**Acceptance Criteria:**

- [x] The disabled call produces one successful `empty` execution.
- [x] The enabled call produces one successful `present` execution.
- [x] The invalid call preserves its exception and produces one generic failed execution.
- [x] Each execution contains an observed selected edge.
- [x] No extra record or unexpected diagnostic exists.

**Files to Modify:**

- `fachtracing-maven-plugin/src/test/java/at/gepardec/fachtracing/maven/SelfTracingRuntimeTest.java` (new)

**Tests Required:**

- [x] Runtime self-trace harness compiles against current project contracts

---

### Task 2: Connect Runtime Capture to the Self-Tracing Gate

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 1
**Priority:** High
**IssueID:** None
**Blocker:** None

**Description:**
Start the harness from the existing self-tracing script after the static pass succeeds.

**Implementation Steps:**

1. Check the invalid static path in the generated graph.
2. Build the Maven-plugin test classpath.
3. Start Java 21 with the current agent JAR and reactor classes.
4. Require the runtime success marker before the script prints its final marker.

**Acceptance Criteria:**

- [x] One command runs static analysis and runtime capture in order.
- [x] The command uses the agent and classes from the current reactor build.
- [x] A missing runtime trace or fingerprint mismatch fails the command.

**Files to Modify:**

- `scripts/verify-self-tracing.sh`

**Tests Required:**

- [x] `FACHTRACING_SKIP_PROJECT_BUILD=true ./scripts/verify-self-tracing.sh`

---

### Task 3: Explain and Verify the Two-Pass Flow

**Status:** Completed
**Estimated Effort:** S
**Dependencies:** Task 2
**Priority:** Medium
**IssueID:** None
**Blocker:** None

**Description:**
Update the guide from a static-only example to the verified two-pass example, then run complete
repository checks.

**Implementation Steps:**

1. Correct the documented graph so that it includes the invalid configuration path.
2. Explain Maven analysis, activation, agent transformation, and runtime records.
3. Run focused and full verification.

**Acceptance Criteria:**

- [x] The guide explains the two process passes and three outcomes.
- [x] The guide uses the checked current graph semantics.
- [x] The full repository verifier passes.

**Files to Modify:**

- `docs/self-tracing.md`

**Tests Required:**

- [x] `./scripts/verify.sh`

## Implementation Order

1. Task 1 creates the runtime proof.
2. Task 2 adds that proof to the existing gate.
3. Task 3 explains and verifies the complete flow.

## Progress Tracking

- Total Tasks: 3
- Completed: 3
- In Progress: 0
- Blocked: 0
- Pending: 0
