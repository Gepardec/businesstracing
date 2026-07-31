# Maven plugin setup

The Fachtracing Maven plugin finds annotated decisions from Maven source roots and the resolved compile classpath. It writes deterministic graph files under `target/fachtracing` by default.

## One-off analysis

Add `fachtracing-api` to the project, annotate a decision method, and run:

```sh
mvn compile at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-SNAPSHOT:analyze
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
  <version>0.1.0-SNAPSHOT</version>
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

The plugin ignores `module-info.java` during graph analysis. Maven still compiles each module
descriptor and applies its JPMS rules. A module with no Java source skips analysis before the plugin
resolves reactor classpaths, and it removes stale Fachtracing output.

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

Each source dependency must use the exact `groupId:artifactId:version` form. The plugin resolves only
the named `sources` classifier. It does not scan dependencies and does not infer source artifacts.
Maven repository mirrors, credentials, and offline mode apply. In offline mode, the exact source JAR
must already be in the local repository. A missing artifact causes a deterministic failure.

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
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-SNAPSHOT:analyze
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
