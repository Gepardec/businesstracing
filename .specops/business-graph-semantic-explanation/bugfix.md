# Bug Fix: Business Graph Semantic Explanation

## Problem Statement

The business graph is structurally correct, but it does not explain the business decision. It exposes
source identifiers, helper methods, architecture roles, and negated Java expressions as business
steps. A business reader must know the source code to understand labels such as:

- `personen adapter rule`
- `angehoerige adapter rule`
- `regelwerk adapter rule`
- `speichere au meldung port`
- `choose by svnr`
- `bis is absent`
- `wert is absent`
- `not von is after stichtag`
- `not ist unter altersgrenze geburtsdatum entry and stichtag`

The last example also loses the subject. The rule concerns a child and an insurance age limit, but the
label names only two operands and one helper method. This is not an explainable business graph.

## Scope Boundary

**In scope:** Static semantic extraction, caller-to-callee role propagation, predicate polarity,
temporal comparison wording, source-visible boundary classification, technical-node suppression,
business-result naming, projection audit data, and local acceptance with the two supplied insurance
graphs and unrelated domain fixtures.

**Out of scope:** Viewer layout, graph routing, run focus, active-run highlighting, JSON schema V2,
SQL, HTTP endpoints, PostgreSQL, LLM calls, application-specific production rules, hard-coded
diagrams, production benchmarks, and CI configuration.

The existing `fachtracing-business-graph/v1` JSON shape stays unchanged. The change improves the
labels and the projected topology.

## Evidence Set

The specification uses two generated graph files as local review evidence. The files stay outside
production code and test fixtures. The hashes identify the reviewed content.

| Evidence ID | Decision | Nodes | Edges | SHA-256 |
| --- | --- | ---: | ---: | --- |
| BG-ENTITLEMENT | check benefit entitlement | 29 | 43 | `db6976432d42d21e45792170d70caa5613629c56f905e59df23535422e79b6fd` |
| BG-NOTIFICATION | submit incapacity notification | 31 | 45 | `333bb7d844273ef09ece3c7fc5577d76ab2e7967570b8ad5a1ac98637b655998` |

Both documents report `COMPLETE`. Completeness here means that analysis found the control paths. It
does not mean that the business explanation is good.

## Root Cause Analysis

### 1. The analyzer stores rendered syntax as semantic text

`StaticDecisionAnalyzer.predicateLabel` falls back to `expression(predicate)`. Switch nodes use
`choose by <expression>`. Negation can add `not` before an already complex phrase. These paths keep
the Java expression shape instead of a structured business proposition.

### 2. Method expansion loses the caller's business role

The analyzer expands source-visible helper methods, but it does not carry the caller role into the
expanded parameters and return values. A birth date can therefore become only `geburtsdatum entry`.
The graph loses the fact that the value belongs to the child.

### 3. Projection treats every non-blocked computation as a business action

`BusinessGraphProjector` keeps a `COMPUTATION` when its label does not match a short technical deny
list. The deny list does not classify adapters, mappers, ports, rule wrappers, selectors, getters, or
data conversion. Therefore implementation boundaries become visible business actions.

### 4. The artifact guard checks words, not meaning

`BusinessLogicArtifactGuard` rejects selected Java tokens. It accepts grammatically invalid or
meaningless labels when those labels do not contain a prohibited token. It does not require a known
subject, a business verb, or a complete proposition.

### 5. The summarizer merges only equivalent graph states

`BusinessGraphSummarizer` merges gap regions and behaviorally equivalent nodes. It does not splice
transparent helpers, merge predicate scaffolding, remove selector nodes, or combine related date
checks into one material rule.

### 6. Terminal naming uses narrow special cases

`BusinessGraphProjector.resultLabel` recognizes a small set of result forms. It otherwise emits
`operation failed`, `completed`, a ternary expression, or the decision name plus `completed`. These
labels do not state the business result.

## Impact Assessment

- **Severity:** High
- **Affected users:** Business users, support staff, auditors, and developers who review business
  decisions.
- **Frequency:** Deterministic for applications that use layered architecture, helper rules, German
  identifiers, generic value wrappers, or negated comparisons.
- **Business impact:** A user cannot explain why a claim was accepted or rejected without reading
  source code.
- **Data impact:** None. The defect is in static interpretation and presentation.

## Semantic Model

The analyzer must build an internal semantic statement before it builds a display label. A semantic
statement has these parts:

- **kind:** rule, action, result, or unknown;
- **subject:** the business entity or business value that the statement concerns;
- **verb or relation:** the action or test;
- **object:** the target entity or comparison value, when one exists;
- **qualifiers:** time, scope, source, or other context;
- **polarity:** positive or negative;
- **confidence:** explicit, source-proven, context-proven, or unknown;
- **provenance:** exact source nodes, edges, symbols, call bindings, and optional semantic contracts.

