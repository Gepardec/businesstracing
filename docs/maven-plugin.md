# Maven plugin setup

The Fachtracing Maven plugin finds annotated decisions from Maven source roots and the resolved compile classpath. It writes deterministic graph files under `target/fachtracing` by default.

## One-off analysis

Add `fachtracing-api` to the project, annotate a decision method, and run:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze
```

This command needs no plugin block. It writes these files for each decision:

- `<decision>-structure.mmd`
- `<decision>-structure.puml`
- `index.md`

## Lifecycle analysis

Use this plugin block to generate the files during `process-classes`, `package`, and `verify`:

```xml
<plugin>
  <groupId>at.gepardec.fachtracing</groupId>
  <artifactId>fachtracing-maven-plugin</artifactId>
  <version>0.1.0-rc.1</version>
  <executions>
    <execution>
      <goals>
        <goal>analyze</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

A parent POM can put this block under `build/plugins` to run it for child modules. Modules without an annotated decision do not produce graph files.

## Multi-module reactors

For a reactor build, each plugin execution uses the current module's Java sources as decision-entry
roots. It also uses Java sources and compile classpaths from the active reactor to resolve compatible
interface and abstract-method implementations. Thus, an implementation in a sibling module can
appear as a dispatch candidate without creating a graph for an annotation in that sibling module.

For a modular graph entry, the plugin puts all connected named module descriptors and sources in one valid
multi-module compiler task. It uses the effective module paths and module source paths. Graph
extraction uses this same attributed task. A module with no Java source skips graph generation and
removes stale Fachtracing output.

### One aggregate reactor result

Run the aggregator after compilation when you want one result directory for the effective Maven
selection:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
```

The goal runs once. It uses the projects that Maven selected, including the effect of `-pl` and
`-am`. It writes the diagrams, `index.md`, and `activation.json` under the execution root's
`target/fachtracing` directory. The activation file contains each graph, its exact runtime probe
manifest, original class fingerprints, the source-boundary fingerprint, and the exact Java-agent
option. A runtime can load this file without Java source or a compiler. Duplicate decision labels
get graph-ID suffixes, so one graph cannot replace another.

Activation generation also scans selected project output directories for the exact supported
`Future.cancel(boolean)`, `CompletableFuture.cancel(boolean)`, and
`ForkJoinTask.cancel(boolean)` bytecode calls. It fingerprints each matching application caller so
cancellation in a separate controller can release the correct trace reservation. Dependency output
directories and JAR files remain lookup-only and are not added by this scan.

Optional `includeProjects` and `excludeProjects` lists use exact `groupId:artifactId` values. They
filter Maven's effective selection. An include value that is not in that selection causes a failure.
The existing `analyze` goal remains available for per-module output.

The aggregate goal supports the same `additionalSourceRoots`, `additionalEntrySourceRoots`,
`sourceDependencies`, and `opaqueLibraryArtifacts` settings as the module goal. You can also set
these lists with the matching `fachtracing.*` command-line properties. In a reactor with a POM
execution root, an additional entry root uses the first selected non-POM project's compiler context.
External sources in a JPMS closure must have explicit module ownership. The analyzer rejects an
unowned external source in a modular closure.

## Explicit external source inputs

Use extra roots when reachable decision code is not in the active reactor. An additional source root
is resolution-only. It cannot create a graph entry. An additional entry root can contain annotated
entry methods.

```xml
<configuration>
  <additionalSourceRoots>
    <additionalSourceRoot>${project.basedir}/../shared-rules/src/main/java</additionalSourceRoot>
  </additionalSourceRoots>
  <additionalEntrySourceRoots>
    <additionalEntrySourceRoot>${project.basedir}/imported-decisions</additionalEntrySourceRoot>
  </additionalEntrySourceRoots>
  <sourceDependencies>
    <sourceDependency>com.acme:shared-rules:2.4.1</sourceDependency>
  </sourceDependencies>
</configuration>
```

For a named external source module, bind the ownership entry by source root or source identity:

```xml
<externalModuleOwnership>
  <ownership>
    <sourceRoot>${project.basedir}/../shared-rules/src/main/java</sourceRoot>
    <kind>named</kind>
    <moduleName>com.acme.shared.rules</moduleName>
    <descriptor>${project.basedir}/../shared-rules/src/main/java/module-info.java</descriptor>
  </ownership>
</externalModuleOwnership>
```

For source that belongs to an automatic module, name the exact Maven-resolved binary that provides
the module identity:

```xml
<externalModuleOwnership>
  <ownership>
    <identity>maven:com.acme:legacy-rules:2.4.1</identity>
    <kind>automatic</kind>
    <moduleName>com.acme.legacy.rules</moduleName>
    <binaryPath>${settings.localRepository}/com/acme/legacy-rules/2.4.1/legacy-rules-2.4.1.jar</binaryPath>
  </ownership>
