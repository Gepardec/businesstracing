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

## Developer JSON and source links

Developer JSON is opt-in because source browsers need repository-specific URLs. Set both values:

```xml
<configuration>
  <repositoryUrl>https://github.com/acme/decision-rules</repositoryUrl>
  <sourceUrlTemplate>https://github.com/acme/decision-rules/blob/{commit}/{path}#L{line}</sourceUrlTemplate>
</configuration>
```

The plugin then adds `<decision>-developer.json` and links it from `index.md`. The file uses UTF-8 and the `fachtracing-developer-graph/v1` format. A graph tool can render `graph.nodes` and `graph.edges`. When a user selects a node with a `source` object, the tool can open `source.url`.

For one-off use, pass the same settings as properties:

```sh
mvn compile \
  -Dfachtracing.repositoryUrl=https://github.com/acme/decision-rules \
  '-Dfachtracing.sourceUrlTemplate=https://github.com/acme/decision-rules/blob/{commit}/{path}#L{line}' \
  at.gepardec.fachtracing:fachtracing-maven-plugin:0.1.0-SNAPSHOT:analyze
```

The Git worktree must be clean. The plugin records the full `HEAD` commit and verifies each analyzed source fingerprint against both the current file and the file blob in that commit. It fails if only one setting is present, the worktree is dirty, a source is outside the repository, a generated or ignored source is absent from the commit, or source content does not match the analysis.

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
