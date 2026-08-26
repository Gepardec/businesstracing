# Design: Business Graph Semantic Explanation

## Design Goal

Create a business graph from source-proven meaning, not from cleaned source text. Preserve the exact
graph for tracing. Project a smaller graph in which each visible node explains one material business
fact, action, or result.

## Current Flow

```text
Java source
  -> StaticDecisionAnalyzer
  -> BusinessDecisionGraph with string labels
  -> BusinessGraphProjector deny-list classification
  -> BusinessGraphSummarizer equivalence merge
  -> BusinessLogicGraph V1
```

The current flow makes the string label both the analysis fact and the display text. Later stages
cannot recover a lost subject or determine whether `adapter rule` is transparent.

## Proposed Flow

```text
Attributed Java source
  -> exact control and dataflow analysis
  -> semantic role propagation
  -> semantic statement extraction
  -> exact BusinessDecisionGraph and developer audit
  -> materiality projection
  -> semantic topology reduction
  -> locale-aware label rendering
  -> BusinessLogicGraph V1
```

The exact graph and runtime bindings stay authoritative. The semantic model is internal projection
metadata. The V1 business JSON remains a graph of text labels.

## Components

### 1. `SemanticRole`

One immutable internal value describes a business role.

```text
SemanticRole
  subject: SymbolRef
  entityRole: Optional<String>
  valueRole: Optional<String>
  ownerRole: Optional<SemanticRole>
  qualifiers: List<SemanticQualifier>
  confidence: EXPLICIT | SOURCE_PROVEN | CONTEXT_PROVEN | UNKNOWN
  provenance: List<SourceFact>
```

Examples:

- `child.birthDate`
- `coveragePeriod.startDate`
- `coveragePeriod.endDate`
- `decisionReferenceDate`
- `insuredAgeLimit`
- `incapacityNotification`

These names are examples of resolved roles. They are not built-in application mappings.

### 2. `SemanticRoleResolver`

This component has one responsibility: resolve roles for attributed values.

It uses this precedence:

1. source declaration and enclosing business entry name;
2. attributed type, record component, field, parameter, and accessor path;
3. caller argument to callee parameter binding;
4. returned value to assignment target binding;
5. downstream use when one unambiguous use names the role;
6. an existing exact method contract for a source-unavailable method;
7. unknown.

The resolver keys roles by compiler symbol and call path. It does not key them by token text. This
prevents one field named `value` from changing another field named `value`.

### 3. `BusinessStatement`

One immutable internal value describes the meaning of a possible visible node.

```text
BusinessStatement
  kind: RULE | ACTION | RESULT | TRANSPARENT | GAP
  subject: Optional<SemanticRole>
  relation: Optional<Relation>
  object: Optional<SemanticRoleOrLiteral>
  qualifiers: List<SemanticQualifier>
  polarity: POSITIVE | NEGATIVE
  materiality: MATERIAL | TRANSPARENT | UNKNOWN
  confidence: EXPLICIT | SOURCE_PROVEN | CONTEXT_PROVEN | UNKNOWN
  provenance: List<ExactGraphRef>
```

The statement owns comparison semantics. A renderer does not parse `not` from a string.

### 4. `BusinessStatementExtractor`

This component converts attributed source constructs into statements.

- A null or empty comparison becomes an availability relation on the resolved role.
- A temporal comparison becomes a start, end, containment, or threshold relation.
- A Boolean helper call becomes its source-visible returned proposition when analysis can prove it.
- A switch selector becomes transparent routing unless the selector itself is the business decision.
- A call becomes an action only when its state effect or external contract is material.
- A wrapper becomes transparent when all material behavior is in expanded children.

The extractor can use exact external method contracts. It does not broaden contract matching.

### 5. `BusinessMaterialityClassifier`

This component decides whether a statement is visible.

| Source behavior | Projection |
| --- | --- |
| Business predicate that changes a terminal path | Keep as `RULE` |
| Persistence, notification, or externally visible state change | Keep as `ACTION` |
| Lookup whose success or result changes a decision | Keep as `RULE` or `ACTION` based on source meaning |
| Adapter, mapper, converter, accessor, selector, or delegation wrapper | Mark `TRANSPARENT` |
| Source-unavailable result-relevant behavior without an exact contract | Keep as `GAP` |
| Calculation used only to implement a retained relation | Merge into that relation |

Names such as `Adapter`, `Port`, and `Rule` are weak architecture hints. The classifier must confirm
materiality from behavior. A `SaveNotificationPort` call is material because it changes external
state. The word `Port` is removed from the display label.

### 6. `SemanticGraphReducer`

This component reduces topology after materiality classification.

Reduction rules run in this order:

1. remove structural entry and outcome markers as today;
2. splice transparent nodes while preserving every exact path mapping;
3. canonicalize predicate polarity and invert edge meaning when required;
4. merge calculations into their single owning rule;
5. merge semantically equivalent rules only when their outgoing behavior is equivalent;
6. combine recognized period-bound checks into one containment rule when path equivalence is proved;
7. create source-proven terminal business results;
8. remove unreachable projected nodes;
9. run graph and semantic invariants.

The reducer must keep one-to-many and many-to-one provenance. The existing projection audit model
needs reasons for transparent transformation, wrapper inlining, selector removal, predicate
canonicalization, semantic merge, and semantic gap.

### 7. `BusinessPhraseRenderer`

This component renders one statement with one phrase grammar. It does not inspect Java syntax.

The renderer uses source-proven business nouns and semantic relations. It must not translate an
application term by class-name table or project configuration. A later generic locale renderer can
add another grammar without changing statement extraction.

