# Dependency Audit: Maven Project Analysis

## Dependency Inventory

| Component | Version | Scope | License | Source |
| --- | --- | --- | --- | --- |
| Maven Plugin API/Core | 3.9.16 | provided | Apache-2.0 | Apache Maven |
| Maven Plugin Tools annotations/plugin | 3.15.2 | provided/build | Apache-2.0 | Apache Maven |

## CVE Scan Results

Trivy's available offline vulnerability database reports zero medium, high, or critical findings
for the plugin POM and both integration-fixture POMs. An attempted database refresh failed because
the local credential helper could not access the public mirror; the offline scan completed.

## EOL Status

Maven 3.9.16 and Plugin Tools 3.15.2 are current official releases at implementation time.

## Verification Method

Versions and recommended annotation-based Mojo setup were verified against official Apache Maven
3.9.16 and Maven Plugin Tools 3.15.2 documentation. The resolved dependency tree and local
filesystem vulnerability scan were captured during final verification. The plugin runtime tree
contains only `fachtracing-engine` and its `fachtracing-api` dependency; Maven API/Core and Plugin
Tools are provided/build-time only.

## Allowed Advisories

None.