Display text is a rendering of this model. Display text is not the semantic model.

## Required Behavior

### BR-01 — Preserve behavior and traceability

- [ ] FOR every exact path to a terminal result THE SYSTEM SHALL preserve one corresponding business
  path with the same terminal meaning.
- [ ] WHEN the projection removes or merges an exact node THE SYSTEM SHALL preserve its exact node and
  edge mapping in developer-only projection audit data.
- [ ] WHEN a predicate is rewritten from negative to positive form THE SYSTEM SHALL invert its
  outgoing branch mapping so runtime evidence still selects the correct business edge.
- [ ] THE SYSTEM SHALL NOT mark an incomplete semantic decision as complete only because a technical
  node was removed.

### BR-02 — Propagate business subjects through calls

- [ ] WHEN the analyzer expands a source-visible call THE SYSTEM SHALL bind each callee parameter to
  the caller argument's semantic role.
- [ ] WHEN a value is read from an entity THE SYSTEM SHALL retain the entity role with the value. For
  example, a birth date that comes from a child remains the child's birth date.
- [ ] WHEN a returned value receives a meaningful local or field role THE SYSTEM SHALL carry that role
  to downstream predicates.
- [ ] WHEN two call paths bind the same helper parameter to different roles THE SYSTEM SHALL keep the
  roles path-specific. It SHALL NOT use one global replacement.
- [ ] WHEN no subject can be proved THE SYSTEM SHALL emit a semantic gap. It SHALL NOT invent a
  subject or require application-specific code.

### BR-03 — Classify source calls by business effect

- [ ] THE SYSTEM SHALL classify each relevant call as a material rule, material action, lookup,
  transparent transformation, orchestration wrapper, or unknown boundary.
- [ ] THE SYSTEM SHALL derive this classification from source-visible behavior, attributed symbols,
  dataflow, effects, and existing exact contracts for source-unavailable methods. A class suffix
  alone SHALL NOT prove a business meaning.
- [ ] WHEN an adapter, mapper, converter, getter, factory, or rule wrapper only moves or converts data
  THE SYSTEM SHALL remove that node and splice its incoming and outgoing paths.
- [ ] WHEN a port call causes a material business effect THE SYSTEM SHALL keep the effect and remove
  the architecture role from its label. For example, a save port can become `save incapacity
  notification`.
- [ ] WHEN a wrapper only delegates to material child rules THE SYSTEM SHALL show the child rules and
  remove the wrapper.
- [ ] WHEN an unknown source-unavailable call can change the result THE SYSTEM SHALL keep the existing
  fail-closed coverage behavior.

### BR-04 — Render one complete business proposition per rule

- [ ] EACH visible rule SHALL identify a subject and one test.
- [ ] THE SYSTEM SHALL render predicates in positive canonical form where this does not hide meaning.
- [ ] THE SYSTEM SHALL use `yes` and `no` only when they answer the visible positive proposition.
- [ ] THE SYSTEM SHALL eliminate leading `not`, double negation, raw `and` expression text, and raw
  method-call argument lists from visible labels.
- [ ] WHEN one source condition contains short-circuit atoms THE SYSTEM SHALL keep each material atom
  as a rule or combine them only when they form one recognized business relation.
- [ ] THE SYSTEM SHALL NOT combine unrelated predicates into one sentence.

### BR-05 — Explain temporal rules

- [ ] WHEN a comparison uses a period start, period end, reference date, or age threshold THE SYSTEM
  SHALL identify the operand roles before it renders the comparison.
- [ ] THE SYSTEM SHALL normalize inverse and negated comparison operators. For example,
  `not start is after reference date` becomes `period starts on or before the reference date`.
- [ ] WHEN an end date is absent and absence means an open period THE SYSTEM SHALL render `period has
  no end date`, not `<field> is absent`.
- [ ] WHEN a period contains a reference date THE SYSTEM SHALL prefer one rule such as `coverage is
  active on the reference date` over separate implementation checks for start, end, and helper
  state, if the combined rule is path-equivalent.
- [ ] WHEN an age limit is a proven constant THE SYSTEM MAY render the value, such as `under 18`.
  Otherwise it SHALL render the named or configured age limit.

### BR-06 — Explain lookup and selection

- [ ] WHEN a selector only routes a switch THE SYSTEM SHALL remove the `choose by` node and keep the
  case meaning on the outgoing business paths.
- [ ] WHEN a lookup determines whether a business entity exists THE SYSTEM SHALL render a proposition
  such as `person was found by social insurance number`.
- [ ] THE SYSTEM SHALL NOT show a raw key name as a standalone action.
- [ ] THE SYSTEM SHALL NOT use `choose by <identifier>` as a business label.

### BR-07 — Resolve generic values by context

