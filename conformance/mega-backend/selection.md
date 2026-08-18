# Mega Backend conformance selection

Pinned revision: `782cdec8dfe5b4062eb5c1859e6a9e53afe02770`.

The corpus is selected before analyzer remediation and supplies no extraction hints. The harness
selects exact methods and gives them business-facing titles. It does not change Mega source.

| Business area | Configured method | Why selected |
| --- | --- | --- |
| Journey warnings | `JourneyDirectionValidator.validate` | Stateful enum choice, compound conditions, assignments, early and final outcomes |
| Time warnings | `TimeOverlapCalculator.isOverlapping` | Boundary comparison with calls through the `ProjectEntry` interface; proves polymorphic alternatives |
| Month end | `MonthEndClarification.canBeResolvedBy` | Authorization decision with status, role, creator, subject employee, and project-lead facts |
| Projects | `Project.isActiveIn` | Date-range boundary decision with derived month start/end values |
| Journey-warning orchestration | `WarningCalculatorsManager.determineJourneyWarnings` | Real strategy-interface dispatch across three source-visible calculators; primary polymorphism proof |

The time-overlap method is private in the reference source but is invoked by the public warning
calculator. Exact configured roots support this method without an annotation. Runtime invocation
must occur through the public calculator path.
The manager entry is the decisive brownfield polymorphism case. Journey warnings were selected
because all participating business strategies are source-visible; time-warning orchestration also
crosses into a binary-only external holiday engine and therefore cannot yield a complete graph
without pretending that unavailable dependency logic was understood.