</externalModuleOwnership>
```

One ownership entry must match each modular external origin. Duplicate matches, unreadable
descriptors, invalid module names, and a binary that is not on Maven's module path fail before
graph extraction. Ownership and checksums are developer provenance. They do not enter diagrams or
records.

Each source dependency must use the exact `groupId:artifactId:version` form. The plugin resolves only
the named `sources` classifier. It does not scan dependencies and does not infer source artifacts.
Maven repository mirrors, credentials, and offline mode apply. In offline mode, the exact source JAR
must already be in the local repository. A missing artifact causes a deterministic failure.

The analyzer reads the effective `maven-compiler-plugin` configuration for each project. It keeps
the encoding, language-selection mode, analysis-safe `compilerArgs`, preview and parameter flags,
generated source root, module descriptor, and module path. An explicit `release` stays a
`--release` setting. Equal `source` and `target` values stay `-source` and `-target` settings. The
analyzer rejects different source and target levels, forked compiler executables, and legacy
free-form compiler arguments.

Maven must run annotation processors during `compile`. Fachtracing does not execute them again. It
removes processor paths, processor names, processor options, and the Maven processing mode from its
private compiler model, then runs source attribution with `-proc:none`. Run `compile` and analysis in
the same Maven invocation so generated Java is present in the registered compile source roots:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
```

This supports processors such as MapStruct that generate normal Java source. An AST-only
transformation that does not produce equivalent Java source can still cause an attribution error.
Fachtracing fails in that case and does not claim complete graph coverage.

The plugin extracts source JARs below
`target/fachtracing-source-dependencies/<archive-sha256>`. It rejects absolute paths, traversal,
backslash paths, duplicate entries, invalid paths, and archives that exceed these defaults:

- `fachtracing.sourceArchiveMaxEntries`: 10,000 entries
- `fachtracing.sourceArchiveMaxEntryBytes`: 4 MiB per entry
- `fachtracing.sourceArchiveMaxTotalBytes`: 128 MiB in total

Set `fachtracing.sourceExtractionDirectory` to change the cache directory. A normal Maven `clean`
removes the default cache. Source archives can contain private source code. Protect the Maven local
repository, build directory, CI artifacts, and developer JSON according to the source owner's access
rules.

## Explicit technical library boundaries

Source-unavailable dependency code is decision-bearing by default. Thus, a result-relevant call into
an unselected dependency JAR creates a coverage gap. Use `opaqueLibraryArtifacts` only for technical
libraries whose internal decisions must stay outside the business graph:

```xml
<configuration>
  <opaqueLibraryArtifacts>
    <opaqueLibraryArtifact>com.acme.persistence:query-api</opaqueLibraryArtifact>
    <opaqueLibraryArtifact>org.apache.commons:commons-lang3</opaqueLibraryArtifact>
  </opaqueLibraryArtifacts>
</configuration>
```

Each value must use exact `groupId:artifactId` form. The plugin maps the value to the exact resolved
JAR paths that occur on the selected projects' compile classpaths. It does not use package prefixes
or artifact-name patterns. An invalid coordinate, a missing artifact, a runtime-only artifact, or a
class directory causes the goal to fail before graph extraction.

You can set the same list for one-off use:

```sh
mvn compile \
  -Dfachtracing.failOnIncomplete=true \
  -Dfachtracing.opaqueLibraryArtifacts=com.acme.persistence:query-api,org.apache.commons:commons-lang3 \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze-reactor
```

Selection is an explicit trust declaration. Do not select an artifact that contains business rules.
A selected reference-returning operation is opaque, and source-visible receiver controls remain in
the graph. A selected Boolean call is opaque only when the source call is already a control
predicate. A direct binary Boolean decision stays fail-closed.

## Developer JSON and source links

Developer JSON is opt-in because source browsers need repository-specific URLs. Set both values:

```xml
<configuration>
  <repositoryUrl>https://github.com/acme/decision-rules</repositoryUrl>
  <sourceUrlTemplate>https://github.com/acme/decision-rules/blob/{commit}/{path}#L{line}</sourceUrlTemplate>
</configuration>
```

The plugin then adds `<decision>-developer.json` and links it from `index.md`. The file uses UTF-8.
A single Git origin keeps the compatible `fachtracing-developer-graph/v1` format. A graph with local,
generated, or Maven sources uses `fachtracing-developer-graph/v2`. V2 lists `sourceOrigins` and gives
each source an `originId`. Only Git sources get a commit-pinned `url`. External and generated sources
never get a false Git URL. A graph tool can render `graph.nodes` and `graph.edges` in both versions.

For one-off use, pass the same settings as properties:

```sh
mvn compile \
  -Dfachtracing.repositoryUrl=https://github.com/acme/decision-rules \
  '-Dfachtracing.sourceUrlTemplate=https://github.com/acme/decision-rules/blob/{commit}/{path}#L{line}' \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-rc.1:analyze
```

The Git worktree must be clean. The plugin records the full `HEAD` commit and verifies each Git source
fingerprint against both the current file and the file blob in that commit. V2 verifies external
source content against the analysis fingerprint and records Maven archive checksums. It fails if only
one setting is present, the worktree is dirty, a Git source is absent from the commit, or source
content does not match the analysis.

Developer JSON contains repository and source data. Mermaid and PlantUML remain business-facing and do not contain these details.

## Other settings

```xml
<configuration>
  <outputDirectory>${project.build.directory}/fachtracing</outputDirectory>
  <encoding>UTF-8</encoding>
  <failOnIncomplete>true</failOnIncomplete>
  <skip>false</skip>
</configuration>
```

- `failOnIncomplete` fails the goal after it writes visible coverage gaps. Its default is `false`.
- `skip` disables analysis. Its default is `false`.
- `outputDirectory` changes the generated-file directory.
- `encoding` controls Mermaid, PlantUML, and Markdown. Developer JSON always uses UTF-8.

Equivalent command-line properties are `fachtracing.outputDirectory`, `fachtracing.encoding`, `fachtracing.failOnIncomplete`, and `fachtracing.skip`.

The plugin removes stale files that match its generated names. It does not delete unrelated files from the output directory.
