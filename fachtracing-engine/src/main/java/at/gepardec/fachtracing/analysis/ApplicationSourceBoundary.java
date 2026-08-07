package at.gepardec.fachtracing.analysis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable application boundary for project-aware source analysis.
 *
 * <p>Entry sources can contain {@code @FachTracing} graph roots. Resolution sources can only
 * supply reachable methods and implementation candidates. Project and origin data are developer
 * metadata and must not enter business graph labels or decision records.
 */
public record ApplicationSourceBoundary(
        List<ProjectSources> projects,
        List<ResolutionSource> externalResolutionSources) {

    /** Creates a validated, deterministic source boundary. */
    public ApplicationSourceBoundary {
        projects = List.copyOf(Objects.requireNonNull(projects, "projects"));
        externalResolutionSources = normalizedExternal(externalResolutionSources);
        if (projects.isEmpty()) throw new IllegalArgumentException("at least one project is required");

        Map<String, ProjectSources> byId = new LinkedHashMap<>();
        for (ProjectSources project : projects) {
            if (byId.put(project.projectId(), project) != null) {
                throw new IllegalArgumentException("duplicate project id: " + project.projectId());
            }
        }
        for (ProjectSources project : projects) {
            for (String dependency : project.projectDependencies()) {
                if (!byId.containsKey(dependency)) {
                    throw new IllegalArgumentException("unknown project dependency " + dependency
                            + " from " + project.projectId());
                }
            }
        }
    }

    /** Converts the original request contract into one project-aware boundary. */
    public static ApplicationSourceBoundary from(AnalysisRequest request) {
        Objects.requireNonNull(request, "request");
        return new ApplicationSourceBoundary(List.of(new ProjectSources(
                "legacy",
                request.rootSourceFiles(),
                request.sourceFiles(),
                request.compilationClasspath(),
                new CompilerModel(request.charset(), "21", List.of()),
                List.of())), List.of());
    }

    /** Returns all entry sources in stable path order. */
    public List<Path> entrySourceFiles() {
        return projects.stream().flatMap(project -> project.entrySourceFiles().stream())
                .distinct().sorted(Comparator.comparing(Path::toString)).toList();
    }

    /** Returns all project and external resolution sources in stable path order. */
    public List<Path> resolutionSourceFiles() {
        var paths = new LinkedHashSet<Path>();
        projects.stream().flatMap(project -> project.resolutionSourceFiles().stream()).forEach(paths::add);
        externalResolutionSources.stream().map(ResolutionSource::path).forEach(paths::add);
        return paths.stream().sorted(Comparator.comparing(Path::toString)).toList();
    }

    /** Returns the union of project compilation classpaths in stable path order. */
    public List<Path> compilationClasspath() {
        return projects.stream().flatMap(project -> project.compilationClasspath().stream())
                .distinct().sorted(Comparator.comparing(Path::toString)).toList();
    }

    /**
     * Adapts a compatible boundary to the existing flat analyzer request.
     *
     * <p>Task-specific compiler orchestration can consume the project models directly. The flat
     * adapter rejects incompatible compiler models instead of silently using one project's values.
     */
    public AnalysisRequest toAnalysisRequest() {
        CompilerModel first = projects.getFirst().compilerModel();
        boolean compatible = projects.stream().map(ProjectSources::compilerModel).allMatch(first::equals);
        if (!compatible) {
            throw new IllegalArgumentException("project compiler models differ; project-aware attribution is required");
        }
        if (!first.release().equals("21") || !first.compilerArguments().isEmpty()
                || !first.modulePath().isEmpty()) {
            throw new IllegalArgumentException("flat analysis supports only the Java 21 default compiler model");
        }
        return new AnalysisRequest(
                resolutionSourceFiles(), compilationClasspath(), first.charset(), entrySourceFiles());
    }

    /** Returns a deterministic fingerprint for boundary identity and runtime activation. */
    public String fingerprint() {
        StringBuilder value = new StringBuilder("fachtracing-application-boundary/v1\n");
        projects.stream().sorted(Comparator.comparing(ProjectSources::projectId)).forEach(project -> {
            value.append(project.projectId()).append('\n')
                    .append(project.compilerModel().charset().name()).append('\n')
                    .append(project.compilerModel().release()).append('\n');
            project.compilerModel().compilerArguments().forEach(item -> value.append("option:").append(item).append('\n'));
            project.compilerModel().modulePath().forEach(item -> value.append("module-path:").append(item).append('\n'));
            project.projectDependencies().forEach(item -> value.append("dependency:").append(item).append('\n'));
            project.moduleDescriptor().ifPresent(item -> value.append("module:").append(item).append('\n'));
            project.entrySourceFiles().forEach(item -> value.append("entry:").append(item).append('\n'));
            project.resolutionSourceFiles().forEach(item -> value.append("resolution:").append(item).append('\n'));
            project.compilationClasspath().forEach(item -> value.append("classpath:").append(item).append('\n'));
        });
        externalResolutionSources.stream()
                .sorted(Comparator.comparing(source -> source.path().toString()))
                .forEach(source -> value.append("external:").append(source.origin().kind()).append(':')
                        .append(source.origin().identity()).append(':').append(source.path()).append(':')
                        .append(source.ownership().fingerprint()).append('\n'));
        return sha256(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Sources and compiler settings for one effective build project. */
    public record ProjectSources(
            String projectId,
            List<Path> entrySourceFiles,
            List<Path> resolutionSourceFiles,
            List<Path> compilationClasspath,
            CompilerModel compilerModel,
            List<String> projectDependencies,
            Optional<Path> moduleDescriptor) {
        /** Compatibility constructor for callers that do not supply JPMS metadata. */
        public ProjectSources(
                String projectId,
                List<Path> entrySourceFiles,
                List<Path> resolutionSourceFiles,
                List<Path> compilationClasspath,
                CompilerModel compilerModel,
                List<String> projectDependencies) {
            this(projectId, entrySourceFiles, resolutionSourceFiles, compilationClasspath,
                    compilerModel, projectDependencies, Optional.empty());
        }

        /** Creates a normalized project model. */
        public ProjectSources {
            projectId = requireText(projectId, "projectId");
            entrySourceFiles = normalizedPaths(entrySourceFiles, "entrySourceFiles");
            resolutionSourceFiles = normalizedPaths(resolutionSourceFiles, "resolutionSourceFiles");
            compilationClasspath = normalizedPaths(compilationClasspath, "compilationClasspath");
            compilerModel = Objects.requireNonNull(compilerModel, "compilerModel");
            projectDependencies = List.copyOf(Objects.requireNonNull(projectDependencies, "projectDependencies"));
            moduleDescriptor = Objects.requireNonNull(moduleDescriptor, "moduleDescriptor")
                    .map(ApplicationSourceBoundary::normalize);
            moduleDescriptor.ifPresent(path -> {
                if (!path.getFileName().toString().equals("module-info.java")) {
                    throw new IllegalArgumentException("module descriptor must be named module-info.java");
                }
            });
            var resolutionKeys = new LinkedHashSet<>(resolutionSourceFiles);
            if (!resolutionKeys.containsAll(entrySourceFiles)) {
                throw new IllegalArgumentException("all entry sources must also be resolution sources");
            }
            if (entrySourceFiles.stream().anyMatch(path -> !isJava(path))
                    || resolutionSourceFiles.stream().anyMatch(path -> !isJava(path))) {
                throw new IllegalArgumentException("all project source files must end in .java");
            }
            if (projectDependencies.stream().anyMatch(projectId::equals)) {
                throw new IllegalArgumentException("a project cannot depend on itself: " + projectId);
            }
        }
    }

    /** How javac selects the source language and available JDK API. */
    public enum LanguageVersionMode {
        /** Use the documented API for one Java release. */
        RELEASE,
        /** Use the running JDK API with Maven source and target compatibility. */
        SOURCE_TARGET
    }

    /** Effective source compiler settings for one project. */
    public record CompilerModel(
            Charset charset,
            String release,
            List<String> compilerArguments,
            List<Path> modulePath,
            LanguageVersionMode languageVersionMode) {
        /** Compatibility constructor for a non-modular compiler model. */
        public CompilerModel(Charset charset, String release, List<String> compilerArguments) {
            this(charset, release, compilerArguments, List.of(), LanguageVersionMode.RELEASE);
        }

        /** Compatibility constructor for a modular release compiler model. */
        public CompilerModel(
                Charset charset,
                String release,
                List<String> compilerArguments,
                List<Path> modulePath) {
            this(charset, release, compilerArguments, modulePath, LanguageVersionMode.RELEASE);
        }

        /** Creates a defensive compiler model. */
        public CompilerModel {
            charset = Objects.requireNonNull(charset, "charset");
            release = requireText(release, "release");
            compilerArguments = List.copyOf(Objects.requireNonNull(compilerArguments, "compilerArguments"));
            modulePath = normalizedPaths(modulePath, "modulePath");
            languageVersionMode = Objects.requireNonNull(languageVersionMode, "languageVersionMode");
        }

        /** Creates a model that preserves Maven source and target compiler semantics. */
        public static CompilerModel sourceTarget(
                Charset charset,
                String version,
                List<String> compilerArguments,
                List<Path> modulePath) {
            return new CompilerModel(charset, version, compilerArguments, modulePath,
                    LanguageVersionMode.SOURCE_TARGET);
        }

        /** Returns javac language-selection options for this model. */
        public List<String> languageOptions() {
            return languageVersionMode == LanguageVersionMode.RELEASE
                    ? List.of("--release", release)
                    : List.of("-source", release, "-target", release);
        }

        /** Java 21 UTF-8 compiler defaults. */
        public static CompilerModel java21() {
            return new CompilerModel(StandardCharsets.UTF_8, "21", List.of());
        }
    }

    /** One source that can resolve calls but cannot create an entry graph. */
    public record ResolutionSource(Path path, SourceOrigin origin, ModuleOwnership ownership) {
        /** Compatibility constructor for a non-modular external source. */
        public ResolutionSource(Path path, SourceOrigin origin) {
            this(path, origin, ModuleOwnership.unnamed());
        }

        /** Creates a normalized resolution source. */
        public ResolutionSource {
            path = normalize(Objects.requireNonNull(path, "path"));
            if (!isJava(path)) throw new IllegalArgumentException("resolution source must end in .java");
            origin = Objects.requireNonNull(origin, "origin");
            ownership = Objects.requireNonNull(ownership, "ownership");
            if (ownership.sourceRoot().isPresent()
                    && !path.startsWith(ownership.sourceRoot().orElseThrow())) {
                throw new IllegalArgumentException("owned source is outside its source root: " + path);
            }
        }
    }

    /** Explicit JPMS ownership for one external source input. */
    public record ModuleOwnership(
            ModuleOwnershipKind kind,
            String moduleName,
            Optional<Path> descriptor,
            Optional<Path> binaryPath,
            Optional<Path> sourceRoot) {
        /** Creates validated module ownership. */
        public ModuleOwnership {
            kind = Objects.requireNonNull(kind, "kind");
            moduleName = Objects.requireNonNullElse(moduleName, "");
            descriptor = normalizedOptional(descriptor, "descriptor");
            binaryPath = normalizedOptional(binaryPath, "binaryPath");
            sourceRoot = normalizedOptional(sourceRoot, "sourceRoot");
            if (kind == ModuleOwnershipKind.UNNAMED) {
                if (!moduleName.isBlank() || descriptor.isPresent() || binaryPath.isPresent()
                        || sourceRoot.isPresent()) {
                    throw new IllegalArgumentException("unnamed ownership cannot contain module metadata");
                }
            } else {
                moduleName = requireText(moduleName, "moduleName");
                if (sourceRoot.isEmpty()) throw new IllegalArgumentException("owned module needs a source root");
                if (kind == ModuleOwnershipKind.NAMED && descriptor.isEmpty()) {
                    throw new IllegalArgumentException("named module ownership needs a descriptor");
                }
                if (kind == ModuleOwnershipKind.AUTOMATIC && binaryPath.isEmpty()) {
                    throw new IllegalArgumentException("automatic module ownership needs a binary path");
                }
            }
        }

        /** Creates ownership for a flat source input. */
        public static ModuleOwnership unnamed() {
            return new ModuleOwnership(ModuleOwnershipKind.UNNAMED, "",
                    Optional.empty(), Optional.empty(), Optional.empty());
        }

        /** Creates ownership for a named source module. */
        public static ModuleOwnership named(String moduleName, Path descriptor, Path sourceRoot) {
            return new ModuleOwnership(ModuleOwnershipKind.NAMED, moduleName,
                    Optional.of(descriptor), Optional.empty(), Optional.of(sourceRoot));
        }

        /** Creates ownership for sources paired with an automatic binary module. */
        public static ModuleOwnership automatic(String moduleName, Path binaryPath, Path sourceRoot) {
            return new ModuleOwnership(ModuleOwnershipKind.AUTOMATIC, moduleName,
                    Optional.empty(), Optional.of(binaryPath), Optional.of(sourceRoot));
        }

        private String fingerprint() {
            return kind + ":" + moduleName + ":" + descriptor.orElse(null) + ":"
                    + binaryPath.orElse(null) + ":" + sourceRoot.orElse(null);
        }
    }

    /** Supported external-source JPMS ownership kinds. */
    public enum ModuleOwnershipKind { UNNAMED, NAMED, AUTOMATIC }

    /** Developer-only provenance for one source boundary input. */
    public record SourceOrigin(OriginKind kind, String identity, String checksum) {
        /** Creates validated source origin data. */
        public SourceOrigin {
            kind = Objects.requireNonNull(kind, "kind");
            identity = requireText(identity, "identity");
            checksum = Objects.requireNonNullElse(checksum, "");
        }
    }

    /** Supported source origin classes. */
    public enum OriginKind { GIT, MAVEN_SOURCE, GENERATED, LOCAL }

    private static List<ResolutionSource> normalizedExternal(List<ResolutionSource> sources) {
        Objects.requireNonNull(sources, "externalResolutionSources");
        Map<Path, ResolutionSource> result = new LinkedHashMap<>();
        for (ResolutionSource source : sources) {
            ResolutionSource previous = result.putIfAbsent(source.path(), source);
            if (previous != null && !previous.equals(source)) {
                throw new IllegalArgumentException("conflicting origin or module ownership for source: "
                        + source.path());
            }
        }
        return result.values().stream().sorted(Comparator.comparing(item -> item.path().toString())).toList();
    }

    private static List<Path> normalizedPaths(List<Path> paths, String name) {
        Objects.requireNonNull(paths, name);
        var result = new ArrayList<Path>();
        paths.stream().map(ApplicationSourceBoundary::normalize).distinct()
                .sorted(Comparator.comparing(Path::toString)).forEach(result::add);
        return List.copyOf(result);
    }

    private static Optional<Path> normalizedOptional(Optional<Path> path, String name) {
        return Objects.requireNonNull(path, name).map(ApplicationSourceBoundary::normalize);
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static boolean isJava(Path path) {
        return path.toString().endsWith(".java");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static String sha256(byte[] value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