Required English templates include:

- `<subject> exists`
- `<subject> is missing`
- `<period> has no end date`
- `<period> starts on or before <date>`
- `<period> ends on or after <date>`
- `<entity> is below <threshold> on <date>`
- `save <object>`
- `<business result>`

The renderer must not concatenate two language grammars. An unknown subject or relation causes a
semantic gap.

### 8. Domain-neutral proof policy

The supplied application must not select a special inference provider. The same production code
must analyze all applications.

Generic inference can use:

- attributed ownership and types;
- method and member morphology after generic architecture suffix removal;
- caller and callee argument bindings;
- return and assignment flow;
- comparison operator and operand positions;
- branch-to-terminal behavior;
- source-visible child statements;
- existing exact contracts only when source is unavailable.

Generic inference cannot use:

- an insurance package or type list;
- a project glossary;
- a graph-specific label map;
- a special expected diagram;
- a token rule that says `Kind` always means `child`;
- an exact contract added only to repair source-visible application labels.

For example, the child role must come from ownership and call context. The engine can retain the
source noun if translation is not available. It must not lose the subject or guess a translation.

## Predicate Polarity

Polarity is semantic, not textual.

For a source predicate such as:

```text
!isBelowAgeLimit(childBirthDate, referenceDate)
```

the extractor creates the positive statement `child is below the insured age limit on the reference
date` and records that source `true` means business `no`. The reducer swaps the business edge labels
and keeps the exact edge IDs in the mapping. Runtime selection therefore remains correct.

This rule also applies to inverse comparisons:

| Source relation | Canonical business relation |
| --- | --- |
| `!(start > referenceDate)` | `start is on or before reference date` |
| `!(referenceDate > end)` | `end is on or after reference date` |
| `value == null` | `<resolved role> is missing` |
| `value != null` | `<resolved role> is available` |

## Result Semantics

Terminal results use this precedence:

1. literal or enum business result;
2. named result object or return variable;
3. source-proven Boolean decision name and branch polarity;
4. nearest material failure fact;
5. semantic gap.

`operation failed` is allowed only as developer audit text. It is not an acceptable business result
when the graph claims to be a complete business explanation.

## Quality Invariants

The final business graph must satisfy all invariants:

1. Each rule has a known subject and relation.
2. Each action has a material verb and object.
3. Each result is a terminal business state.
4. Each non-result node can reach a result.
5. Each exact terminal path maps to a business terminal path.
6. Each `yes` or `no` edge answers the visible source rule.
7. No transparent implementation role is visible.
8. No semantic merge joins paths with different terminal meaning.
9. No application-specific rule exists in production code.
10. V1 JSON contains only the current public fields.

## Compatibility

- `BusinessDecisionGraph` stays the exact tracing graph.
- `BusinessLogicGraph` stays the public business graph model.
- `fachtracing-business-graph/v1` stays the exported JSON contract.
- Developer JSON keeps all source detail. The analyzer can add source-proven semantic nodes and
  attributes, so regenerated exact node and edge IDs can change.
- Runtime activation is regenerated from the new exact graph. Exact-to-business path mapping stays
  authoritative.
- Business graph IDs can change when semantic content changes. This is expected because the current
  ID already depends on node labels and topology.

## Error Handling

- An unknown non-material helper can be transparent only when dataflow proves that its child
  statement contains all material meaning.
- An unknown result-relevant predicate becomes a semantic gap.
- A phrase renderer that cannot render a complete statement returns a semantic violation. It does
  not emit partial text.

## Security and Data Handling

- Semantic extraction uses source symbols and static literals. It does not read runtime customer
  values.
- Business graph labels must not include personally identifiable runtime data.
- Existing evidence redaction and runtime value handling do not change.

## Test Design

### Construct fixtures

Use small applications from at least three unrelated domains:

- insurance eligibility: subject propagation, age threshold, open period, and terminal decision;
- shipping: port persistence versus adapter transformation;
- access control: selector removal, lookup existence, and positive predicate polarity.

Fixtures model Java constructs. They do not use production mappings or hard-coded diagrams.

### Path-equivalence tests

For every fixture:

1. enumerate exact entry-to-terminal paths within a bounded graph;
2. map each path through projection audit data;
3. confirm the same ordered material statements and terminal meaning;
4. confirm polarity against exact `true` and `false` edges;
5. confirm that transparent exact nodes remain in developer audit data.

### Local real-graph review

A local review command accepts arbitrary business or developer JSON paths. When developer audit
data is available, it evaluates semantic invariants and regenerates the business graph. For
business-only JSON, it evaluates the final label and topology invariants that the document contains.
The command verifies the two optional evidence hashes and prints the five-part explanation score.

## Alternatives Rejected

### Add more regular expressions to `BusinessLanguageNormalizer`

Rejected. A regular expression can improve grammar, but it cannot restore the child subject, prove a
material effect, or preserve branch polarity.

### Hide every class whose name ends in `Adapter`, `Port`, or `Rule`

Rejected. A save port can be a material business action. A rule class can contain the material
decision. Names are hints, not proof.

### Use one application-specific translation map or contract provider

Rejected. This would violate the domain-neutral product boundary and fail for the next application.

### Use an LLM during the build

Rejected for this scope. It would make output non-deterministic, add data-transfer concerns, and
could invent business meaning. Source-proven inference is deterministic.

### Change the business JSON schema

Rejected. The current V1 fields can carry the improved graph. Internal semantic metadata and
developer-only audit data do not require a public schema change.
