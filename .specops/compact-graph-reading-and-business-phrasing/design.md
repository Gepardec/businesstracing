# Design: Compact Graph Reading and Business Phrasing

## Architecture Overview

The fix changes two pure presentation boundaries. The viewer chooses a smaller topology slice before
ELK lays it out. The analyzer renders already-proved aggregate roles with a language-neutral shape.
Neither change modifies the public graph model.

## Technical Decisions

### Bounded compact context

Wide Explore view keeps the current complete direct neighborhood. Compact Explore view keeps the
focus and all unique direct successors. It adds predecessors in stable graph-edge order only while
the default three-card budget has capacity. The guide already lists all omitted predecessors and
can select each one.

The compact selector has no viewport or DOM dependency. `FlowCanvas` supplies the compact state
from the measured safe area. A threshold transition triggers a new local layout.

### Readable viewport floor

`readingViewport` uses the existing 0.62 neighborhood floor on all screen sizes. If the bounded
local graph cannot fit at that floor, the viewer frames the selected card at the 0.86 reading floor.
It does not shrink the full neighborhood to 0.48.

### Neutral aggregate grammar

The analyzer keeps source roles separate until it joins them. The visible form is:

```text
subject — collection: condition (qualifier)
```

When no subject exists, the form starts with `collection`. The qualifier contains source expression
text only. Punctuation supplies structure without an English or domain-specific dictionary.

## Component Design

- `compactNeighborhood`: pure graph topology selection with an explicit node budget.
- `FlowCanvas`: observes compact-state transitions and requests the correct local graph.
- `readingViewport`: enforces the readable floor.
- Aggregate label helpers: extract the condition and qualifier separately and join source terms.

## Security and Compatibility

The fix adds no customer data, network access, dependency, storage field, or JSON field. Existing
redaction and exact-source mapping remain unchanged.

## Testing Strategy

- Unit: compact topology, outgoing-alternative preservation, reading zoom, aggregate wording.
- Browser: 860-by-900 card size, guide clearance, and topology count on the real graph.
- Real graph: regenerated label text, graph review, and screenshot inspection.

## Risks and Mitigations

- A compact slice can omit useful incoming context. The guide keeps and exposes every omitted
  predecessor.
- A graph can have many direct outgoing alternatives. The selector keeps them all because hiding a
  choice is worse than falling back to focus framing.
- Punctuation can expose poor source names. This is safer than inventing translations and keeps the
  limitation visible.
