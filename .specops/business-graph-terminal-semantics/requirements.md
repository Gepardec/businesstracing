# Feature: Business Graph Terminal Semantics

## Overview

Generated graphs currently use technical-looking identifier suffixes and create one generic final
node per return. Business readers need a single Start/Stop lifecycle and terminal edges that state
the returned business value.

## Acceptance Criteria

- [x] THE SYSTEM SHALL generate exactly one `Start` entry and one `Stop` outcome for each annotated decision graph.
- [x] WHEN a root method returns through any branch THE SYSTEM SHALL connect that path to the shared `Stop` and state the returned expression on the terminal edge.
- [x] WHEN a root method terminates exceptionally THE SYSTEM SHALL connect its failure path to the shared `Stop` without representing normal continuation.
- [x] THE SYSTEM SHALL remove standalone `id` and `ids` suffix words from all generated business labels using a domain-neutral rule.
- [x] THE SYSTEM SHALL express result-relevant Java null comparisons as business absence/existence statements and SHALL NOT expose the word `null`.
- [x] Runtime outcome probes from every return SHALL continue to correlate with the shared `Stop` node and preserve the typed final result.
- [x] PlantUML, Mermaid, explanations, generic fixtures, and pinned Mega conformance SHALL reflect the improved semantics.

## Scope

This changes generated graph labels and topology oracles. Defensive null guards remain excluded;
business-significant optional values remain visible as “absent” or “exists.” Opaque internal node/edge identifiers
remain unchanged as protocol correlation data and are still hidden behind renderer aliases.
