# Dependency Audit: Self-Explainable Runtime Dogfood

**Verified:** 2026-08-18T08:40:29Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

This specification adds no package, Maven dependency, plugin dependency, or version change. It uses the Java standard library and current project modules.

## Verification Method

- Compared all changed POM content with `origin/main`.
- Confirmed that the new profile configures the existing `fachtracing-maven-plugin` at `${project.version}`.
- Ran the full local PR gate and pinned external conformance gates.

## Allowed Advisories

None.
