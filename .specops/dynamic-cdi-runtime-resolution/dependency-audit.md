# Dependency Audit: Dynamic CDI runtime resolution

**Verified:** 2026-08-18T12:08:00Z
**Threshold:** medium
**Result:** PASS

## Dependency Inventory

The change adds `org.jboss.weld.se:weld-se-core:6.0.4.Final` in test scope. Weld supplies the real
CDI 4.1 container for conformance. It is not a production dependency of the Jakarta EE adapter.

## Review

- The Weld project lists 6.0.4.Final as a stable CDI 4.1 release for Jakarta EE 11.
- Maven resolved the dependency and its transitive graph.
- Repository verification proves that the external release does not contain the test container.
- No production source imports Weld or Jakarta CDI classes.

## Verification

- `./scripts/verify-dynamic-cdi.sh` passed with Weld SE 6.0.4.Final.
- `./scripts/verify.sh` passed, including the external release contract.
- `./scripts/verify-pr.sh` passed all pull-request conformance gates.
