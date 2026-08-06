# Design: CI Isolated Maven Repository

## Approach

Add `scripts/maven-repository-path.sh` as the single path resolver. It prints one absolute Maven
repository path with this precedence:

1. `FACHTRACING_MAVEN_REPOSITORY` for an explicit verification override.
2. `FACHTRACING_RELEASE_MAVEN_REPOSITORY` for the clean release workspace.
3. `$HOME/.m2/repository` for normal local and CI Maven use.

The resolver removes repeated path separators before consumers compare Maven-generated classpaths.
This handles the trailing slash in the macOS `TMPDIR` value without changing spaces or symlinks.

`verify.sh`, `verify-mega-backend.sh`, `verify-postgres.sh`, and `verify-release-gates.sh` read this
path once and build all manual classpaths from it. Maven commands keep their existing repository
configuration.

## Architecture Decisions

- Use a small POSIX shell resolver so all verification entry points share one rule.
- Keep the current release environment variable for compatibility.
- Add a separate explicit override for direct verification and focused testing.
- Normalize repeated separators before classpath validation.
- Test the resolver without Maven or network access.

## Failure Behavior

The resolver exits when neither an override nor `HOME` provides a usable base. Existing `set -eu`
behavior then stops the calling gate. It never falls back to a different repository after an
explicit override.

## Security and Data

The resolver handles local file paths only. Data classification is Internal. It does not read
credentials, Maven settings, or repository contents.