- [ ] THE SYSTEM SHALL resolve generic names such as `value`, `entry`, `from`, `to`, `start`, `end`,
  and `result` from their attributed symbol, declaring type, access path, call binding, and downstream
  use.
- [ ] WHEN context identifies a value as an age limit THE SYSTEM SHALL render `age limit is not
  configured`, not `value is absent`.
- [ ] WHEN context identifies a child's birth date THE SYSTEM SHALL render `child's birth date is
  missing`, not `birth date is empty`.
- [ ] WHEN context is insufficient THE SYSTEM SHALL emit a semantic gap. It SHALL NOT guess from one
  ambiguous token.

### BR-08 — Keep inference domain-neutral

- [ ] The reported insurance examples SHALL work through generic source and graph analysis. They
  SHALL NOT require an insurance adapter, glossary, contract provider, or configuration file.
- [ ] Production code and built-in inference rules SHALL NOT contain insurance-specific packages,
  class names, method names, field names, labels, translations, or expected graph shapes.
- [ ] THE SYSTEM SHALL apply the same role, relation, materiality, polarity, and reduction algorithms
  to unrelated applications.
- [ ] Generic test fixtures SHALL cover at least insurance eligibility, shipping, and access control.
  No fixture-specific label SHALL enter production code.
- [ ] Existing exact method contracts SHALL CONTINUE TO describe source-unavailable libraries. The
  implementation SHALL NOT use them as a required escape hatch for source-visible application code.
- [ ] THE SYSTEM SHALL keep one phrase grammar inside a label and preserve meaningful source domain
  nouns. It SHALL NOT concatenate raw operands from one language with relation fragments from
  another language.

### BR-09 — Simplify the business topology

- [ ] THE SYSTEM SHALL remove transparent action chains and connect the nearest material predecessor
  and successor.
- [ ] THE SYSTEM SHALL merge repeated predicates only when their semantic statement, polarity, and
  downstream behavior are equivalent.
- [ ] THE SYSTEM SHALL remove duplicate helper checks that are fully represented by one retained
  business rule.
- [ ] THE SYSTEM SHALL keep separate paths when they have different business results, evidence, or
  runtime branch identity.
- [ ] THE SYSTEM SHALL keep the graph acyclic only when the exact business behavior is acyclic. It
  SHALL NOT remove a real business retry or cycle to make the graph smaller.

### BR-10 — Name business results

- [ ] EACH terminal node SHALL state a business result, not a Java return expression or generic
  completion state.
- [ ] A Boolean decision SHALL use distinct positive and negative results when the source or explicit
  semantic contract proves them.
- [ ] A failure result SHALL identify the business failure when it can be proved, such as missing
  configuration or rejected entitlement. It SHALL use a semantic gap when it cannot be proved.
- [ ] THE SYSTEM SHALL NOT export ternary expressions, method names, `operation failed`, or
  `<decision> completed` as the final label when a more specific source-proven result exists.

### BR-11 — Enforce semantic quality

- [ ] THE artifact guard SHALL reject visible labels that contain architecture roles such as adapter,
  mapper, converter, implementation, or port unless the word is part of an explicitly approved
  business term.
- [ ] THE artifact guard SHALL reject `choose by`, leading `not`, unresolved standalone values, raw
  Boolean syntax, and rule-wrapper suffixes in visible labels.
- [ ] THE artifact guard SHALL validate structure as well as vocabulary: a rule needs a subject and a
  relation; an action needs a material verb and object; a result needs a terminal business state.
- [ ] Guard violations SHALL identify the source node and the missing semantic part.

### BR-12 — Keep contract compatibility

- [ ] THE SYSTEM SHALL CONTINUE TO export `fachtracing-business-graph/v1` with the current fields and
  node kinds.
- [ ] THE SYSTEM SHALL CONTINUE TO export the exact developer graph without semantic node removal.
- [ ] THE SYSTEM SHALL CONTINUE TO correlate runtime paths through exact-to-business mappings.
- [ ] THE SYSTEM SHALL NOT require a new JSON format for existing viewers.

### BR-13 — Evidence acceptance

- [ ] BG-ENTITLEMENT SHALL contain no visible adapter, port, rule-wrapper, raw selector, raw `value`,
  raw `from` or `to`, leading negation, or ternary-result label.
- [ ] BG-NOTIFICATION SHALL contain no visible adapter, port, rule-wrapper, raw selector, raw `value`,
  raw `from` or `to`, leading negation, or generic operation-failure label when the source proves a
  more specific failure.
- [ ] BG-ENTITLEMENT SHALL preserve the material facts for own entitlement, active coverage on the
  reference date, child identity, child's birth date or age rule, related insured persons, and the
  final entitlement result when those facts are present in the exact graph.
- [ ] BG-NOTIFICATION SHALL preserve the entitlement decision and the material action that saves the
  incapacity notification.
- [ ] BOTH graphs SHALL pass the five-part explanation review in this specification.
- [ ] IF either evidence file is absent THEN generic construct fixtures SHALL still run and the local
  evidence review SHALL report that it did not run.

## Reported Example Transformations

These transformations are acceptance examples. They do not authorize hard-coded production labels.

| Current label | Required semantic treatment | Example display text |
| --- | --- | --- |
| `personen adapter rule` | Remove transparent adapter or name its proved lookup effect. | No node, or `find person` |
| `angehoerige adapter rule` | Remove transparent adapter; retain the related-person lookup or rule. | `find insured family members` |
| `regelwerk adapter rule` | Remove the wrapper; retain the configured threshold fact. | `insured age limit is configured` |
| `speichere au meldung port` | Keep material persistence effect; remove architecture role. | `save incapacity notification` |
| `choose by svnr` | Remove selector mechanics or render the proved entity lookup. | `person was found by social insurance number` |
| `bis is absent` | Resolve the period role. | `coverage period has no end date` |
| `wert is absent` | Resolve the configured-value role. | `insured age limit is not configured` |
| `not von is after stichtag` | Normalize relation and polarity. | `coverage starts on or before the reference date` |
| `not stichtag is after bis` | Normalize relation and polarity. | `coverage ends on or after the reference date` |
| `not ist unter altersgrenze geburtsdatum entry and stichtag` | Restore subject, age rule, date context, and branch polarity. | `child is below the insured age limit on the reference date` |
| `pruefe anspruch hat anspruch svnr ? anspruch vorhanden : kein anspruch` | Replace code expression with distinct terminal states. | `entitlement confirmed` / `no entitlement` |

The value `18` can replace `insured age limit` only when static analysis proves that value for the
path.

## Five-Part Explanation Review

Each evidence graph must score 5 of 5. A partial score is a failed acceptance review.

1. **Orientation:** The title and first material step state which business decision and subject the
   graph concerns.
2. **Rule clarity:** Each visible rule is one complete proposition that a business reader can answer
   without source code.
3. **Branch clarity:** Each branch outcome answers its source rule and reaches an identifiable next
   fact, action, or result.
4. **Relevance:** Each visible action changes business state, retrieves required business facts, or
   explains a result. No architecture or conversion step remains.
5. **Result clarity:** Each reachable terminal states the business result and distinguishes accepted,
   rejected, saved, failed, and semantically unknown outcomes where applicable.

## Regression Risk Analysis

### Blast Radius

- `StaticDecisionAnalyzer` creates exact node labels and expands source-visible calls.
- New internal semantic-role and statement components interpret attributed symbols and call bindings.
- `BusinessGraphProjector` decides which exact nodes stay visible and preserves mapping data.
- `BusinessGraphSummarizer` can splice transparent nodes and merge equivalent semantic rules.
- `BusinessLogicArtifactGuard` validates final explanation quality.
- Business JSON, Mermaid, PlantUML, runtime activation, and execution explanations consume the
  projected graph.

### Risk Tiers

| Behavior | Tier | Reason |
| --- | --- | --- |
| Exact graph topology and probe bindings | Must-Test | Semantic projection must not change runtime instrumentation. |
| Predicate polarity and edge mapping | Must-Test | A wrong inversion explains the opposite result. |
| Transparent-node splicing | Must-Test | Removal can disconnect a path or merge distinct paths. |
| Terminal result naming | Must-Test | Every explanation ends at a result. |
| Existing V1 JSON parsing | Must-Test | The user must not generate a new format. |
| Domain-neutral role inference | Must-Test | A token-based shortcut can leak meaning between symbols or applications. |
| Existing external method contracts | Must-Test | They remain the trusted source-unavailable semantic path. |
| Mermaid and PlantUML text | Nice-To-Test | They consume the same graph model. |

## Acceptance Criteria

- [ ] The internal semantic statement model exists and does not replace the public V1 graph model.
- [ ] Caller roles remain attached across nested source-visible calls.
- [ ] Predicate polarity normalization preserves runtime branch identity.
- [ ] Technical boundaries are removed or rewritten by material effect.
- [ ] Generic value and date operands use contextual roles or a semantic gap.
- [ ] Terminal nodes state source-proven business results.
- [ ] The artifact guard rejects the reported failure classes.
- [ ] Generic fixtures from at least three unrelated domains prove that the rules are not
  insurance-specific.
- [ ] Both supplied evidence graphs pass the five-part explanation review.
- [ ] Existing V1 business JSON and developer JSON remain compatible.

## Team Conventions

- Use ASD-STE100 Simplified Technical English.
- Keep one responsibility in each component.
- Do not use subagents.
- Do not add hard-coded diagrams or application-specific production rules.
- Do not change CI configuration for this work.
