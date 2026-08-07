package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Framework-neutral Java 21 source analyzer for {@code @FachTracing} entry points. */
public final class StaticDecisionAnalyzer {
    /** Analyzes the first graph entry from a project-aware application boundary. */
    public AnalysisManifest.AnalysisResult analyze(ApplicationSourceBoundary boundary) {
        return analyze(boundary, OpaqueLibraryBoundary.empty());
    }

    /** Analyzes the first graph entry with explicitly trusted technical dependency archives. */
    public AnalysisManifest.AnalysisResult analyze(
            ApplicationSourceBoundary boundary,
            OpaqueLibraryBoundary opaqueLibraries) {
        List<AnalysisManifest.AnalysisResult> results = analyzeAll(boundary, opaqueLibraries);
        if (results.isEmpty()) throw new IllegalArgumentException("No @FachTracing method found");
        return results.getFirst();
    }

    /** Analyzes every graph entry from a compatible project-aware application boundary. */
    public List<AnalysisManifest.AnalysisResult> analyzeAll(ApplicationSourceBoundary boundary) {
        return analyzeAll(boundary, OpaqueLibraryBoundary.empty());
    }

    /** Analyzes every graph entry with explicitly trusted technical dependency archives. */
    public List<AnalysisManifest.AnalysisResult> analyzeAll(
            ApplicationSourceBoundary boundary,
            OpaqueLibraryBoundary opaqueLibraries) {
        Objects.requireNonNull(boundary, "boundary");
        Objects.requireNonNull(opaqueLibraries, "opaqueLibraries");
        String searchedBoundary = "searched projects " + boundary.projects().stream()
                .map(ApplicationSourceBoundary.ProjectSources::projectId).sorted().toList()
                + ", external sources " + boundary.externalResolutionSources().stream()
                .map(source -> source.origin().kind() + ":" + source.origin().identity())
                .sorted().toList() + ", boundary " + boundary.fingerprint();
        var results = new ArrayList<AnalysisManifest.AnalysisResult>();
        for (ApplicationSourceBoundary.ProjectSources project : boundary.projects()) {
            if (project.entrySourceFiles().isEmpty()) continue;
            List<ApplicationSourceBoundary.ProjectSources> closure = projectClosure(boundary, project);
            boolean modular = project.moduleDescriptor().isPresent();
            List<ApplicationSourceBoundary.ProjectSources> analysisClosure = modular
                    ? closure.stream().filter(item -> item.moduleDescriptor().isPresent()).toList()
                    : closure;
            List<Path> sources = java.util.stream.Stream.concat(
                            analysisClosure.stream().flatMap(item -> item.resolutionSourceFiles().stream()),
                            boundary.externalResolutionSources().stream()
                                    .map(ApplicationSourceBoundary.ResolutionSource::path))
                    .distinct().sorted(Comparator.comparing(Path::toString)).toList();
            List<Path> classpath = closure.stream()
                    .flatMap(item -> item.compilationClasspath().stream()).distinct()
                    .sorted(Comparator.comparing(Path::toString)).toList();
            var request = new AnalysisRequest(
                    sources, classpath, project.compilerModel().charset(), project.entrySourceFiles());
            if (modular) {
                results.addAll(analyzeModular(request, analysisClosure, boundary, opaqueLibraries));
            } else {
                results.addAll(analyzeAll(request, project.compilerModel(), opaqueLibraries));
            }
        }
        return results.stream()
                .map(result -> withSearchedBoundary(result, searchedBoundary))
                .toList();
    }

    private static List<ApplicationSourceBoundary.ProjectSources> projectClosure(
            ApplicationSourceBoundary boundary,
            ApplicationSourceBoundary.ProjectSources root) {
        Map<String, ApplicationSourceBoundary.ProjectSources> projects = boundary.projects().stream()
                .collect(Collectors.toMap(ApplicationSourceBoundary.ProjectSources::projectId,
                        project -> project, (left, right) -> left, LinkedHashMap::new));
        var pending = new java.util.ArrayDeque<String>();
        var selected = new LinkedHashSet<String>();
        pending.add(root.projectId());
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (!selected.add(id)) continue;
            pending.addAll(projects.get(id).projectDependencies());
            projects.values().stream()
                    .filter(candidate -> candidate.projectDependencies().contains(id))
                    .map(ApplicationSourceBoundary.ProjectSources::projectId)
                    .forEach(pending::addLast);
        }
        return selected.stream().map(projects::get).toList();
    }

    private static AnalysisManifest.AnalysisResult withSearchedBoundary(
            AnalysisManifest.AnalysisResult result,
            String searchedBoundary) {
        List<AnalysisManifest.AnalysisDiagnostic> diagnostics = result.diagnostics().stream()
                .map(diagnostic -> {
                    if (!diagnostic.message().contains("unavailable")) return diagnostic;
                    return new AnalysisManifest.AnalysisDiagnostic(
                            diagnostic.severity(), diagnostic.source(), diagnostic.line(),
                            diagnostic.column(), diagnostic.constructKind(),
                            diagnostic.message() + "; " + searchedBoundary);
                }).toList();
        return new AnalysisManifest.AnalysisResult(result.graph(), result.manifest(), diagnostics);
    }

    /** Parses and attributes sources, then analyzes the first annotated method in source order. */
    public AnalysisManifest.AnalysisResult analyze(AnalysisRequest request) {
        return analyze(request, OpaqueLibraryBoundary.empty());
    }

    /** Analyzes the first annotated method with explicitly trusted technical dependency archives. */
    public AnalysisManifest.AnalysisResult analyze(
            AnalysisRequest request,
            OpaqueLibraryBoundary opaqueLibraries) {
        List<AnalysisManifest.AnalysisResult> results = analyzeAll(request, opaqueLibraries);
        if (results.isEmpty()) throw new IllegalArgumentException("No @FachTracing method found");
        return results.getFirst();
    }

    /** Parses and attributes sources, then analyzes every annotated method in deterministic source order. */
    public List<AnalysisManifest.AnalysisResult> analyzeAll(AnalysisRequest request) {
        return analyzeAll(request, OpaqueLibraryBoundary.empty());
    }

    /** Analyzes all annotated methods with explicitly trusted technical dependency archives. */
    public List<AnalysisManifest.AnalysisResult> analyzeAll(
            AnalysisRequest request,
            OpaqueLibraryBoundary opaqueLibraries) {
        return analyzeAll(request, ApplicationSourceBoundary.CompilerModel.java21(), opaqueLibraries);
    }

    private List<AnalysisManifest.AnalysisResult> analyzeAll(
            AnalysisRequest request,
            ApplicationSourceBoundary.CompilerModel compilerModel,
            OpaqueLibraryBoundary opaqueLibraries) {
        List<String> options = new ArrayList<>(List.of("-proc:none"));
        options.addAll(compilerModel.languageOptions());
        options.addAll(compilerModel.compilerArguments());
        if (!request.compilationClasspath().isEmpty()) {
            options.add("-classpath");
            options.add(joinPaths(request.compilationClasspath()));
        }
        return analyzeWithCompiler(request, request.sourceFiles(), options, opaqueLibraries);
    }

    private List<AnalysisManifest.AnalysisResult> analyzeModular(
            AnalysisRequest request,
            List<ApplicationSourceBoundary.ProjectSources> closure,
            ApplicationSourceBoundary boundary,
            OpaqueLibraryBoundary opaqueLibraries) {
        List<ApplicationSourceBoundary.ResolutionSource> external = boundary.externalResolutionSources();
        if (external.stream().anyMatch(source -> source.ownership().kind()
                == ApplicationSourceBoundary.ModuleOwnershipKind.UNNAMED)) {
            throw new IllegalArgumentException("JPMS graph extraction cannot assign external sources without "
                    + "explicit named or automatic module ownership");
        }
        if (closure.stream().anyMatch(project -> project.moduleDescriptor().isEmpty())) {
            throw new IllegalArgumentException("JPMS graph extraction requires a module descriptor for every "
                    + "connected source project");
        }
        var first = closure.getFirst().compilerModel();
        boolean compatible = closure.stream().map(ApplicationSourceBoundary.ProjectSources::compilerModel)
                .allMatch(model -> model.charset().equals(first.charset())
                        && model.release().equals(first.release())
                        && model.languageVersionMode() == first.languageVersionMode()
                        && model.compilerArguments().equals(first.compilerArguments()));
        if (!compatible) {
            throw new IllegalArgumentException("connected JPMS projects use incompatible compiler settings");
        }

        var sourceFiles = new LinkedHashSet<Path>(request.sourceFiles());
        closure.forEach(project -> sourceFiles.add(project.moduleDescriptor().orElseThrow()));
        external.stream().map(ApplicationSourceBoundary.ResolutionSource::ownership)
                .filter(ownership -> ownership.kind() == ApplicationSourceBoundary.ModuleOwnershipKind.NAMED)
                .map(ApplicationSourceBoundary.ModuleOwnership::descriptor)
                .map(Optional::orElseThrow).forEach(sourceFiles::add);
        Set<String> sourceModuleNames = java.util.stream.Stream.concat(closure.stream()
                .map(project -> moduleName(project.moduleDescriptor().orElseThrow()))
                , external.stream().map(ApplicationSourceBoundary.ResolutionSource::ownership)
                        .filter(ownership -> ownership.kind()
                                == ApplicationSourceBoundary.ModuleOwnershipKind.NAMED)
                        .map(ApplicationSourceBoundary.ModuleOwnership::moduleName))
                .collect(Collectors.toSet());
        var modulePath = java.util.stream.Stream.concat(
                        closure.stream().flatMap(project -> project.compilerModel().modulePath().stream()),
                        external.stream().map(ApplicationSourceBoundary.ResolutionSource::ownership)
                                .filter(ownership -> ownership.kind()
                                        == ApplicationSourceBoundary.ModuleOwnershipKind.AUTOMATIC)
                                .map(ApplicationSourceBoundary.ModuleOwnership::binaryPath)
                                .map(Optional::orElseThrow))
                .distinct().filter(path -> !containsModule(path, sourceModuleNames))
                .sorted(Comparator.comparing(Path::toString)).toList();
        var options = new ArrayList<>(List.of("-proc:none"));
        options.addAll(first.languageOptions());
        options.addAll(first.compilerArguments());
        if (!modulePath.isEmpty()) {
            options.add("--module-path");
            options.add(joinPaths(modulePath));
        }
        var modulePaths = new HashSet<>(modulePath);
        List<Path> classpath = request.compilationClasspath().stream()
                .filter(path -> !modulePaths.contains(path)).toList();
        if (!classpath.isEmpty()) {
            options.add("-classpath");
            options.add(joinPaths(classpath));
        }
        for (var project : closure) {
            options.add("--module-source-path");
            options.add(moduleName(project.moduleDescriptor().orElseThrow()) + "="
                    + joinPaths(sourceRoots(project)));
        }
        external.stream().map(ApplicationSourceBoundary.ResolutionSource::ownership).distinct()
                .filter(ownership -> ownership.kind() == ApplicationSourceBoundary.ModuleOwnershipKind.NAMED)
                .forEach(ownership -> {
                    String descriptorName = moduleName(ownership.descriptor().orElseThrow());
                    if (!descriptorName.equals(ownership.moduleName())) {
                        throw new IllegalArgumentException("external module ownership name "
                                + ownership.moduleName() + " does not match descriptor " + descriptorName);
                    }
                    options.add("--module-source-path");
                    options.add(ownership.moduleName() + "=" + ownership.sourceRoot().orElseThrow());
                });
        external.stream().map(ApplicationSourceBoundary.ResolutionSource::ownership).distinct()
                .filter(ownership -> ownership.kind() == ApplicationSourceBoundary.ModuleOwnershipKind.AUTOMATIC)
                .forEach(ownership -> {
                    options.add("--patch-module");
                    options.add(ownership.moduleName() + "=" + ownership.sourceRoot().orElseThrow());
                });
        Path output;
        try {
            output = Files.createTempDirectory("fachtracing-jpms-");
        } catch (IOException error) {
            throw new IllegalStateException("Could not create the JPMS analysis output", error);
        }
        options.add("-d");
        options.add(output.toString());
        try {
            return analyzeWithCompiler(request, List.copyOf(sourceFiles), options, opaqueLibraries);
        } finally {
            deleteTree(output);
        }
    }

    private static void deleteTree(Path root) {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException error) {
            throw new IllegalStateException("Could not remove the JPMS analysis output " + root, error);
        }
    }

    private List<AnalysisManifest.AnalysisResult> analyzeWithCompiler(
            AnalysisRequest request,
            List<Path> compilerSources,
            List<String> options,
            OpaqueLibraryBoundary opaqueLibraries) {
        Objects.requireNonNull(request, "request");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK is required for source analysis");

        var compilerDiagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(
                compilerDiagnostics, Locale.ROOT, request.charset())) {
            Iterable<? extends JavaFileObject> sources = files.getJavaFileObjectsFromPaths(compilerSources);
            JavacTask task = (JavacTask) compiler.getTask(
                    null, files, compilerDiagnostics, options, null, sources);
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            task.analyze();

            List<String> errors = compilerDiagnostics.getDiagnostics().stream()
                    .filter(diagnostic -> diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                    .map(Object::toString)
                    .toList();
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Effective compiler context failed during graph extraction: "
                        + String.join(" | ", errors));
            }

            Trees trees = Trees.instance(task);
            SourceIndex index = SourceIndex.create(units, trees, request.rootSourceFiles());
            List<MethodLocation> roots = index.annotatedMethods().stream()
                    .sorted(Comparator
                            .comparing((MethodLocation location) -> location.unit().getSourceFile().toUri().toString())
                            .thenComparingLong(MethodLocation::startPosition))
                    .toList();
            if (roots.isEmpty()) return List.of();

            Map<String, String> sourceFingerprints = fingerprints(request);
            var results = new ArrayList<AnalysisManifest.AnalysisResult>();
            for (MethodLocation root : roots) {
                String label = annotationLabel(root.method());
                if (label.isBlank()) label = words(root.method().getName().toString());
                Element rootElement = trees.getElement(root.path());
                String identity = rootElement == null ? root.method().toString() : rootElement.toString();
                String graphId = hash("graph", root.unit().getSourceFile().toUri(), identity);
                var builder = new DecisionGraphBuilder(graphId, label);
                var diagnostics = new ArrayList<AnalysisManifest.AnalysisDiagnostic>();
                var extractor = new Extractor(
                        trees, task.getTypes(), task.getElements(), index, builder, diagnostics,
                        request.compilationClasspath(), opaqueLibraries);
                String entry = extractor.addEntry(root);
                extractor.extract(root, entry, true, new HashSet<>());
                var built = builder.build(entry, sourceFingerprints, diagnostics);
                results.add(new AnalysisManifest.AnalysisResult(
                        built.graph(), built.manifest(), built.diagnostics()));
            }
            return List.copyOf(results);
        } catch (IOException error) {
            throw new IllegalStateException("Could not analyze Java sources", error);
        }
    }

    private static final Pattern MODULE_DECLARATION = Pattern.compile(
            "(?m)\\b(?:open\\s+)?module\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*\\{");
    private static final Pattern PACKAGE_DECLARATION = Pattern.compile(
            "(?m)\\bpackage\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");

    private static String moduleName(Path descriptor) {
        try {
            var matcher = MODULE_DECLARATION.matcher(Files.readString(descriptor));
            if (!matcher.find()) throw new IllegalArgumentException("module declaration is unavailable in " + descriptor);
            return matcher.group(1);
        } catch (IOException error) {
            throw new IllegalArgumentException("could not read module descriptor " + descriptor, error);
        }
    }

    private static boolean containsModule(Path path, Set<String> moduleNames) {
        if (!Files.exists(path)) return false;
        try {
            return java.lang.module.ModuleFinder.of(path).findAll().stream()
                    .map(reference -> reference.descriptor().name()).anyMatch(moduleNames::contains);
        } catch (java.lang.module.FindException ignored) {
            return false;
        }
    }

    private static List<Path> sourceRoots(ApplicationSourceBoundary.ProjectSources project) {
        var roots = new LinkedHashSet<Path>();
        roots.add(project.moduleDescriptor().orElseThrow().getParent());
        for (Path source : project.resolutionSourceFiles()) {
            try {
                var matcher = PACKAGE_DECLARATION.matcher(Files.readString(source));
                Path root = source.getParent();
                if (matcher.find()) {
                    for (String ignored : matcher.group(1).split("\\.")) {
                        if (root == null) break;
                        root = root.getParent();
                    }
                }
                if (root != null) roots.add(root.toAbsolutePath().normalize());
            } catch (IOException error) {
                throw new IllegalArgumentException("could not read module source " + source, error);
            }
        }
        return roots.stream().sorted(Comparator.comparing(Path::toString)).toList();
    }

    private static String joinPaths(List<Path> paths) {
        return paths.stream().map(Path::toString)
                .collect(Collectors.joining(System.getProperty("path.separator")));
    }

    private static final class Extractor {
        private final Trees trees;
        private final Types types;
        private final Elements elements;
        private final SourceIndex index;
        private final DecisionGraphBuilder builder;
        private final List<AnalysisManifest.AnalysisDiagnostic> diagnostics;
        private final List<Path> binaryClasspath;
        private final BinaryTypeOriginResolver binaryTypeOrigins;
        private final OpaqueLibraryBoundary opaqueLibraries;
        private final List<String> pendingFailureNodes = new ArrayList<>();
        private final Map<ExecutableElement, MutationSummary> mutationSummaries = new HashMap<>();
        private final Set<ExecutableElement> activeMutationSummaries = new HashSet<>();
        private String rootStop;

        private Extractor(
                Trees trees,
                Types types,
                Elements elements,
                SourceIndex index,
                DecisionGraphBuilder builder,
                List<AnalysisManifest.AnalysisDiagnostic> diagnostics,
                List<Path> binaryClasspath,
                OpaqueLibraryBoundary opaqueLibraries) {
            this.trees = trees;
            this.types = types;
            this.elements = elements;
            this.index = index;
            this.builder = builder;
            this.diagnostics = diagnostics;
            this.binaryClasspath = List.copyOf(binaryClasspath);
            this.binaryTypeOrigins = new BinaryTypeOriginResolver(binaryClasspath);
            this.opaqueLibraries = opaqueLibraries;
        }

        private String addEntry(MethodLocation method) {
            return builder.addNode(BusinessDecisionGraph.NodeKind.ENTRY, "Start", Map.of(),
                    mapping(method, method.method()), AnalysisManifest.ProbeKind.ENTRY,
                    ownerHint(method.path()), method.method().getName().toString(),
                    methodDescriptor(method));
        }

        private String stop(MethodLocation location, Tree terminal) {
            if (rootStop == null) {
                rootStop = builder.addNode(BusinessDecisionGraph.NodeKind.OUTCOME, "Stop", Map.of(),
                        mapping(location, terminal), null, "", "");
            }
            pendingFailureNodes.forEach(failure -> builder.addEdge(failure, rootStop, "fails"));
            pendingFailureNodes.clear();
            return rootStop;
        }

        private void connectFailureToStop(String failure, MethodLocation location, Tree terminal, boolean root) {
            if (rootStop == null && !root) {
                pendingFailureNodes.add(failure);
                return;
            }
            String terminalNode = stop(location, terminal);
            builder.addEdge(failure, terminalNode, "fails");
        }

        private DependencyGraphBuilder.CallEffects callEffects(
                MethodLocation caller, MethodInvocationTree call) {
            DependencyGraphBuilder.CallEffects callbackEffects = callbackEffects(caller, call);
            TreePath path = TreePath.getPath(caller.unit(), call);
            Element called = path == null ? null : trees.getElement(path);
            if (!(called instanceof ExecutableElement executable)) {
                return mergeEffects(callbackEffects,
                        new DependencyGraphBuilder.CallEffects(Set.of(), possibleReferenceRoots(caller, call)));
            }
            Set<String> platformWrites = platformMutationRoots(caller, call, executable);
            if (!platformWrites.isEmpty()) {
                return mergeEffects(callbackEffects,
                        new DependencyGraphBuilder.CallEffects(platformWrites, Set.of()));
            }
            MethodLocation callee = index.methods().get(executable);
            if (callee != null && callee.method().getBody() != null) {
                MutationSummary summary = mutationSummary(callee);
                Set<String> proven = mapSummaryRoots(
                        call, summary.receiverWrite(), summary.parameterWrites());
                Set<String> possible = mapSummaryRoots(
                        call, summary.receiverUnknown(), summary.parameterUnknown());
                return mergeEffects(callbackEffects,
                        new DependencyGraphBuilder.CallEffects(proven, possible));
            }
            if (isOpaqueLibraryReferenceOperation(executable)) {
                return mergeEffects(callbackEffects, new DependencyGraphBuilder.CallEffects(
                        opaqueLibraryReceiverRoots(caller, call), Set.of()));
            }
            if (isProvenReadOnlyLibraryOperation(executable)) return callbackEffects;
            return mergeEffects(callbackEffects,
                    new DependencyGraphBuilder.CallEffects(Set.of(), possibleReferenceRoots(caller, call)));
        }

        private DependencyGraphBuilder.CallEffects callbackEffects(
                MethodLocation caller, MethodInvocationTree call) {
            String method = call.getMethodSelect() instanceof MemberSelectTree member
                    ? member.getIdentifier().toString() : call.getMethodSelect().toString();
            if (!Set.of("forEach", "forEachOrdered", "map", "mapToInt", "mapToLong", "mapToDouble",
                    "flatMap", "filter", "anyMatch", "allMatch", "noneMatch", "removeIf", "replaceAll",
                    "compute", "computeIfAbsent", "computeIfPresent", "merge").contains(method)) {
                return DependencyGraphBuilder.CallEffects.none();
            }
            var proven = new LinkedHashSet<String>();
            var possible = new LinkedHashSet<String>();
            for (Tree argument : call.getArguments()) {
                if (argument instanceof LambdaExpressionTree lambda) {
                    new TreeScanner<Void, Void>() {
                        @Override public Void visitMethodInvocation(MethodInvocationTree nested, Void unused) {
                            DependencyGraphBuilder.CallEffects effects = callEffects(caller, nested);
                            proven.addAll(effects.provenWrites());
                            possible.addAll(effects.possibleWrites());
                            return super.visitMethodInvocation(nested, unused);
                        }
                    }.scan((Tree) lambda.getBody(), null);
                } else if (argument instanceof MemberReferenceTree reference) {
                    DependencyGraphBuilder.CallEffects effects = memberReferenceEffects(caller, call, reference);
                    proven.addAll(effects.provenWrites());
                    possible.addAll(effects.possibleWrites());
                }
            }
            return new DependencyGraphBuilder.CallEffects(proven, possible);
        }

        private DependencyGraphBuilder.CallEffects memberReferenceEffects(
                MethodLocation caller,
                MethodInvocationTree callback,
                MemberReferenceTree reference) {
            TreePath path = TreePath.getPath(caller.unit(), reference);
            Element called = path == null ? null : trees.getElement(path);
            Set<String> callbackInputs = callbackInputRoots(callback);
            if (!(called instanceof ExecutableElement executable)) {
                var possible = new LinkedHashSet<>(callbackInputs);
                possible.addAll(memberReferenceReceiverRoots(caller, reference));
                return new DependencyGraphBuilder.CallEffects(Set.of(), possible);
            }
            Set<String> platformWrites = platformMutationRoots(caller, reference, executable);
            if (!platformWrites.isEmpty()) {
                return new DependencyGraphBuilder.CallEffects(platformWrites, Set.of());
            }
            MethodLocation callee = index.methods().get(executable);
            if (callee != null && callee.method().getBody() != null) {
                MutationSummary summary = mutationSummary(callee);
                Set<String> receiverRoots = memberReferenceReceiverRoots(caller, reference);
                var proven = new LinkedHashSet<String>();
                var possible = new LinkedHashSet<String>();
                boolean unboundReceiver = receiverRoots.isEmpty()
                        && !executable.getModifiers().contains(Modifier.STATIC);
                if (summary.receiverWrite()) {
                    if (unboundReceiver) possible.addAll(callbackInputs);
                    else proven.addAll(receiverRoots);
                }
                if (summary.receiverUnknown()) {
                    if (unboundReceiver) possible.addAll(callbackInputs);
                    else possible.addAll(receiverRoots);
                }
                if (!summary.parameterWrites().isEmpty() || !summary.parameterUnknown().isEmpty()) {
                    possible.addAll(callbackInputs);
                }
                return new DependencyGraphBuilder.CallEffects(proven, possible);
            }
            if (isOpaqueLibraryReferenceOperation(executable)) {
                Set<String> receiverRoots = memberReferenceReceiverRoots(caller, reference);
                if (receiverRoots.isEmpty() && !executable.getModifiers().contains(Modifier.STATIC)) {
                    receiverRoots = callbackInputs;
                }
                return new DependencyGraphBuilder.CallEffects(receiverRoots, Set.of());
            }
            if (isProvenReadOnlyLibraryOperation(executable)) {
                return DependencyGraphBuilder.CallEffects.none();
            }
            var possible = new LinkedHashSet<>(callbackInputs);
            possible.addAll(memberReferenceReceiverRoots(caller, reference));
            return new DependencyGraphBuilder.CallEffects(Set.of(), possible);
        }

        private static DependencyGraphBuilder.CallEffects mergeEffects(
                DependencyGraphBuilder.CallEffects first,
                DependencyGraphBuilder.CallEffects second) {
            var proven = new LinkedHashSet<>(first.provenWrites());
            proven.addAll(second.provenWrites());
            var possible = new LinkedHashSet<>(first.possibleWrites());
            possible.addAll(second.possibleWrites());
            return new DependencyGraphBuilder.CallEffects(proven, possible);
        }

        private Set<String> platformMutationRoots(
                MethodLocation caller,
                MethodInvocationTree call,
                ExecutableElement executable) {
            String owner = ((TypeElement) executable.getEnclosingElement()).getQualifiedName().toString();
            String method = executable.getSimpleName().toString();
            if (Set.of("java.util.Collections", "java.util.Arrays").contains(owner)
                    && Set.of("sort", "parallelSort", "fill", "copy", "swap", "reverse", "rotate",
                            "shuffle", "replaceAll", "setAll", "parallelSetAll").contains(method)
                    && !call.getArguments().isEmpty()) {
                return stateRoots(call.getArguments().getFirst());
            }
            if (!isPlatformReceiverMutation(owner, method)
                    || !(call.getMethodSelect() instanceof MemberSelectTree member)) return Set.of();
            TreePath receiverPath = TreePath.getPath(caller.unit(), member.getExpression());
            Element receiver = receiverPath == null ? null : trees.getElement(receiverPath);
            if (receiver != null && Set.of(ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM,
                    ElementKind.RECORD, ElementKind.PACKAGE).contains(receiver.getKind())) return Set.of();
            return stateRoots(member.getExpression());
        }

        private Set<String> platformMutationRoots(
                MethodLocation caller,
                MemberReferenceTree reference,
                ExecutableElement executable) {
            String owner = ((TypeElement) executable.getEnclosingElement()).getQualifiedName().toString();
            String method = executable.getSimpleName().toString();
            if (!isPlatformReceiverMutation(owner, method)) return Set.of();
            return memberReferenceReceiverRoots(caller, reference);
        }

        private static boolean isPlatformReceiverMutation(String owner, String method) {
            boolean collection = owner.startsWith("java.util.") && Set.of(
                    "add", "addAll", "remove", "removeAll", "removeIf", "retainAll", "clear",
                    "set", "replace", "replaceAll", "sort", "put", "putAll", "putIfAbsent",
                    "compute", "computeIfAbsent", "computeIfPresent", "merge", "setValue",
                    "offer", "offerFirst", "offerLast", "addFirst", "addLast", "push", "pop",
                    "poll", "pollFirst", "pollLast", "removeFirst", "removeLast", "drainTo",
                    "next", "previous", "forEachRemaining")
                    .contains(method);
            boolean mutableText = (owner.equals("java.lang.StringBuilder")
                    || owner.equals("java.lang.StringBuffer"))
                    && Set.of("append", "appendCodePoint", "delete", "deleteCharAt", "insert",
                            "replace", "reverse", "setCharAt", "setLength").contains(method);
            boolean atomic = owner.startsWith("java.util.concurrent.atomic.")
                    && (method.startsWith("set") || method.startsWith("compareAndSet")
                    || method.startsWith("getAnd") || method.startsWith("increment")
                    || method.startsWith("decrement") || method.startsWith("addAnd"));
            return collection || mutableText || atomic;
        }

        private Set<String> memberReferenceReceiverRoots(
                MethodLocation caller, MemberReferenceTree reference) {
            Tree qualifier = reference.getQualifierExpression();
            TreePath path = TreePath.getPath(caller.unit(), qualifier);
            Element receiver = path == null ? null : trees.getElement(path);
            if (receiver != null && Set.of(ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM,
                    ElementKind.RECORD, ElementKind.PACKAGE).contains(receiver.getKind())) return Set.of();
            return stateRoots(qualifier);
        }

        private static Set<String> callbackInputRoots(MethodInvocationTree callback) {
            if (!(callback.getMethodSelect() instanceof MemberSelectTree member)) return Set.of();
            return stateRoots(callbackSource(member.getExpression()));
        }

        private static Tree callbackSource(Tree receiver) {
            if (receiver instanceof MethodInvocationTree pipeline
                    && pipeline.getMethodSelect() instanceof MemberSelectTree select
                    && Set.of("stream", "parallelStream").contains(select.getIdentifier().toString())) {
                return select.getExpression();
            }
            return receiver;
        }

        private Set<String> mapSummaryRoots(
                MethodInvocationTree call,
                boolean receiver,
                Set<Integer> parameters) {
            var roots = new LinkedHashSet<String>();
            if (receiver) {
                if (call.getMethodSelect() instanceof MemberSelectTree member) {
                    roots.addAll(stateRoots(member.getExpression()));
                } else {
                    roots.add("this");
                }
            }
            for (Integer parameter : parameters) {
                if (parameter >= 0 && parameter < call.getArguments().size()) {
                    roots.addAll(stateRoots(call.getArguments().get(parameter)));
                }
            }
            return Set.copyOf(roots);
        }

        private Set<String> possibleReferenceRoots(MethodLocation location, MethodInvocationTree call) {
            var roots = new LinkedHashSet<String>();
            if (call.getMethodSelect() instanceof MemberSelectTree member) {
                TreePath path = TreePath.getPath(location.unit(), member.getExpression());
                Element receiver = path == null ? null : trees.getElement(path);
                boolean typeReceiver = receiver != null && Set.of(ElementKind.CLASS, ElementKind.INTERFACE,
                        ElementKind.ENUM, ElementKind.RECORD, ElementKind.PACKAGE).contains(receiver.getKind());
                if (!typeReceiver && referenceValue(location, member.getExpression())) {
                    roots.addAll(stateRoots(member.getExpression()));
                }
            } else {
                roots.add("this");
            }
            for (Tree argument : call.getArguments()) {
                if (referenceValue(location, argument)) roots.addAll(stateRoots(argument));
            }
            return Set.copyOf(roots);
        }

        private Set<String> opaqueLibraryReceiverRoots(
                MethodLocation location, MethodInvocationTree call) {
            if (!(call.getMethodSelect() instanceof MemberSelectTree member)) return Set.of();
            TreePath path = TreePath.getPath(location.unit(), member.getExpression());
            Element receiver = path == null ? null : trees.getElement(path);
            if (receiver != null && Set.of(ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM,
                    ElementKind.RECORD, ElementKind.PACKAGE).contains(receiver.getKind())) return Set.of();
            return stateRoots(member.getExpression());
        }

        private boolean isOpaqueLibraryReferenceOperation(ExecutableElement executable) {
            TypeKind result = executable.getReturnType().getKind();
            if (!Set.of(TypeKind.ARRAY, TypeKind.DECLARED, TypeKind.TYPEVAR,
                    TypeKind.WILDCARD, TypeKind.INTERSECTION).contains(result)) return false;
            return hasOpaqueLibraryOwner(executable);
        }

        private boolean isOpaqueLibraryBooleanOperation(ExecutableElement executable) {
            return executable.getReturnType().getKind() == TypeKind.BOOLEAN
                    && hasOpaqueLibraryOwner(executable);
        }

        private boolean hasOpaqueLibraryOwner(ExecutableElement executable) {
            if (!(executable.getEnclosingElement() instanceof TypeElement owner)) return false;
            String binaryName = elements.getBinaryName(owner).toString();
            BinaryTypeOriginResolver.Resolution resolution = binaryTypeOrigins.resolve(binaryName);
            return resolution.origin() == BinaryTypeOriginResolver.Origin.ARCHIVE
                    && opaqueLibraries.contains(resolution.location().orElseThrow());
        }

        private boolean referenceValue(MethodLocation location, Tree tree) {
            TreePath path = TreePath.getPath(location.unit(), tree);
            TypeMirror type = path == null ? null : trees.getTypeMirror(path);
            return type == null || !type.getKind().isPrimitive();
        }

        private static Set<String> stateRoots(Tree tree) {
            return DependencyGraphBuilder.collectIdentifiers(tree);
        }

        private MutationSummary mutationSummary(MethodLocation location) {
            Element element = trees.getElement(location.path());
            if (!(element instanceof ExecutableElement executable)) return MutationSummary.unknown();
            MutationSummary cached = mutationSummaries.get(executable);
            if (cached != null) return cached;
            if (!activeMutationSummaries.add(executable)) return recursiveSummary(location);

            Map<String, Integer> parameters = new LinkedHashMap<>();
            for (int index = 0; index < location.method().getParameters().size(); index++) {
                parameters.put(location.method().getParameters().get(index).getName().toString(), index);
            }
            Set<String> fields = executable.getEnclosingElement().getEnclosedElements().stream()
                    .filter(member -> member.getKind() == ElementKind.FIELD)
                    .map(member -> member.getSimpleName().toString()).collect(Collectors.toSet());
            var receiverWrite = new boolean[1];
            var receiverUnknown = new boolean[1];
            var parameterWrites = new LinkedHashSet<Integer>();
            var parameterUnknown = new LinkedHashSet<Integer>();
            var aliases = new LocalAliasResolver();

            new TreePathScanner<Void, Void>() {
                @Override public Void visitVariable(VariableTree node, Void unused) {
                    if (node.getInitializer() != null) {
                        aliases.assign(node.getName().toString(), node.getInitializer());
                    }
                    return super.visitVariable(node, unused);
                }

                @Override public Void visitAssignment(AssignmentTree node, Void unused) {
                    if (node.getVariable() instanceof IdentifierTree identifier) {
                        if (fields.contains(identifier.getName().toString())) {
                            mark(node.getVariable(), true);
                        } else {
                            aliases.assign(identifier.getName().toString(), node.getExpression());
                        }
                    } else {
                        mark(node.getVariable(), true);
                    }
                    return super.visitAssignment(node, unused);
                }

                @Override public Void visitIf(IfTree node, Void unused) {
                    scan(node.getCondition(), unused);
                    LocalAliasResolver before = aliases.copy();
                    scan(node.getThenStatement(), unused);
                    LocalAliasResolver thenState = aliases.copy();
                    aliases.replaceWith(before);
                    if (node.getElseStatement() != null) scan(node.getElseStatement(), unused);
                    LocalAliasResolver elseState = aliases.copy();
                    aliases.replaceWith(LocalAliasResolver.merge(List.of(thenState, elseState)));
                    return null;
                }

                @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
                    mark(node.getVariable(), true);
                    return super.visitCompoundAssignment(node, unused);
                }

                @Override public Void visitUnary(UnaryTree node, Void unused) {
                    if (Set.of(Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT,
                            Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT).contains(node.getKind())) {
                        mark(node.getExpression(), true);
                    }
                    return super.visitUnary(node, unused);
                }

                @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                    DependencyGraphBuilder.CallEffects effects = callEffects(location, node);
                    effects.provenWrites().forEach(name -> markName(name, true));
                    effects.possibleWrites().forEach(name -> markName(name, false));
                    return super.visitMethodInvocation(node, unused);
                }

                private void mark(Tree target, boolean proven) {
                    Tree state = target instanceof ArrayAccessTree array ? array.getExpression() : target;
                    stateRoots(state).forEach(name -> markName(name, proven));
                }

                private void markName(String name, boolean proven) {
                    LocalAliasResolver.Resolution resolution = aliases.resolution(name);
                    if (proven) {
                        resolution.provedRoots().forEach(root -> markRoot(root, true));
                        resolution.possibleRoots().forEach(root -> markRoot(root, false));
                    } else {
                        resolution.allRoots().forEach(root -> markRoot(root, false));
                    }
                }

                private void markRoot(String root, boolean proven) {
                    Integer parameter = parameters.get(root);
                    if (parameter != null) {
                        (proven ? parameterWrites : parameterUnknown).add(parameter);
                    } else if (root.equals("this") || root.equals("super") || fields.contains(root)) {
                        if (proven) receiverWrite[0] = true;
                        else receiverUnknown[0] = true;
                    }
                }
            }.scan(new TreePath(location.path(), location.method().getBody()), null);

            activeMutationSummaries.remove(executable);
            var summary = new MutationSummary(receiverWrite[0], Set.copyOf(parameterWrites),
                    receiverUnknown[0], Set.copyOf(parameterUnknown));
            mutationSummaries.put(executable, summary);
            return summary;
        }

        private MutationSummary recursiveSummary(MethodLocation location) {
            Set<Integer> parameters = java.util.stream.IntStream.range(0, location.method().getParameters().size())
                    .filter(index -> {
                        TreePath path = TreePath.getPath(
                                location.unit(), location.method().getParameters().get(index));
                        TypeMirror type = path == null ? null : trees.getTypeMirror(path);
                        return type == null || !type.getKind().isPrimitive();
                    }).boxed().collect(Collectors.toSet());
            return new MutationSummary(false, Set.of(), true, parameters);
        }

        private Set<Tree> unknownResultEffects(
                MethodLocation location,
                DependencyGraphBuilder.MethodDependencies dependencies,
                Set<Tree> slice) {
            var dependentNames = new LinkedHashSet<String>();
            slice.forEach(tree -> dependentNames.addAll(DependencyGraphBuilder.collectIdentifiers(tree)));
            Element method = trees.getElement(location.path());
            Set<String> fields = method instanceof ExecutableElement executable
                    ? executable.getEnclosingElement().getEnclosedElements().stream()
                            .filter(member -> member.getKind() == ElementKind.FIELD)
                            .map(member -> member.getSimpleName().toString()).collect(Collectors.toSet())
                    : Set.of();
            boolean receiverStateReturned = dependentNames.stream().anyMatch(fields::contains);
            Set<Tree> result = Collections.newSetFromMap(new IdentityHashMap<>());
            dependencies.possibleEffectsByIdentifier().forEach((name, calls) -> {
                if (!dependentNames.contains(name) && !(name.equals("this") && receiverStateReturned)) return;
                calls.stream().filter(call -> !relevant(call, slice, dependencies)).forEach(result::add);
            });
            return Collections.unmodifiableSet(result);
        }

        private record MutationSummary(
                boolean receiverWrite,
                Set<Integer> parameterWrites,
                boolean receiverUnknown,
                Set<Integer> parameterUnknown) {
            private static MutationSummary unknown() {
                return new MutationSummary(false, Set.of(), true, Set.of());
            }
        }

        private Extraction extract(
                MethodLocation location,
                String predecessor,
                boolean root,
                Set<ExecutableElement> activeMethods) {
            return extract(location, predecessor, root, activeMethods, Set.of());
        }

        private Extraction extract(
                MethodLocation location,
                String predecessor,
                boolean root,
                Set<ExecutableElement> activeMethods,
                Set<String> effectRoots) {
            Element methodElement = trees.getElement(location.path());
            if (methodElement instanceof ExecutableElement executable && !activeMethods.add(executable)) {
                return new Extraction(predecessor, List.of(new Tail(predecessor, "result")));
            }
            var dependencies = new DependencyGraphBuilder().build(
                    location.method(), call -> callEffects(location, call));
            Set<Tree> slice = new BackwardDecisionSlicer().slice(dependencies, effectRoots);
            Set<Tree> unknownResultEffects = unknownResultEffects(location, dependencies, slice);
            var flow = new FlowScanner(location, root, activeMethods, dependencies, slice,
                    unknownResultEffects, predecessor);
            flow.scan(new TreePath(location.path(), location.method().getBody()), null);
            if (methodElement instanceof ExecutableElement executable) activeMethods.remove(executable);
            return new Extraction(flow.lastNode(), flow.exits());
        }

        private Set<String> calleeEffectRoots(
                MethodLocation callee,
                MethodInvocationTree call,
                DependencyGraphBuilder.MethodDependencies callerDependencies,
                Set<Tree> callerSlice) {
            Set<String> dependentNames = new LinkedHashSet<>();
            callerSlice.forEach(tree -> dependentNames.addAll(DependencyGraphBuilder.collectIdentifiers(tree)));
            Set<String> relevantCallerRoots = callerDependencies.effectsByIdentifier().entrySet().stream()
                    .filter(entry -> dependentNames.contains(entry.getKey()))
                    .filter(entry -> entry.getValue().stream().anyMatch(effect -> effect == call))
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            if (relevantCallerRoots.isEmpty()) return Set.of();

            MutationSummary summary = mutationSummary(callee);
            var roots = new LinkedHashSet<String>();
            Set<String> receiverRoots = call.getMethodSelect() instanceof MemberSelectTree member
                    ? stateRoots(member.getExpression()) : Set.of("this");
            if (summary.receiverWrite() && !Collections.disjoint(receiverRoots, relevantCallerRoots)) {
                roots.add("this");
                Element element = trees.getElement(callee.path());
                if (element instanceof ExecutableElement executable) {
                    executable.getEnclosingElement().getEnclosedElements().stream()
                            .filter(member -> member.getKind() == ElementKind.FIELD)
                            .map(member -> member.getSimpleName().toString()).forEach(roots::add);
                }
            }
            for (Integer index : summary.parameterWrites()) {
                if (index >= 0 && index < callee.method().getParameters().size()
                        && index < call.getArguments().size()
                        && !Collections.disjoint(
                                stateRoots(call.getArguments().get(index)), relevantCallerRoots)) {
                    roots.add(callee.method().getParameters().get(index).getName().toString());
                }
            }
            return Set.copyOf(roots);
        }

        @SuppressWarnings("unused")
        private Extraction extractLegacy(
                MethodLocation location,
                String predecessor,
                boolean root,
                Set<ExecutableElement> activeMethods) {
            Element methodElement = trees.getElement(location.path());
            if (methodElement instanceof ExecutableElement executable && !activeMethods.add(executable)) {
                return new Extraction(predecessor, List.of(new Tail(predecessor, "result")));
            }

            var dependencies = new DependencyGraphBuilder().build(
                    location.method(), call -> callEffects(location, call));
            Set<Tree> slice = new BackwardDecisionSlicer().slice(dependencies);
            var last = new String[] { predecessor };
            var predicateNodes = new LinkedHashMap<Tree, String>();
            var returnNodes = new LinkedHashMap<ReturnTree, String>();
            var exitNodes = new ArrayList<String>();
            var pendingResultTails = new ArrayList<String>();
            var terminalNodes = new HashSet<String>();

            new TreePathScanner<Void, Void>() {
                @Override public Void visitIf(IfTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.PREDICATE,
                                expression(node.getCondition()), node.getCondition(), AnalysisManifest.ProbeKind.PREDICATE);
                        connect(last[0], id, "next");
                        last[0] = id;
                        predicateNodes.put(node, id);
                    }
                    return super.visitIf(node, unused);
                }

                @Override public Void visitSwitch(SwitchTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.CHOICE,
                                "choose by " + expression(node.getExpression()), node.getExpression(),
                                AnalysisManifest.ProbeKind.PREDICATE);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitSwitch(node, unused);
                }

                @Override public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.CHOICE,
                                "choose by " + expression(node.getExpression()), node.getExpression(),
                                AnalysisManifest.ProbeKind.PREDICATE);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitSwitchExpression(node, unused);
                }

                @Override public Void visitVariable(VariableTree node, Void unused) {
                    if (node.getInitializer() != null && relevant(node.getInitializer(), slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "derive " + words(node.getName().toString()), node,
                                null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitVariable(node, unused);
                }

                @Override public Void visitAssignment(AssignmentTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "update decision value", node, null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitAssignment(node, unused);
                }

                @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "update decision value", node, null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitCompoundAssignment(node, unused);
                }

                @Override public Void visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.PREDICATE,
                                expression(node.getCondition()), node.getCondition(), AnalysisManifest.ProbeKind.PREDICATE);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitConditionalExpression(node, unused);
                }

                @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                    if (!relevant(node, slice, dependencies)) return super.visitMethodInvocation(node, unused);
                    Element called = trees.getElement(getCurrentPath());
                    if (!(called instanceof ExecutableElement executable)) {
                        return super.visitMethodInvocation(node, unused);
                    }
                    if (isSupportedLibraryOperation(executable)) {
                        return super.visitMethodInvocation(node, unused);
                    }
                    TypeElement owner = (TypeElement) executable.getEnclosingElement();
                    boolean dynamic = owner.getKind() == ElementKind.INTERFACE
                            || executable.getModifiers().contains(Modifier.ABSTRACT);
                    if (dynamic) {
                        String dispatch = add(BusinessDecisionGraph.NodeKind.DISPATCH,
                                "select applicable decision rule", node, AnalysisManifest.ProbeKind.DISPATCH);
                        connect(last[0], dispatch, "next");
                        last[0] = dispatch;
                        int candidate = 0;
                        for (TypeElement implementation : index.types()) {
                            if (implementation.getKind().isClass()
                                    && !implementation.getModifiers().contains(Modifier.ABSTRACT)
                                    && types.isSubtype(types.erasure(implementation.asType()), types.erasure(owner.asType()))) {
                                String alternative = builder.addNode(
                                        BusinessDecisionGraph.NodeKind.COMPUTATION,
                                        "possible decision rule " + (++candidate),
                                        Map.of("candidate", Integer.toString(candidate)),
                                        null, null, "", "");
                                String candidateEdge = builder.addEdge(
                                        dispatch, alternative, "candidate " + candidate);
                                MethodLocation implementationMethod = implementationOf(executable, implementation);
                                if (implementationMethod != null) {
                                    builder.addDispatchTarget(dispatch, candidateEdge, implementation.toString(),
                                            implementationMethod.method().getName().toString(),
                                            methodDescriptor(implementationMethod));
                                    pendingResultTails.addAll(extract(
                                            implementationMethod, alternative, false, activeMethods).exits().stream()
                                            .map(Tail::nodeId).toList());
                                } else {
                                    pendingResultTails.add(alternative);
                                }
                            }
                        }
                        if (candidate == 0) {
                            addCoverageGap(node, "decision-rule implementations are unavailable");
                        }
                    } else {
                        MethodLocation callee = index.methods().get(executable);
                        if (callee != null && !activeMethods.contains(executable)) {
                            String call = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                    "evaluate " + words(executable.getSimpleName().toString()), node, null);
                            connect(last[0], call, "next");
                            Extraction linked = extract(callee, call, false, activeMethods);
                            pendingResultTails.addAll(linked.exits().stream().map(Tail::nodeId).toList());
                            last[0] = call;
                        } else if (callee == null) {
                            addCoverageGap(node, "called decision logic is unavailable");
                        }
                    }
                    return super.visitMethodInvocation(node, unused);
                }

                @Override public Void visitReturn(ReturnTree node, Void unused) {
                    if (!slice.contains(node)) return super.visitReturn(node, unused);
                    if (node.getExpression() != null) {
                        scan(node.getExpression(), unused);
                        if (isPredicateExpression(node.getExpression())) {
                            String predicate = add(BusinessDecisionGraph.NodeKind.PREDICATE,
                                    expression(node.getExpression()), node.getExpression(),
                                    AnalysisManifest.ProbeKind.PREDICATE);
                            connect(last[0], predicate, "next");
                            last[0] = predicate;
                        }
                    }
                    var kind = root ? BusinessDecisionGraph.NodeKind.OUTCOME : BusinessDecisionGraph.NodeKind.COMPUTATION;
                    String label = root ? "final decision" : "derived decision value";
                    String id = add(kind, label, node, root ? AnalysisManifest.ProbeKind.OUTCOME : null);
                    if (pendingResultTails.isEmpty()) connect(last[0], id, "next");
                    for (String resultTail : pendingResultTails) {
                        connect(resultTail, id, "result");
                    }
                    pendingResultTails.clear();
                    last[0] = id;
                    terminalNodes.add(id);
                    exitNodes.add(id);
                    returnNodes.put(node, id);
                    return null;
                }

                @Override public Void visitForLoop(ForLoopTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String label = node.getCondition() == null ? "repeat entries"
                                : "repeat while " + expression(node.getCondition());
                        String id = add(BusinessDecisionGraph.NodeKind.CHOICE, label, node, null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitForLoop(node, unused);
                }

                @Override public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.CHOICE,
                                "for each " + words(node.getVariable().getName().toString()) + " in "
                                        + expression(node.getExpression()), node, null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitEnhancedForLoop(node, unused);
                }

                @Override public Void visitWhileLoop(WhileLoopTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.CHOICE,
                                "repeat while " + expression(node.getCondition()), node, null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitWhileLoop(node, unused);
                }

                @Override public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
                    if (relevant(node, slice, dependencies)) {
                        String id = add(BusinessDecisionGraph.NodeKind.CHOICE,
                                "repeat while " + expression(node.getCondition()), node, null);
                        connect(last[0], id, "next");
                        last[0] = id;
                    }
                    return super.visitDoWhileLoop(node, unused);
                }

                @Override public Void visitTry(TryTree node, Void unused) {
                    gapIfRelevant(node, "try statement");
                    return super.visitTry(node, unused);
                }

                @Override public Void visitSynchronized(SynchronizedTree node, Void unused) {
                    gapIfRelevant(node, "synchronized statement");
                    return super.visitSynchronized(node, unused);
                }

                private void gapIfRelevant(Tree node, String description) {
                    if (!relevant(node, slice, dependencies)) return;
                    addCoverageGap(node, description);
                }

                private void addCoverageGap(Tree node, String description) {
                    String id = add(BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                            "analysis incomplete: " + description, node, null);
                    connect(last[0], id, "unresolved");
                    last[0] = id;
                    builder.addGap(id, description + " affects the decision");
                    var source = mapping(location, node);
                    diagnostics.add(new AnalysisManifest.AnalysisDiagnostic(
                            AnalysisManifest.Severity.WARNING, source.source(), source.line(), source.column(),
                            source.treeKind(), description + " is outside the supported generic analysis subset"));
                }

                private String add(
                        BusinessDecisionGraph.NodeKind kind,
                        String label,
                        Tree tree,
                        AnalysisManifest.ProbeKind probe) {
                    return builder.addNode(kind, label, Map.of(), mapping(location, tree), probe,
                            ownerHint(location.path()), location.method().getName().toString(),
                            methodDescriptor(location));
                }

                private void connect(String from, String to, String outcome) {
                    if (!from.equals(to) && !terminalNodes.contains(from)) builder.addEdge(from, to, outcome);
                }
            }.scan(new TreePath(location.path(), location.method().getBody()), null);

            // Add explicit branch edges in addition to execution-order edges.
            for (Map.Entry<Tree, String> predicate : predicateNodes.entrySet()) {
                IfTree decision = (IfTree) predicate.getKey();
                ReturnTree trueReturn = firstReturn(decision.getThenStatement(), returnNodes.keySet());
                ReturnTree falseReturn = firstReturn(decision.getElseStatement(), returnNodes.keySet());
                if (falseReturn == null) {
                    falseReturn = fallthroughReturn(decision, dependencies, returnNodes.keySet());
                }
                if (trueReturn != null) builder.addEdge(predicate.getValue(), returnNodes.get(trueReturn), "true");
                if (falseReturn != null) builder.addEdge(predicate.getValue(), returnNodes.get(falseReturn), "false");
            }

            if (methodElement instanceof ExecutableElement executable) activeMethods.remove(executable);
            return new Extraction(last[0], exitNodes.stream().map(node -> new Tail(node, "result")).toList());
        }

        private final class FlowScanner extends TreePathScanner<Void, Void> {
            private final MethodLocation location;
            private final boolean root;
            private final Set<ExecutableElement> activeMethods;
            private final DependencyGraphBuilder.MethodDependencies dependencies;
            private final Set<Tree> slice;
            private final Set<Tree> unknownResultEffects;
            private final List<Tail> exitNodes = new ArrayList<>();
            private List<Tail> frontier;
            private String lastNode;
            private int catchingDepth;
            private List<Tail> caughtTails = new ArrayList<>();
            private int deferredReturnDepth;
            private List<DeferredReturn> deferredReturns = new ArrayList<>();
            private final Set<VariableTree> transparentLoopAliases =
                    Collections.newSetFromMap(new IdentityHashMap<>());
            private final Map<String, String> validationHelperRoles = new LinkedHashMap<>();

            private FlowScanner(
                    MethodLocation location,
                    boolean root,
                    Set<ExecutableElement> activeMethods,
                    DependencyGraphBuilder.MethodDependencies dependencies,
                    Set<Tree> slice,
                    Set<Tree> unknownResultEffects,
                    String predecessor) {
                this.location = location;
                this.root = root;
                this.activeMethods = activeMethods;
                this.dependencies = dependencies;
                this.slice = slice;
                this.unknownResultEffects = unknownResultEffects;
                this.frontier = List.of(new Tail(predecessor, "next"));
                this.lastNode = predecessor;
            }

            private String lastNode() { return lastNode; }

            private List<Tail> exits() {
                if (!exitNodes.isEmpty()) return List.copyOf(exitNodes);
                return List.copyOf(frontier);
            }

            @Override public Void visitIf(IfTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitIf(node, unused);
                if (!(unwrapParentheses(node.getCondition()) instanceof ConditionalExpressionTree)) {
                    scan(node.getCondition(), unused);
                }
                PredicatePlan predicate = addPredicatePlan(node.getCondition());
                enter(predicate);

                frontier = predicate.trueTails();
                scan(node.getThenStatement(), unused);
                List<Tail> trueTails = List.copyOf(frontier);

                frontier = predicate.falseTails();
                if (node.getElseStatement() != null) scan(node.getElseStatement(), unused);
                List<Tail> falseTails = List.copyOf(frontier);

                var merged = new ArrayList<Tail>(trueTails.size() + falseTails.size());
                merged.addAll(trueTails);
                merged.addAll(falseTails);
                frontier = List.copyOf(merged);
                return null;
            }

            @Override public Void visitSwitch(SwitchTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitSwitch(node, unused);
                scan(node.getExpression(), unused);
                String choice = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        "choose by " + expression(node.getExpression()), node.getExpression(), null);
                advance(choice);
                var merged = new ArrayList<Tail>();
                Map<Long, Long> lineCounts = controlLineCounts(node.getCases());
                Tree ambiguous = null;
                List<Tail> rejectedGuardTails = List.of();
                int caseIndex = 0;
                for (CaseTree branch : node.getCases()) {
                    String outcome = caseLabel(branch, ++caseIndex);
                    var inputs = new ArrayList<Tail>();
                    inputs.add(new Tail(choice, outcome));
                    inputs.addAll(rejectedGuardTails);
                    frontier = List.copyOf(inputs);
                    if (branch.getGuard() != null) {
                        scan(branch.getGuard(), unused);
                        PredicatePlan guard = addPredicatePlan(branch.getGuard(), isPatternCase(branch)
                                ? AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED
                                : AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
                        enter(guard);
                        frontier = guard.trueTails();
                        rejectedGuardTails = guard.falseTails();
                    } else {
                        rejectedGuardTails = List.of();
                    }
                    if (branch.getStatements() != null) scan(branch.getStatements(), unused);
                    else scan(branch.getBody(), unused);
                    Tree target = controlTarget(branch);
                    long line = target == null ? -1 : mapping(location, target).line();
                    if (line > 0 && lineCounts.getOrDefault(line, 0L) == 1) {
                        builder.addControlTarget(choice, outcome, ownerHint(location.path()),
                                runtimeMemberHint(branch), methodDescriptor(location), line,
                                controlPoint(branch));
                    } else if (ambiguous == null) {
                        ambiguous = branch;
                    }
                    merged.addAll(frontier);
                }
                frontier = List.copyOf(merged);
                if (ambiguous != null) addCoverageGap(
                        ambiguous, "switch case path has no unique executable source line");
                return null;
            }

            @Override public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitSwitchExpression(node, unused);
                scan(node.getExpression(), unused);
                String choice = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        "choose by " + expression(node.getExpression()), node.getExpression(), null);
                advance(choice);
                var merged = new ArrayList<Tail>();
                Map<Long, Long> lineCounts = controlLineCounts(node.getCases());
                Tree ambiguous = null;
                List<Tail> rejectedGuardTails = List.of();
                int caseIndex = 0;
                for (CaseTree branch : node.getCases()) {
                    String outcome = caseLabel(branch, ++caseIndex);
                    var inputs = new ArrayList<Tail>();
                    inputs.add(new Tail(choice, outcome));
                    inputs.addAll(rejectedGuardTails);
                    frontier = List.copyOf(inputs);
                    if (branch.getGuard() != null) {
                        scan(branch.getGuard(), unused);
                        PredicatePlan guard = addPredicatePlan(branch.getGuard(), isPatternCase(branch)
                                ? AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED
                                : AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
                        enter(guard);
                        frontier = guard.trueTails();
                        rejectedGuardTails = guard.falseTails();
                    } else {
                        rejectedGuardTails = List.of();
                    }
                    if (branch.getStatements() != null) scan(branch.getStatements(), unused);
                    else scan(branch.getBody(), unused);
                    Tree target = controlTarget(branch);
                    long line = target == null ? -1 : mapping(location, target).line();
                    if (line > 0 && lineCounts.getOrDefault(line, 0L) == 1) {
                        builder.addControlTarget(choice, outcome, ownerHint(location.path()),
                                runtimeMemberHint(branch), methodDescriptor(location), line,
                                controlPoint(branch));
                    } else if (ambiguous == null) {
                        ambiguous = branch;
                    }
                    merged.addAll(frontier);
                }
                frontier = List.copyOf(merged);
                if (ambiguous != null) addCoverageGap(
                        ambiguous, "switch case path has no unique executable source line");
                return null;
            }

            @Override public Void visitVariable(VariableTree node, Void unused) {
                if (transparentLoopAliases.contains(node)) return null;
                if (node.getInitializer() == null || !relevant(node.getInitializer(), slice, dependencies)) {
                    return super.visitVariable(node, unused);
                }
                if (validationHelperVariable(node)) {
                    String subject = words(node.getName().toString().replaceFirst("(?i)Validator$", ""));
                    validationHelperRoles.put(node.getName().toString(),
                            subject.isBlank() ? "validation" : subject);
                    String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                            subject.isBlank() ? "validation" : subject, node, null);
                    advance(id);
                    return null;
                }
                scan(node.getInitializer(), unused);
                String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION, derivationLabel(node), node, null);
                advance(id);
                return null;
            }

            private boolean validationHelperVariable(VariableTree variable) {
                if (variable.getInitializer() == null
                        || variable.getInitializer().getKind() != Tree.Kind.NEW_CLASS) return false;
                String name = variable.getName().toString();
                int[] uses = { 0 };
                int[] validationReceivers = { 0 };
                new TreeScanner<Void, Void>() {
                    @Override public Void visitIdentifier(IdentifierTree identifier, Void unused) {
                        if (identifier.getName().contentEquals(name)) uses[0]++;
                        return super.visitIdentifier(identifier, unused);
                    }

                    @Override public Void visitMethodInvocation(MethodInvocationTree call, Void unused) {
                        if (call.getMethodSelect() instanceof MemberSelectTree member
                                && member.getIdentifier().contentEquals("validate")
                                && member.getExpression() instanceof IdentifierTree identifier
                                && identifier.getName().contentEquals(name)) {
                            validationReceivers[0]++;
                        }
                        return super.visitMethodInvocation(call, unused);
                    }
                }.scan(location.method().getBody(), null);
                return uses[0] > 0 && uses[0] == validationReceivers[0];
            }

            @Override public Void visitAssignment(AssignmentTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitAssignment(node, unused);
                scan(node.getExpression(), unused);
                String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "set " + expression(node.getVariable()) + " to " + expression(node.getExpression()), node, null);
                advance(id);
                return null;
            }

            @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitCompoundAssignment(node, unused);
                scan(node.getExpression(), unused);
                String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "update " + expression(node.getVariable()) + " using " + expression(node.getExpression()), node, null);
                advance(id);
                return null;
            }

            @Override public Void visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitConditionalExpression(node, unused);
                scan(node.getCondition(), unused);
                PredicatePlan predicate = addPredicatePlan(node.getCondition());
                enter(predicate);
                frontier = predicate.trueTails();
                scan(node.getTrueExpression(), unused);
                String trueValue = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "use " + expression(node.getTrueExpression()), node.getTrueExpression(), null);
                advance(trueValue);
                List<Tail> trueTails = List.copyOf(frontier);
                frontier = predicate.falseTails();
                scan(node.getFalseExpression(), unused);
                String falseValue = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "use " + expression(node.getFalseExpression()), node.getFalseExpression(), null);
                advance(falseValue);
                var merged = new ArrayList<Tail>(trueTails);
                merged.addAll(frontier);
                frontier = List.copyOf(merged);
                return null;
            }

            @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                if (unknownResultEffects.contains(node)) {
                    addCoverageGap(node, "a possible side effect on the returned decision cannot be reconstructed");
                    return null;
                }
                if (!relevant(node, slice, dependencies)) return super.visitMethodInvocation(node, unused);
                Element called = trees.getElement(getCurrentPath());
                if (!(called instanceof ExecutableElement executable)) return super.visitMethodInvocation(node, unused);
                ExecutableElement reflected = reflectedContract(node, executable);
                if (reflected != null) {
                    scan(node.getArguments(), unused);
                    scanDynamicInvocation(node, reflected, (TypeElement) reflected.getEnclosingElement(),
                            reflected.getEnclosingElement().asType());
                    return null;
                }
                if (isReflectionInvoke(executable)) {
                    addCoverageGap(node, "reflected decision target cannot be reconstructed from constants");
                    return null;
                }
                Set<String> platformWrites = platformMutationRoots(location, node, executable);
                if (!platformWrites.isEmpty()) {
                    scan(node.getArguments(), unused);
                    String mutation = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                            invocationLabel(node, executable), node, null);
                    advance(mutation);
                    return null;
                }
                if (isSupportedLibraryOperation(executable)) return super.visitMethodInvocation(node, unused);
                if (isOpaqueLibraryReferenceOperation(executable)
                        || (isOpaqueLibraryBooleanOperation(executable) && isSourceControlPredicate(node))) {
                    return super.visitMethodInvocation(node, unused);
                }

                scan(node.getMethodSelect(), unused);
                scan(node.getArguments(), unused);
                TypeElement owner = (TypeElement) executable.getEnclosingElement();
                TypeMirror receiverType = dispatchReceiverType(node);
                boolean dynamic = executable.getModifiers().contains(Modifier.ABSTRACT)
                        || hasDecisionBearingOverrides(executable, owner);
                if (dynamic) {
                    if (isDecisionFreeValueAccess(executable, owner)) return null;
                    scanDynamicInvocation(node, executable, owner, receiverType);
                } else {
                    MethodLocation callee = index.methods().get(executable);
                    if (callee != null && isDecisionFreeProjection(callee)) return null;
                    if (callee != null && !activeMethods.contains(executable)) {
                        String call = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                invocationLabel(node, executable), node, null);
                        advance(call);
                        Extraction linked = extract(callee, call, false, activeMethods,
                                calleeEffectRoots(callee, node, dependencies, slice));
                        frontier = linked.exits();
                    } else if (callee == null) {
                        BytecodeDecisionAnalyzer.Result fallback = new BytecodeDecisionAnalyzer().analyze(
                                owner.getQualifiedName().toString(), executable.getSimpleName().toString(),
                                methodDescriptor(executable), binaryClasspath);
                        if (fallback instanceof BytecodeDecisionAnalyzer.Fragment fragment) {
                            String call = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                    "evaluate binary decision rule", node, null);
                            advance(call);
                            String predicate = builder.addNode(
                                    BusinessDecisionGraph.NodeKind.PREDICATE,
                                    fragment.predicateLabel(), Map.of(), mapping(location, node),
                                    null, "", "");
                            builder.addProbe(predicate, AnalysisManifest.ProbeKind.PREDICATE,
                                    fragment.ownerName().replace('/', '.'), fragment.methodName(),
                                    fragment.descriptor(), null);
                            advance(predicate);
                            builder.setBranchCompletions(predicate,
                                    List.of(AnalysisManifest.BranchCompletion.BOTH_OUTCOMES));
                            frontier = List.of(new Tail(predicate, "true"), new Tail(predicate, "false"));
                        } else {
                            addCoverageGap(node, ((BytecodeDecisionAnalyzer.Gap) fallback).reason());
                        }
                    }
                }
                return null;
            }

            private boolean isSourceControlPredicate(MethodInvocationTree invocation) {
                Tree current = invocation;
                Tree parent;
                while ((parent = dependencies.parents().get(current)) != null) {
                    Tree condition = switch (parent) {
                        case IfTree decision -> decision.getCondition();
                        case WhileLoopTree loop -> loop.getCondition();
                        case DoWhileLoopTree loop -> loop.getCondition();
                        case ForLoopTree loop -> loop.getCondition();
                        case ConditionalExpressionTree decision -> decision.getCondition();
                        default -> null;
                    };
                    if (condition != null && containsTree(condition, invocation)) return true;
                    current = parent;
                }
                return false;
            }

            private boolean containsTree(Tree root, Tree target) {
                boolean[] found = { false };
                new TreeScanner<Void, Void>() {
                    @Override public Void scan(Tree tree, Void unused) {
                        if (tree == target) {
                            found[0] = true;
                            return null;
                        }
                        return found[0] ? null : super.scan(tree, unused);
                    }
                }.scan(root, null);
                return found[0];
            }

            private void scanDynamicInvocation(
                    MethodInvocationTree node,
                    ExecutableElement contract,
                    TypeElement owner,
                    TypeMirror receiverType) {
                String dispatch = add(BusinessDecisionGraph.NodeKind.DISPATCH,
                        "select applicable decision rule", node, AnalysisManifest.ProbeKind.DISPATCH);
                advance(dispatch);
                var resultTails = new ArrayList<Tail>();
                int candidate = 0;
                for (TypeElement implementation : index.types()) {
                    if (!implementation.getKind().isClass()
                            || implementation.getModifiers().contains(Modifier.ABSTRACT)
                            || !isCompatibleDispatchTarget(implementation, owner, receiverType)) continue;
                    String alternative = builder.addNode(
                            BusinessDecisionGraph.NodeKind.COMPUTATION,
                            businessRuleLabel(implementation),
                            Map.of(), null, null, "", "");
                    candidate++;
                    String candidateEdge = builder.addEdge(dispatch, alternative, "selected rule");
                    MethodLocation implementationMethod = implementationOf(contract, implementation);
                    if (implementationMethod == null) {
                        resultTails.add(new Tail(alternative, "result"));
                        continue;
                    }
                    builder.addDispatchTarget(dispatch, candidateEdge, implementation.toString(),
                            implementationMethod.method().getName().toString(),
                            methodDescriptor(implementationMethod));
                    Extraction linked = extract(implementationMethod, alternative, false, activeMethods);
                    resultTails.addAll(linked.exits());
                }
                if (candidate == 0) addCoverageGap(node, "decision-rule implementations are unavailable");
                else frontier = List.copyOf(resultTails);
            }

            private ExecutableElement reflectedContract(
                    MethodInvocationTree invocation, ExecutableElement called) {
                if (!isReflectionInvoke(called)
                        || !(invocation.getMethodSelect() instanceof MemberSelectTree invokeSelect)
                        || !(invokeSelect.getExpression() instanceof IdentifierTree methodVariable)) return null;
                Tree definition = dependencies.definitions().get(methodVariable.getName().toString());
                if (!(definition instanceof MethodInvocationTree lookup)
                        || lookup.getArguments().isEmpty()
                        || !(lookup.getArguments().getFirst() instanceof LiteralTree methodName)
                        || !(methodName.getValue() instanceof String name)
                        || !(lookup.getMethodSelect() instanceof MemberSelectTree lookupSelect)
                        || !(lookupSelect.getExpression() instanceof MemberSelectTree classLiteral)
                        || !classLiteral.getIdentifier().contentEquals("class")) return null;
                TreePath typePath = TreePath.getPath(location.unit(), classLiteral.getExpression());
                Element type = typePath == null ? null : trees.getElement(typePath);
                if (!(type instanceof TypeElement contractOwner)) return null;
                int parameters = lookup.getArguments().size() - 1;
                List<ExecutableElement> matches = elements.getAllMembers(contractOwner).stream()
                        .filter(member -> member instanceof ExecutableElement)
                        .map(member -> (ExecutableElement) member)
                        .filter(method -> method.getSimpleName().contentEquals(name))
                        .filter(method -> method.getParameters().size() == parameters)
                        .toList();
                return matches.size() == 1 ? matches.getFirst() : null;
            }

            private boolean isReflectionInvoke(ExecutableElement executable) {
                return executable.getSimpleName().contentEquals("invoke")
                        && executable.getEnclosingElement() instanceof TypeElement owner
                        && owner.getQualifiedName().contentEquals("java.lang.reflect.Method");
            }

            @Override public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitLambdaExpression(node, unused);
                Tree parent = dependencies.parents().get(node);
                if (!(node.getBody() instanceof Tree body)) {
                    return super.visitLambdaExpression(node, unused);
                }
                boolean predicateCallback = parent instanceof MethodInvocationTree invocation
                        && isPredicateOperation(invocation);
                if (!predicateCallback && !isPredicateExpression(body)) return super.visitLambdaExpression(node, unused);
                scan(body, unused);
                if (body.getKind() == Tree.Kind.BLOCK || !isPredicateExpression(body)) return null;
                PredicatePlan predicate = addPredicatePlan(body);
                enter(predicate);
                var tails = new ArrayList<Tail>(predicate.trueTails());
                tails.addAll(predicate.falseTails());
                frontier = List.copyOf(tails);
                return null;
            }

            @Override public Void visitMemberReference(MemberReferenceTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitMemberReference(node, unused);
                Element called = trees.getElement(getCurrentPath());
                if (!(called instanceof ExecutableElement executable)) {
                    addCoverageGap(node, "method-reference target is unavailable");
                    return null;
                }
                TypeElement owner = (TypeElement) executable.getEnclosingElement();
                if (isDecisionFreeValueAccess(executable, owner)) return null;
                Set<String> platformWrites = platformMutationRoots(location, node, executable);
                if (!platformWrites.isEmpty()) {
                    String mutation = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                            memberReferenceMutationLabel(node, executable), node, null);
                    advance(mutation);
                    return null;
                }
                if (isOpaqueLibraryReferenceOperation(executable)) return null;
                MethodLocation callee = index.methods().get(executable);
                if (callee == null) {
                    if (!isSupportedLibraryOperation(executable)) {
                        addCoverageGap(node, "method-reference decision logic is unavailable");
                    }
                    return null;
                }
                if (callee.method().getBody() == null) {
                    addCoverageGap(node, "method-reference decision logic has no source body");
                    return null;
                }
                if (activeMethods.contains(executable)) return null;
                String call = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "evaluate " + words(executable.getSimpleName().toString()), node, null);
                advance(call);
                Extraction linked = extract(callee, call, false, activeMethods);
                frontier = linked.exits();
                return null;
            }

            private String memberReferenceMutationLabel(
                    MemberReferenceTree reference, ExecutableElement executable) {
                Tree parent = dependencies.parents().get(reference);
                String target = expression(reference.getQualifierExpression());
                if (parent instanceof MethodInvocationTree callback
                        && callback.getMethodSelect() instanceof MemberSelectTree select) {
                    String source = expression(callbackSource(select.getExpression()));
                    return words(executable.getSimpleName().toString()) + " " + source + " to " + target;
                }
                return words(executable.getSimpleName().toString()) + " " + target;
            }

            @Override public Void visitReturn(ReturnTree node, Void unused) {
                if (!slice.contains(node)) return super.visitReturn(node, unused);
                if (node.getExpression() != null) {
                    scan(node.getExpression(), unused);
                    if (isPredicateExpression(node.getExpression())) {
                        PredicatePlan predicate = addPredicatePlan(node.getExpression());
                        enter(predicate);
                        var outcomes = new ArrayList<Tail>(predicate.trueTails());
                        outcomes.addAll(predicate.falseTails());
                        frontier = List.copyOf(outcomes);
                    }
                }
                if (deferredReturnDepth > 0) {
                    deferredReturns.add(new DeferredReturn(List.copyOf(frontier), node, returnedLabel(node)));
                    frontier = List.of();
                    return null;
                }
                if (!root) {
                    frontier.forEach(tail -> {
                        exitNodes.add(new Tail(tail.nodeId(),
                                tail.outcome().equals("next") ? "result" : tail.outcome()));
                    });
                    frontier = List.of();
                    return null;
                }
                String id = stop(location, node);
                if (node.getExpression() != null
                        && unwrapParentheses(node.getExpression()) instanceof MethodInvocationTree call
                        && receiverEvidenceRelevant(call)) {
                    addEvidenceTargets(id, node.getExpression());
                }
                builder.addProbe(id, AnalysisManifest.ProbeKind.OUTCOME,
                        ownerHint(location.path()), runtimeMemberHint(node), methodDescriptor(location),
                        mapping(location, node));
                String returned = returnedLabel(node);
                for (Tail tail : frontier) {
                    String outcome = tail.outcome().equals("next")
                            ? returned : tail.outcome() + "; " + returned;
                    builder.addEdge(tail.nodeId(), id, outcome);
                }
                lastNode = id;
                exitNodes.add(new Tail(id, "result"));
                frontier = List.of();
                return null;
            }

            @Override public Void visitThrow(ThrowTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitThrow(node, unused);
                scan(node.getExpression(), unused);
                if (catchingDepth > 0) {
                    String alternative = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                            "use alternative decision result", node, null);
                    advance(alternative);
                    caughtTails.addAll(frontier);
                    frontier = List.of();
                    return null;
                }
                String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "decision cannot continue", node, null);
                advance(id);
                connectFailureToStop(id, location, node, root);
                if (root) {
                    String terminal = stop(location, node);
                    lastNode = terminal;
                    exitNodes.add(new Tail(terminal, "failure"));
                }
                frontier = List.of();
                return null;
            }

            @Override public Void visitForLoop(ForLoopTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitForLoop(node, unused);
                IndexedLoopPlan indexed = indexedLoop(node);
                if (indexed != null) {
                    String loop = add(BusinessDecisionGraph.NodeKind.CHOICE,
                            "a following entry exists", node, null);
                    advance(loop);
                    frontier = List.of(new Tail(loop, "yes"));
                    transparentLoopAliases.addAll(indexed.aliases());
                    try {
                        scan(node.getStatement(), unused);
                    } finally {
                        transparentLoopAliases.removeAll(indexed.aliases());
                    }
                    for (Tail tail : frontier) builder.addEdge(tail.nodeId(), loop, "next entry");
                    frontier = List.of(new Tail(loop, "no"));
                    return null;
                }
                scan(node.getInitializer(), unused);
                if (node.getCondition() != null) scan(node.getCondition(), unused);
                String loop = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        node.getCondition() == null ? "repeat entries" : "repeat while " + expression(node.getCondition()),
                        node, null);
                advance(loop);
                frontier = List.of(new Tail(loop, "item"));
                scan(node.getStatement(), unused);
                scan(node.getUpdate(), unused);
                for (Tail tail : frontier) builder.addEdge(tail.nodeId(), loop, "next item");
                frontier = List.of(new Tail(loop, "done"));
                return null;
            }

            private IndexedLoopPlan indexedLoop(ForLoopTree loop) {
                if (loop.getInitializer().size() != 1
                        || !(loop.getInitializer().getFirst() instanceof VariableTree counter)
                        || !(counter.getInitializer() instanceof LiteralTree initial)
                        || !(initial.getValue() instanceof Number number) || number.intValue() != 0
                        || !(loop.getCondition() instanceof BinaryTree condition)
                        || condition.getKind() != Tree.Kind.LESS_THAN
                        || !(condition.getLeftOperand() instanceof IdentifierTree left)
                        || !left.getName().contentEquals(counter.getName())
                        || !(condition.getRightOperand() instanceof MethodInvocationTree sizeCall)
                        || !(sizeCall.getMethodSelect() instanceof MemberSelectTree sizeSelect)
                        || !sizeSelect.getIdentifier().contentEquals("size")
                        || !sizeCall.getArguments().isEmpty()
                        || loop.getUpdate().size() != 1
                        || !(loop.getUpdate().getFirst() instanceof ExpressionStatementTree updateStatement)
                        || !(updateStatement.getExpression() instanceof UnaryTree update)
                        || !(update.getKind() == Tree.Kind.POSTFIX_INCREMENT
                        || update.getKind() == Tree.Kind.PREFIX_INCREMENT)
                        || !(update.getExpression() instanceof IdentifierTree updated)
                        || !updated.getName().contentEquals(counter.getName())) return null;
                String collection = sizeSelect.getExpression().toString();
                var aliases = new ArrayList<VariableTree>();
                new TreeScanner<Void, Void>() {
                    @Override public Void visitVariable(VariableTree variable, Void unused) {
                        if (indexedAccess(variable.getInitializer(), collection, counter.getName().toString())) {
                            aliases.add(variable);
                            return null;
                        }
                        return super.visitVariable(variable, unused);
                    }
                }.scan(loop.getStatement(), null);
                return new IndexedLoopPlan(List.copyOf(aliases));
            }

            private boolean indexedAccess(Tree tree, String collection, String counter) {
                if (!(tree instanceof MethodInvocationTree call)
                        || !(call.getMethodSelect() instanceof MemberSelectTree select)
                        || !select.getIdentifier().contentEquals("get")
                        || !select.getExpression().toString().equals(collection)
                        || call.getArguments().size() != 1
                        || !(call.getArguments().getFirst() instanceof IdentifierTree index)) return false;
                return index.getName().contentEquals(counter);
            }

            private record IndexedLoopPlan(List<VariableTree> aliases) { }

            @Override public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitEnhancedForLoop(node, unused);
                scan(node.getExpression(), unused);
                String loop = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        "for each " + words(node.getVariable().getName().toString()) + " in "
                                + expression(node.getExpression()), node, null);
                advance(loop);
                frontier = List.of(new Tail(loop, "item"));
                scan(node.getStatement(), unused);
                for (Tail tail : frontier) builder.addEdge(tail.nodeId(), loop, "next item");
                frontier = List.of(new Tail(loop, "done"));
                return null;
            }

            @Override public Void visitWhileLoop(WhileLoopTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitWhileLoop(node, unused);
                return scanConditionalLoop(node, node.getCondition(), node.getStatement(), unused);
            }

            @Override public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitDoWhileLoop(node, unused);
                return scanConditionalLoop(node, node.getCondition(), node.getStatement(), unused);
            }

            private Void scanConditionalLoop(Tree node, Tree condition, Tree body, Void unused) {
                scan(condition, unused);
                String loop = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        "repeat while " + expression(condition), node, null);
                advance(loop);
                frontier = List.of(new Tail(loop, "true"));
                scan(body, unused);
                for (Tail tail : frontier) builder.addEdge(tail.nodeId(), loop, "repeat");
                frontier = List.of(new Tail(loop, "false"));
                return null;
            }

            @Override public Void visitTry(TryTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return null;
                if (!resourcesAreDecisionSafe(node)) {
                    addCoverageGap(node, "resource close logic can change the decision but is unavailable");
                }
                if (node.getCatches().isEmpty()) {
                    if (node.getFinallyBlock() == null) {
                        scan(node.getBlock(), unused);
                        return null;
                    }
                    List<DeferredReturn> outerDeferred = deferredReturns;
                    deferredReturns = new ArrayList<>();
                    deferredReturnDepth++;
                    scan(node.getBlock(), unused);
                    deferredReturnDepth--;
                    frontier = applyFinally(
                            node.getFinallyBlock(), List.copyOf(frontier), deferredReturns, outerDeferred);
                    deferredReturns = outerDeferred;
                    return null;
                }

                String choice = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        "select decision result path", node, null);
                advance(choice);
                List<Tail> outerCaught = caughtTails;
                caughtTails = new ArrayList<>();
                List<DeferredReturn> outerDeferred = deferredReturns;
                boolean ownsDeferredReturns = node.getFinallyBlock() != null;
                if (ownsDeferredReturns) deferredReturns = new ArrayList<>();
                if (node.getFinallyBlock() != null) deferredReturnDepth++;
                frontier = List.of(new Tail(choice, "primary result"));
                catchingDepth++;
                scan(node.getBlock(), unused);
                catchingDepth--;
                List<Tail> normalTails = List.copyOf(frontier);
                for (ReturnTree returned : returnsIn(node.getBlock())) {
                    builder.addControlTarget(choice, "primary result", ownerHint(location.path()),
                            runtimeMemberHint(returned), methodDescriptor(location),
                            mapping(location, returned).line(), AnalysisManifest.ControlPoint.RETURN);
                }

                var merged = new ArrayList<Tail>(normalTails);
                int alternativeIndex = 0;
                for (CatchTree caught : node.getCatches()) {
                    String outcome = "alternative result " + (++alternativeIndex);
                    var inputs = new ArrayList<Tail>();
                    inputs.add(new Tail(choice, outcome));
                    if (alternativeIndex == 1) inputs.addAll(caughtTails);
                    frontier = List.copyOf(inputs);
                    scan(caught.getBlock(), unused);
                    merged.addAll(frontier);
                    Tree target = caught.getBlock().getStatements().isEmpty()
                            ? null : caught.getBlock().getStatements().getFirst();
                    if (target == null) {
                        addCoverageGap(caught, "catch path has no executable source line");
                    } else {
                        builder.addControlTarget(choice, outcome, ownerHint(location.path()),
                                runtimeMemberHint(caught), methodDescriptor(location), mapping(location, target).line());
                    }
                }
                if (node.getFinallyBlock() != null) deferredReturnDepth--;
                caughtTails = outerCaught;
                frontier = List.copyOf(merged);
                if (node.getFinallyBlock() != null) {
                    frontier = applyFinally(
                            node.getFinallyBlock(), List.copyOf(frontier), deferredReturns, outerDeferred);
                }
                if (ownsDeferredReturns) deferredReturns = outerDeferred;
                if (hasUnavailableExceptionTrigger(node.getBlock())) {
                    addCoverageGap(node.getBlock(),
                            "exception-triggering decision logic is unavailable");
                }
                return null;
            }

            @Override public Void visitSynchronized(SynchronizedTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return null;
                scan(node.getBlock(), unused);
                return null;
            }

            private String add(
                    BusinessDecisionGraph.NodeKind kind,
                    String label,
                    Tree tree,
                    AnalysisManifest.ProbeKind probe) {
                return builder.addNode(kind, label, Map.of(), mapping(location, tree), probe,
                        ownerHint(location.path()), runtimeMemberHint(tree), methodDescriptor(location));
            }

            private PredicatePlan addPredicatePlan(Tree condition) {
                return addPredicatePlan(condition, AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
            }

            private PredicatePlan addPredicatePlan(
                    Tree condition, AnalysisManifest.BranchCompletion completion) {
                Tree unwrapped = unwrapParentheses(condition);
                if (unwrapped instanceof UnaryTree unary
                        && unary.getKind() == Tree.Kind.LOGICAL_COMPLEMENT
                        && containsShortCircuit(unary.getExpression())) {
                    PredicatePlan nested = addPredicatePlan(unary.getExpression(), reverse(completion));
                    return new PredicatePlan(nested.entryNodeId(), nested.falseTails(), nested.trueTails());
                }
                if (unwrapped instanceof ConditionalExpressionTree conditional) {
                    PredicatePlan selector = addPredicatePlan(
                            conditional.getCondition(), AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
                    PredicatePlan whenTrue = addPredicatePlan(conditional.getTrueExpression(), completion);
                    PredicatePlan whenFalse = addPredicatePlan(conditional.getFalseExpression(), completion);
                    connect(selector.trueTails(), whenTrue.entryNodeId());
                    connect(selector.falseTails(), whenFalse.entryNodeId());
                    var trueTails = new ArrayList<Tail>(whenTrue.trueTails());
                    trueTails.addAll(whenFalse.trueTails());
                    var falseTails = new ArrayList<Tail>(whenTrue.falseTails());
                    falseTails.addAll(whenFalse.falseTails());
                    return new PredicatePlan(selector.entryNodeId(), List.copyOf(trueTails), List.copyOf(falseTails));
                }
                if (unwrapped instanceof BinaryTree binary
                        && binary.getKind() == Tree.Kind.CONDITIONAL_AND) {
                    PredicatePlan left = addPredicatePlan(
                            binary.getLeftOperand(), AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
                    PredicatePlan right = addPredicatePlan(binary.getRightOperand(), completion);
                    connect(left.trueTails(), right.entryNodeId());
                    var falseTails = new ArrayList<Tail>(left.falseTails());
                    falseTails.addAll(right.falseTails());
                    return new PredicatePlan(left.entryNodeId(), right.trueTails(), List.copyOf(falseTails));
                }
                if (unwrapped instanceof BinaryTree binary
                        && binary.getKind() == Tree.Kind.CONDITIONAL_OR) {
                    PredicatePlan left = addPredicatePlan(
                            binary.getLeftOperand(), AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED);
                    PredicatePlan right = addPredicatePlan(binary.getRightOperand(), completion);
                    connect(left.falseTails(), right.entryNodeId());
                    var trueTails = new ArrayList<Tail>(left.trueTails());
                    trueTails.addAll(right.trueTails());
                    return new PredicatePlan(left.entryNodeId(), List.copyOf(trueTails), right.falseTails());
                }
                String id = add(BusinessDecisionGraph.NodeKind.PREDICATE,
                        expression(unwrapped), unwrapped, AnalysisManifest.ProbeKind.PREDICATE);
                addEvidenceTargets(id, unwrapped);
                builder.setBranchCompletions(id, List.of(completion));
                return new PredicatePlan(id, List.of(new Tail(id, "true")), List.of(new Tail(id, "false")));
            }

            private void addEvidenceTargets(String nodeId, Tree predicate) {
                if (runtimeMemberHint(predicate).endsWith("#lambda")) return;
                Map<String, Integer> parameters = new LinkedHashMap<>();
                for (int index = 0; index < location.method().getParameters().size(); index++) {
                    parameters.put(location.method().getParameters().get(index).getName().toString(), index);
                }
                var seen = new LinkedHashSet<String>();
                final boolean[] unavailable = { false };
                final boolean[] unsupportedReceiver = { false };
                Tree root = unwrapParentheses(predicate);
                if (root instanceof MethodInvocationTree call
                        && call.getMethodSelect() instanceof MemberSelectTree member
                        && member.getExpression() instanceof IdentifierTree identifier) {
                    String name = identifier.getName().toString();
                    Integer argumentIndex = parameters.get(name);
                    if (argumentIndex != null && seen.add(name) && !technicalIdentifier(name)) {
                        builder.addEvidenceTarget(nodeId, ownerHint(location.path()),
                                location.method().getName().toString(), methodDescriptor(location),
                                argumentIndex, words(name), mapping(location, identifier).line());
                    }
                }
                new TreeScanner<Void, Void>() {
                    @Override public Void visitIdentifier(IdentifierTree identifier, Void unused) {
                        String name = identifier.getName().toString();
                        Integer argumentIndex = parameters.get(name);
                        if (argumentIndex != null && seen.add(name) && !technicalIdentifier(name)) {
                            builder.addEvidenceTarget(nodeId, ownerHint(location.path()),
                                    location.method().getName().toString(), methodDescriptor(location),
                                    argumentIndex, words(name), mapping(location, identifier).line());
                        } else if (argumentIndex == null && !name.equals("this")
                                && !name.equals("super") && !technicalPosition(identifier)) {
                            unavailable[0] = true;
                        }
                        return super.visitIdentifier(identifier, unused);
                    }

                    @Override public Void visitMethodInvocation(MethodInvocationTree call, Void unused) {
                        if (call.getMethodSelect() instanceof MemberSelectTree member) {
                            Tree receiver = member.getExpression();
                            boolean relevantReceiver = receiverEvidenceRelevant(call);
                            boolean self = receiver instanceof IdentifierTree identifier
                                    && (identifier.getName().contentEquals("this")
                                    || identifier.getName().contentEquals("super"));
                            TreePath receiverPath = TreePath.getPath(location.unit(), receiver);
                            Element receiverElement = receiverPath == null ? null : trees.getElement(receiverPath);
                            boolean typeReceiver = receiverElement != null && Set.of(
                                    ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM,
                                    ElementKind.RECORD, ElementKind.ANNOTATION_TYPE,
                                    ElementKind.PACKAGE).contains(receiverElement.getKind());
                            if (relevantReceiver && !self && !typeReceiver) {
                                if (!(receiver instanceof IdentifierTree identifier)
                                        || !parameters.containsKey(identifier.getName().toString())) {
                                    unsupportedReceiver[0] = true;
                                }
                                scan(receiver, unused);
                            }
                        }
                        for (Tree argument : call.getArguments()) scan(argument, unused);
                        return null;
                    }

                    @Override public Void visitMemberSelect(MemberSelectTree member, Void unused) {
                        unavailable[0] = true;
                        return null;
                    }
                }.scan(predicate, null);
                if (unsupportedReceiver[0]
                        || (unavailable[0] && requiresOperandEvidence(predicate))) {
                    long line = mapping(location, predicate).line();
                    builder.addEvidenceTarget(nodeId, ownerHint(location.path()),
                            location.method().getName().toString(), methodDescriptor(location), -1,
                            line > 0 ? "exact predicate evidence is unavailable at source line " + line
                                    : "exact predicate evidence is unavailable at an unknown source line",
                            line);
                }
            }

            private boolean receiverEvidenceRelevant(MethodInvocationTree call) {
                if (!(call.getMethodSelect() instanceof MemberSelectTree member)) return false;
                if (member.getExpression() instanceof IdentifierTree identifier
                        && location.method().getParameters().stream().anyMatch(parameter ->
                        parameter.getName().contentEquals(identifier.getName()))) return true;
                return Set.of("equals", "contains", "isBefore", "isAfter", "isEqual",
                        "startsWith", "endsWith", "matches").contains(member.getIdentifier().toString());
            }

            private boolean requiresOperandEvidence(Tree predicate) {
                Tree unwrapped = unwrapParentheses(predicate);
                if (unwrapped instanceof UnaryTree unary
                        && unary.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
                    unwrapped = unwrapParentheses(unary.getExpression());
                }
                return !(unwrapped instanceof MethodInvocationTree);
            }

            private void enter(PredicatePlan plan) {
                connect(frontier, plan.entryNodeId());
                lastNode = plan.entryNodeId();
            }

            private void connect(List<Tail> tails, String target) {
                for (Tail tail : tails) builder.addEdge(tail.nodeId(), target, tail.outcome());
            }

            private AnalysisManifest.BranchCompletion reverse(AnalysisManifest.BranchCompletion completion) {
                return completion == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED
                        ? AnalysisManifest.BranchCompletion.BOTH_OUTCOMES
                        : AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED;
            }

            private record PredicatePlan(String entryNodeId, List<Tail> trueTails, List<Tail> falseTails) { }

            private record DeferredReturn(List<Tail> tails, ReturnTree tree, String resultLabel) { }

            private void advance(String nodeId) {
                for (Tail tail : frontier) builder.addEdge(tail.nodeId(), nodeId, tail.outcome());
                frontier = List.of(new Tail(nodeId, "next"));
                lastNode = nodeId;
            }

            private void addCoverageGap(Tree tree, String description) {
                String id = add(BusinessDecisionGraph.NodeKind.COVERAGE_GAP,
                        "analysis incomplete: " + description, tree, null);
                List<Tail> unresolved = frontier.stream().map(tail -> new Tail(tail.nodeId(), "unresolved")).toList();
                frontier = unresolved;
                advance(id);
                builder.addGap(id, description + " affects the decision");
                var source = mapping(location, tree);
                diagnostics.add(new AnalysisManifest.AnalysisDiagnostic(
                        AnalysisManifest.Severity.WARNING, source.source(), source.line(), source.column(),
                        source.treeKind(), description + " is outside the supported generic analysis subset"));
            }

            private String caseLabel(CaseTree branch, int caseIndex) {
                if (branch.getExpressions().isEmpty()) {
                    boolean fallback = branch.getLabels().stream()
                            .anyMatch(label -> label.getKind() == Tree.Kind.DEFAULT_CASE_LABEL);
                    return fallback ? "default" : "matching alternative " + caseIndex;
                }
                return branch.getExpressions().stream().map(StaticDecisionAnalyzer::expression)
                        .collect(Collectors.joining(" or "));
            }

            private Map<Long, Long> controlLineCounts(List<? extends CaseTree> cases) {
                return cases.stream().map(this::controlTarget).filter(Objects::nonNull)
                        .map(tree -> mapping(location, tree).line())
                        .filter(line -> line > 0)
                        .collect(Collectors.groupingBy(line -> line, LinkedHashMap::new, Collectors.counting()));
            }

            private Tree controlTarget(CaseTree branch) {
                if (branch.getStatements() != null) {
                    return branch.getStatements().isEmpty() ? null : branch.getStatements().getFirst();
                }
                return branch.getBody();
            }

            private AnalysisManifest.ControlPoint controlPoint(CaseTree branch) {
                boolean pattern = isPatternCase(branch);
                if (pattern && branch.getGuard() != null) return AnalysisManifest.ControlPoint.PREDICATE_TRUE;
                if (pattern) return AnalysisManifest.ControlPoint.CASE_EXIT;
                return AnalysisManifest.ControlPoint.LINE;
            }

            private boolean isPatternCase(CaseTree branch) {
                return branch.getExpressions().isEmpty() && branch.getLabels().stream()
                        .noneMatch(label -> label.getKind() == Tree.Kind.DEFAULT_CASE_LABEL);
            }

            private List<ReturnTree> returnsIn(Tree tree) {
                var returns = new ArrayList<ReturnTree>();
                new TreeScanner<Void, Void>() {
                    @Override public Void visitReturn(ReturnTree node, Void unused) {
                        returns.add(node);
                        return null;
                    }
                }.scan(tree, null);
                return List.copyOf(returns);
            }

            private List<Tail> applyFinally(
                    BlockTree finallyBlock,
                    List<Tail> normalTails,
                    List<DeferredReturn> returns,
                    List<DeferredReturn> outerReturns) {
                var merged = new ArrayList<Tail>();
                if (!normalTails.isEmpty()) {
                    frontier = normalTails;
                    scan(finallyBlock, null);
                    merged.addAll(frontier);
                }
                for (DeferredReturn returned : returns) {
                    frontier = returned.tails();
                    scan(finallyBlock, null);
                    if (frontier.isEmpty()) continue;
                    if (deferredReturnDepth > 0) {
                        outerReturns.add(new DeferredReturn(
                                List.copyOf(frontier), returned.tree(), returned.resultLabel()));
                        continue;
                    }
                    if (!root) {
                        frontier.forEach(tail -> exitNodes.add(new Tail(tail.nodeId(),
                                tail.outcome().equals("next") ? "result" : tail.outcome())));
                        continue;
                    }
                    String terminal = stop(location, returned.tree());
                    builder.addProbe(terminal, AnalysisManifest.ProbeKind.OUTCOME,
                            ownerHint(location.path()), runtimeMemberHint(returned.tree()),
                            methodDescriptor(location), mapping(location, returned.tree()));
                    for (Tail tail : frontier) {
                        String outcome = tail.outcome().equals("next")
                                ? returned.resultLabel() : tail.outcome() + "; " + returned.resultLabel();
                        builder.addEdge(tail.nodeId(), terminal, outcome);
                    }
                    lastNode = terminal;
                    exitNodes.add(new Tail(terminal, "result"));
                }
                return List.copyOf(merged);
            }

            private String returnedLabel(ReturnTree node) {
                if (node.getExpression() != null
                        && unwrapParentheses(node.getExpression()) instanceof SwitchExpressionTree) {
                    return "returns selected value";
                }
                return "returns " + (node.getExpression() == null
                        ? "no value"
                        : (isPredicateExpression(node.getExpression()) ? "whether " : "")
                                + expression(node.getExpression()));
            }

            private boolean hasUnavailableExceptionTrigger(Tree tree) {
                var unavailable = new boolean[1];
                new TreeScanner<Void, Void>() {
                    @Override public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                        if (!relevant(node, slice, dependencies)) return super.visitMethodInvocation(node, unused);
                        TreePath path = TreePath.getPath(location.unit(), node);
                        Element called = path == null ? null : trees.getElement(path);
                        if (!(called instanceof ExecutableElement executable)
                                || index.methods().get(executable) == null) unavailable[0] = true;
                        return super.visitMethodInvocation(node, unused);
                    }
                }.scan(tree, null);
                return unavailable[0];
            }

            private boolean resourcesAreDecisionSafe(TryTree tree) {
                if (tree.getResources().isEmpty()) return true;
                for (Tree resource : tree.getResources()) {
                    Tree expression = resource instanceof VariableTree variable
                            ? variable.getInitializer() : resource;
                    TreePath path = TreePath.getPath(location.unit(), expression);
                    TypeMirror type = path == null ? null : trees.getTypeMirror(path);
                    Element resourceType = type == null ? null : types.asElement(type);
                    if (!(resourceType instanceof TypeElement declared)) return false;
                    ExecutableElement close = elements.getAllMembers(declared).stream()
                            .filter(member -> member instanceof ExecutableElement)
                            .map(member -> (ExecutableElement) member)
                            .filter(method -> method.getSimpleName().contentEquals("close"))
                            .filter(method -> method.getParameters().isEmpty())
                            .findFirst().orElse(null);
                    MethodLocation closeLocation = close == null ? null : index.methods().get(close);
                    if (closeLocation == null || closeLocation.method().getBody() == null
                            || !closeLocation.method().getBody().getStatements().isEmpty()) return false;
                }
                return true;
            }

            private String runtimeMemberHint(Tree tree) {
                Tree current = tree;
                while (current != null && current != dependencies.method()) {
                    if (current instanceof LambdaExpressionTree) {
                        return location.method().getName() + "#lambda";
                    }
                    current = dependencies.parents().get(current);
                }
                return location.method().getName().toString();
            }

            private String derivationLabel(VariableTree variable) {
                String subject = words(variable.getName().toString());
                Tree initializer = variable.getInitializer();
                if (initializer.getKind() == Tree.Kind.NEW_CLASS) return "initialize " + subject;
                if (containsImplementationSyntax(initializer)) return "derive " + subject;
                if (initializer instanceof MethodInvocationTree call) {
                    if (call.getMethodSelect() instanceof MemberSelectTree member
                            && member.getIdentifier().contentEquals("validate")
                            && member.getExpression() instanceof IdentifierTree identifier
                            && validationHelperRoles.containsKey(identifier.getName().toString())) {
                        return "derive " + subject;
                    }
                    TreePath path = TreePath.getPath(location.unit(), call);
                    Element called = path == null ? null : trees.getElement(path);
                    if (called instanceof ExecutableElement executable
                            && (executable.getModifiers().contains(Modifier.ABSTRACT)
                            || executable.getEnclosingElement().getKind() == ElementKind.INTERFACE)) {
                        return "derive " + subject;
                    }
                }
                return "derive " + subject + " as " + expression(initializer);
            }

            private String invocationLabel(MethodInvocationTree call, ExecutableElement executable) {
                String method = executable.getSimpleName().toString();
                if (call.getMethodSelect() instanceof MemberSelectTree member) {
                    if (member.getExpression() instanceof IdentifierTree identifier
                            && method.equals("validate")) {
                        String role = validationHelperRoles.get(identifier.getName().toString());
                        if (role != null) return "evaluate " + role;
                    }
                    if (method.startsWith("set") && method.length() > 3) {
                        String receiver = expression(member.getExpression()).replaceFirst("^new\\s+", "");
                        return "set " + receiver + " " + words(method.substring(3));
                    }
                }
                return "evaluate " + words(method);
            }

            private boolean isPredicateOperation(MethodInvocationTree invocation) {
                String method = invocation.getMethodSelect() instanceof MemberSelectTree member
                        ? member.getIdentifier().toString()
                        : invocation.getMethodSelect().toString();
                return Set.of("filter", "anyMatch", "allMatch", "noneMatch", "dropWhile", "takeWhile")
                        .contains(method);
            }

            private TypeMirror dispatchReceiverType(MethodInvocationTree invocation) {
                if (!(invocation.getMethodSelect() instanceof MemberSelectTree member)) return null;
                TreePath receiverPath = TreePath.getPath(location.unit(), member.getExpression());
                return receiverPath == null ? null : trees.getTypeMirror(receiverPath);
            }

            private boolean isCompatibleDispatchTarget(
                    TypeElement implementation,
                    TypeElement contractOwner,
                    TypeMirror receiverType) {
                if (receiverType != null && types.isSubtype(implementation.asType(), receiverType)) return true;
                if (receiverType != null && !receiverType.toString().equals(contractOwner.asType().toString())) return false;
                return types.isSubtype(types.erasure(implementation.asType()), types.erasure(contractOwner.asType()));
            }

            private boolean hasDecisionBearingOverrides(ExecutableElement contract, TypeElement owner) {
                if (owner.getKind() != ElementKind.INTERFACE) return false;
                return index.types().stream()
                        .filter(type -> type.getKind().isClass())
                        .filter(type -> types.isSubtype(types.erasure(type.asType()), types.erasure(owner.asType())))
                        .map(type -> implementationOf(contract, type))
                        .filter(Objects::nonNull)
                        .anyMatch(method -> !isDecisionFreeProjection(method));
            }
        }

        private record Tail(String nodeId, String outcome) { }

        private record Extraction(String last, List<Tail> exits) { }

        private MethodLocation implementationOf(ExecutableElement contract, TypeElement implementation) {
            return index.methods().entrySet().stream()
                    .filter(entry -> entry.getKey().getEnclosingElement().equals(implementation))
                    .filter(entry -> entry.getKey().getSimpleName().contentEquals(contract.getSimpleName()))
                    .filter(entry -> entry.getKey().getParameters().size() == contract.getParameters().size())
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }

        private boolean isDecisionFreeValueAccess(ExecutableElement contract, TypeElement owner) {
            if (!contract.getParameters().isEmpty()) return false;
            var implementations = index.types().stream()
                    .filter(type -> type.getKind().isClass())
                    .filter(type -> !type.getModifiers().contains(Modifier.ABSTRACT))
                    .filter(type -> types.isSubtype(types.erasure(type.asType()), types.erasure(owner.asType())))
                    .map(type -> implementationOf(contract, type))
                    .filter(Objects::nonNull)
                    .toList();
            if (implementations.isEmpty()) return false;
            return implementations.stream().allMatch(location -> {
                if (location.method().getBody() == null
                        || location.method().getBody().getStatements().size() != 1
                        || !(location.method().getBody().getStatements().getFirst() instanceof ReturnTree returned)
                        || returned.getExpression() == null) return false;
                return returned.getExpression().getKind() == Tree.Kind.IDENTIFIER
                        || returned.getExpression().getKind() == Tree.Kind.MEMBER_SELECT;
            });
        }

        private boolean isDecisionFreeProjection(MethodLocation location) {
            if (!location.method().getParameters().isEmpty()
                    || location.method().getBody() == null
                    || location.method().getBody().getStatements().size() != 1
                    || !(location.method().getBody().getStatements().getFirst() instanceof ReturnTree returned)
                    || returned.getExpression() == null) return false;
            return isProjectionExpression(returned.getExpression());
        }

        private boolean isProjectionExpression(Tree expression) {
            return switch (expression) {
                case IdentifierTree ignored -> true;
                case MemberSelectTree selected -> isProjectionExpression(selected.getExpression());
                case ParenthesizedTree parenthesized -> isProjectionExpression(parenthesized.getExpression());
                case TypeCastTree cast -> isProjectionExpression(cast.getExpression());
                case MethodInvocationTree invocation -> invocation.getArguments().isEmpty()
                        && isProjectionExpression(invocation.getMethodSelect());
                default -> false;
            };
        }

        private static String businessRuleLabel(TypeElement implementation) {
            String simpleName = implementation.getSimpleName().toString()
                    .replaceFirst("(?:Calculator|Strategy|Policy|Service|Rule|Implementation|Impl)$", "");
            String label = words(simpleName).trim();
            return label.isBlank() ? "applicable business rule" : label + " rule";
        }

        private static ReturnTree firstReturn(Tree subtree, Set<ReturnTree> returns) {
            if (subtree == null) return null;
            final ReturnTree[] found = { null };
            new TreeScanner<Void, Void>() {
                @Override public Void visitReturn(ReturnTree node, Void unused) {
                    if (found[0] == null && returns.contains(node)) found[0] = node;
                    return null;
                }
            }.scan(subtree, null);
            return found[0];
        }

        private static ReturnTree fallthroughReturn(
                IfTree decision,
                DependencyGraphBuilder.MethodDependencies dependencies,
                Set<ReturnTree> returns) {
            Tree parent = dependencies.parents().get(decision);
            if (!(parent instanceof BlockTree block)) return null;
            var statements = block.getStatements();
            int index = statements.indexOf(decision);
            for (int next = index + 1; next < statements.size(); next++) {
                ReturnTree returned = firstReturn(statements.get(next), returns);
                if (returned != null) return returned;
            }
            return null;
        }

        private String methodDescriptor(MethodLocation location) {
            Element element = trees.getElement(location.path());
            if (!(element instanceof ExecutableElement executable)) {
                throw new IllegalArgumentException("attributed method is unavailable for runtime binding");
            }
            var descriptor = new StringBuilder("(");
            executable.getParameters().forEach(parameter -> descriptor.append(typeDescriptor(parameter.asType())));
            return descriptor.append(')').append(typeDescriptor(executable.getReturnType())).toString();
        }

        private String methodDescriptor(ExecutableElement executable) {
            var descriptor = new StringBuilder("(");
            executable.getParameters().forEach(parameter -> descriptor.append(typeDescriptor(parameter.asType())));
            return descriptor.append(')').append(typeDescriptor(executable.getReturnType())).toString();
        }

        private String typeDescriptor(TypeMirror type) {
            TypeMirror erased = types.erasure(type);
            return switch (erased.getKind()) {
                case BOOLEAN -> "Z";
                case BYTE -> "B";
                case SHORT -> "S";
                case INT -> "I";
                case LONG -> "J";
                case CHAR -> "C";
                case FLOAT -> "F";
                case DOUBLE -> "D";
                case VOID -> "V";
                case ARRAY -> "[" + typeDescriptor(((ArrayType) erased).getComponentType());
                case DECLARED, ERROR -> {
                    Element element = types.asElement(erased);
                    if (!(element instanceof TypeElement declared)) {
                        throw new IllegalArgumentException("declared runtime type is unavailable: " + erased);
                    }
                    yield "L" + elements.getBinaryName(declared).toString().replace('.', '/') + ";";
                }
                default -> throw new IllegalArgumentException(
                        "unsupported runtime descriptor type " + erased.getKind() + ": " + erased);
            };
        }

        private String ownerHint(TreePath methodPath) {
            Element element = trees.getElement(methodPath);
            return element == null ? "" : element.getEnclosingElement().toString();
        }

        private static boolean relevant(
                Tree tree,
                Set<Tree> slice,
                DependencyGraphBuilder.MethodDependencies dependencies) {
            if (slice.contains(tree)) return true;
            for (Tree item : slice) {
                Tree current = item;
                while (current != null && current != dependencies.method()) {
                    if (current == tree) return true;
                    current = dependencies.parents().get(current);
                }
            }
            Tree current = tree;
            while (current != null && current != dependencies.method()) {
                if (slice.contains(current)) return true;
                current = dependencies.parents().get(current);
            }
            return false;
        }

        private static boolean isPredicateExpression(Tree expression) {
            return switch (expression.getKind()) {
                case EQUAL_TO, NOT_EQUAL_TO, LESS_THAN, LESS_THAN_EQUAL,
                        GREATER_THAN, GREATER_THAN_EQUAL, CONDITIONAL_AND,
                        CONDITIONAL_OR, LOGICAL_COMPLEMENT -> true;
                default -> false;
            };
        }

        private AnalysisManifest.SourceMapping mapping(MethodLocation location, Tree tree) {
            SourcePositions positions = trees.getSourcePositions();
            long position = positions.getStartPosition(location.unit(), tree);
            long line = position < 0 ? -1 : location.unit().getLineMap().getLineNumber(position);
            long column = position < 0 ? -1 : location.unit().getLineMap().getColumnNumber(position);
            Path source = Path.of(location.unit().getSourceFile().toUri());
            return new AnalysisManifest.SourceMapping("pending", source, line, column, tree.getKind().name());
        }
    }

    private record MethodLocation(
            CompilationUnitTree unit,
            MethodTree method,
            TreePath path,
            long startPosition) { }

    private record SourceIndex(
            List<MethodLocation> annotatedMethods,
            Map<ExecutableElement, MethodLocation> methods,
            List<TypeElement> types) {
        private static SourceIndex create(
                List<CompilationUnitTree> units,
                Trees trees,
                List<Path> rootSourceFiles) {
            var annotated = new ArrayList<MethodLocation>();
            var methods = new LinkedHashMap<ExecutableElement, MethodLocation>();
            var types = new ArrayList<TypeElement>();
            Set<Path> rootSources = rootSourceFiles.stream()
                    .map(path -> path.toAbsolutePath().normalize())
                    .collect(Collectors.toUnmodifiableSet());
            SourcePositions positions = trees.getSourcePositions();
            for (CompilationUnitTree unit : units) {
                boolean graphRootSource = rootSources.contains(
                        Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize());
                new TreePathScanner<Void, Void>() {
                    @Override public Void visitMethod(MethodTree node, Void unused) {
                        Element element = trees.getElement(getCurrentPath());
                        if (element instanceof ExecutableElement executable) {
                            var location = new MethodLocation(unit, node, getCurrentPath(),
                                    positions.getStartPosition(unit, node));
                            methods.put(executable, location);
                            if (graphRootSource && hasFachTracing(node)) annotated.add(location);
                        }
                        return super.visitMethod(node, unused);
                    }

                    @Override public Void visitClass(com.sun.source.tree.ClassTree node, Void unused) {
                        Element element = trees.getElement(getCurrentPath());
                        if (element instanceof TypeElement type) types.add(type);
                        return super.visitClass(node, unused);
                    }
                }.scan(unit, null);
            }
            return new SourceIndex(List.copyOf(annotated), Map.copyOf(methods), List.copyOf(types));
        }
    }

    private static boolean hasFachTracing(MethodTree method) {
        return method.getModifiers().getAnnotations().stream()
                .map(AnnotationTree::getAnnotationType)
                .map(Object::toString)
                .anyMatch(name -> name.equals("FachTracing") || name.endsWith(".FachTracing"));
    }

    private static String annotationLabel(MethodTree method) {
        return method.getModifiers().getAnnotations().stream()
                .filter(annotation -> {
                    String name = annotation.getAnnotationType().toString();
                    return name.equals("FachTracing") || name.endsWith(".FachTracing");
                })
                .flatMap(annotation -> annotation.getArguments().stream())
                .map(Object::toString)
                .map(argument -> argument.contains("=") ? argument.substring(argument.indexOf('=') + 1).trim() : argument)
                .map(value -> value.replaceAll("^\"|\"$", ""))
                .findFirst()
                .orElse("");
    }

    private static Map<String, String> fingerprints(AnalysisRequest request) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (Path source : request.sourceFiles()) {
            result.put(source.toString(), hashBytes(Files.readAllBytes(source)));
        }
        return Collections.unmodifiableMap(result);
    }

    private static String expression(Tree tree) {
        String text = renderExpression(tree);
        text = java.util.regex.Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b").matcher(text)
                .replaceAll(match -> match.group().toLowerCase(Locale.ROOT));
        text = text.replaceAll("(?i)\\bnull\\b", "absent")
                .replaceAll("\\s+", " ").trim();
        return text;
    }

    private static String renderExpression(Tree tree) {
        return switch (tree) {
            case ParenthesizedTree parenthesized -> groupedExpression(parenthesized.getExpression());
            case BinaryTree binary -> renderBinary(binary);
            case UnaryTree unary when unary.getKind() == Tree.Kind.LOGICAL_COMPLEMENT ->
                    negatedExpression(unary.getExpression());
            case MethodInvocationTree call -> renderCall(call);
            case LambdaExpressionTree lambda -> renderExpression((Tree) lambda.getBody());
            case MemberSelectTree member -> words(member.getIdentifier().toString());
            case IdentifierTree identifier -> identifierLabel(identifier.getName().toString());
            case LiteralTree literal -> literal.getValue() == null ? "absent" : String.valueOf(literal.getValue());
            case InstanceOfTree instance -> renderExpression(instance.getExpression()) + " is a "
                    + words(instance.getType().toString());
            default -> words(tree.toString().replaceAll("[().,]", " "));
        };
    }

    private static String groupedExpression(Tree tree) {
        Tree unwrapped = tree instanceof ParenthesizedTree nested ? nested.getExpression() : tree;
        return switch (unwrapped.getKind()) {
            case CONDITIONAL_OR -> "either " + renderExpression(unwrapped);
            case CONDITIONAL_AND -> "all of " + renderExpression(unwrapped);
            default -> renderExpression(unwrapped);
        };
    }

    private static String negatedExpression(Tree tree) {
        Tree unwrapped = tree instanceof ParenthesizedTree parenthesized ? parenthesized.getExpression() : tree;
        if (unwrapped instanceof BinaryTree binary && isNullComparison(binary)) {
            return renderNullComparison(binary, true);
        }
        if (unwrapped instanceof BinaryTree binary && isComparison(binary.getKind())) {
            return renderExpression(binary.getLeftOperand()) + negatedBinaryOperator(binary.getKind())
                    + renderExpression(binary.getRightOperand());
        }
        if (unwrapped instanceof BinaryTree binary && binary.getKind() == Tree.Kind.CONDITIONAL_OR) {
            return "neither " + renderExpression(binary.getLeftOperand()) + " nor "
                    + renderExpression(binary.getRightOperand());
        }
        if (unwrapped instanceof BinaryTree binary && binary.getKind() == Tree.Kind.CONDITIONAL_AND) {
            return "not all of " + renderExpression(binary);
        }
        String rendered = renderExpression(unwrapped);
        if (rendered.startsWith("is ")) return "is not " + rendered.substring(3);
        if (rendered.startsWith("has ")) return "does not have " + rendered.substring(4);
        int equals = rendered.indexOf(" equals ");
        if (equals >= 0) {
            return rendered.substring(0, equals) + " does not equal "
                    + rendered.substring(equals + " equals ".length());
        }
        return "not " + rendered;
    }

    private static String renderBinary(BinaryTree binary) {
        if (isNullComparison(binary)) return renderNullComparison(binary, false);
        String collectionBoundary = collectionBoundary(binary);
        if (collectionBoundary != null) return collectionBoundary;
        return renderExpression(binary.getLeftOperand()) + binaryOperator(binary.getKind())
                + renderExpression(binary.getRightOperand());
    }

    private static String collectionBoundary(BinaryTree binary) {
        if (binary.getKind() != Tree.Kind.LESS_THAN
                || !(binary.getRightOperand() instanceof MethodInvocationTree call)
                || !(call.getMethodSelect() instanceof MemberSelectTree select)
                || !select.getIdentifier().contentEquals("size")
                || !call.getArguments().isEmpty()) return null;
        if (followingPosition(binary.getLeftOperand())) return "a following entry exists";
        if (technicalPosition(binary.getLeftOperand())) return "another entry exists";
        return null;
    }

    private static boolean isNullComparison(BinaryTree binary) {
        return (binary.getKind() == Tree.Kind.EQUAL_TO || binary.getKind() == Tree.Kind.NOT_EQUAL_TO)
                && (isNullLiteral(binary.getLeftOperand()) || isNullLiteral(binary.getRightOperand()));
    }

    private static boolean isComparison(Tree.Kind kind) {
        return switch (kind) {
            case EQUAL_TO, NOT_EQUAL_TO, LESS_THAN, LESS_THAN_EQUAL,
                    GREATER_THAN, GREATER_THAN_EQUAL -> true;
            default -> false;
        };
    }

    private static String negatedBinaryOperator(Tree.Kind kind) {
        return switch (kind) {
            case EQUAL_TO -> " does not equal ";
            case NOT_EQUAL_TO -> " equals ";
            case LESS_THAN -> " is at least ";
            case LESS_THAN_EQUAL -> " is above ";
            case GREATER_THAN -> " is at most ";
            case GREATER_THAN_EQUAL -> " is below ";
            default -> throw new IllegalArgumentException("not a comparison: " + kind);
        };
    }

    private static boolean isNullLiteral(Tree tree) {
        return tree instanceof LiteralTree literal && literal.getValue() == null;
    }

    private static String renderNullComparison(BinaryTree binary, boolean negated) {
        Tree subject = isNullLiteral(binary.getLeftOperand())
                ? binary.getRightOperand() : binary.getLeftOperand();
        boolean absent = binary.getKind() == Tree.Kind.EQUAL_TO;
        if (negated) absent = !absent;
        return renderExpression(subject) + (absent ? " is absent" : " exists");
    }

    private static boolean containsShortCircuit(Tree tree) {
        Tree unwrapped = unwrapParentheses(tree);
        if (unwrapped instanceof BinaryTree binary
                && (binary.getKind() == Tree.Kind.CONDITIONAL_AND
                || binary.getKind() == Tree.Kind.CONDITIONAL_OR)) return true;
        if (unwrapped instanceof UnaryTree unary && unary.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            return containsShortCircuit(unary.getExpression());
        }
        return false;
    }

    private static Tree unwrapParentheses(Tree tree) {
        Tree current = tree;
        while (current instanceof ParenthesizedTree parenthesized) current = parenthesized.getExpression();
        return current;
    }

    private static boolean containsImplementationSyntax(Tree tree) {
        final boolean[] found = { false };
        new TreeScanner<Void, Void>() {
            @Override public Void scan(Tree candidate, Void unused) {
                if (candidate != null && (candidate.getKind() == Tree.Kind.LAMBDA_EXPRESSION
                        || candidate.getKind() == Tree.Kind.MEMBER_REFERENCE
                        || candidate.getKind() == Tree.Kind.TYPE_CAST)) {
                    found[0] = true;
                }
                return found[0] ? null : super.scan(candidate, unused);
            }
        }.scan(tree, null);
        return found[0];
    }

    private static String renderCall(MethodInvocationTree call) {
        String method;
        String receiver = "";
        if (call.getMethodSelect() instanceof MemberSelectTree member) {
            method = member.getIdentifier().toString();
            receiver = renderExpression(member.getExpression());
        } else {
            method = call.getMethodSelect().toString();
        }
        if (method.equals("get") && call.getArguments().size() == 1) {
            Tree position = call.getArguments().getFirst();
            if (followingPosition(position)) return receiver.isBlank()
                    ? "following entry" : receiver + " following entry";
            if (technicalPosition(position)) return receiver.isBlank()
                    ? "current entry" : receiver + " current entry";
        }
        List<String> arguments = call.getArguments().stream()
                .filter(argument -> !technicalPosition(argument))
                .map(StaticDecisionAnalyzer::renderExpression).toList();
        if (method.equals("equals") && arguments.size() == 1) {
            if (receiver.equals("true")) return arguments.getFirst();
            if (receiver.equals("false")) return "not " + arguments.getFirst();
            return receiver + " equals " + arguments.getFirst();
        }
        if (method.equals("stream") && arguments.isEmpty()) return receiver;
        if (Set.of("filter", "anyMatch", "allMatch", "noneMatch").contains(method)
                && arguments.size() == 1) {
            String quantifier = switch (method) {
                case "filter" -> "items in";
                case "anyMatch" -> "any of";
                case "allMatch" -> "all of";
                case "noneMatch" -> "none of";
                default -> throw new IllegalStateException();
            };
            return quantifier + " " + receiver + " match " + arguments.getFirst();
        }
        String operation = switch (method) {
            case "contains" -> "contains";
            case "isBefore" -> "is before";
            case "isAfter" -> "is after";
            case "isEqual" -> "equals";
            case "get" -> "entry";
            case "size" -> "entry count";
            default -> words(method.replaceFirst("^get(?=[A-Z])", ""));
        };
        if (arguments.isEmpty() && (method.startsWith("get") || method.startsWith("is"))) {
            return receiver.isBlank() ? operation : receiver + " " + operation;
        }
        String subject = receiver.isBlank() ? operation : receiver + " " + operation;
        return arguments.isEmpty() ? subject : subject + " " + String.join(" and ", arguments);
    }

    private static String binaryOperator(Tree.Kind kind) {
        return switch (kind) {
            case CONDITIONAL_AND -> " and ";
            case CONDITIONAL_OR -> " or ";
            case EQUAL_TO -> " equals ";
            case NOT_EQUAL_TO -> " does not equal ";
            case LESS_THAN -> " is below ";
            case LESS_THAN_EQUAL -> " is at most ";
            case GREATER_THAN -> " is above ";
            case GREATER_THAN_EQUAL -> " is at least ";
            case PLUS -> " plus ";
            case MINUS -> " minus ";
            case MULTIPLY -> " multiplied by ";
            case DIVIDE -> " divided by ";
            default -> " " + words(kind.name()) + " ";
        };
    }

    private static boolean isSupportedLibraryOperation(ExecutableElement executable) {
        Element owner = executable.getEnclosingElement();
        if (!(owner instanceof TypeElement type)) return false;
        String qualifiedName = type.getQualifiedName().toString();
        if (qualifiedName.startsWith("java.") || qualifiedName.startsWith("javax.")
                || qualifiedName.startsWith("jakarta.")) return true;
        return isSupportedRecordOperation(executable, type);
    }

    private static boolean isProvenReadOnlyLibraryOperation(ExecutableElement executable) {
        Element owner = executable.getEnclosingElement();
        if (!(owner instanceof TypeElement type)) return false;
        String qualifiedName = type.getQualifiedName().toString();
        String method = executable.getSimpleName().toString();
        if (qualifiedName.equals("java.lang.String")
                || qualifiedName.equals("java.lang.Boolean")
                || qualifiedName.equals("java.lang.Byte")
                || qualifiedName.equals("java.lang.Short")
                || qualifiedName.equals("java.lang.Integer")
                || qualifiedName.equals("java.lang.Long")
                || qualifiedName.equals("java.lang.Float")
                || qualifiedName.equals("java.lang.Double")
                || qualifiedName.equals("java.lang.Character")
                || qualifiedName.equals("java.lang.Math")
                || qualifiedName.equals("java.lang.StrictMath")
                || qualifiedName.equals("java.lang.Thread") && Set.of(
                        "start", "startVirtualThread", "join", "currentThread", "isVirtual",
                        "isAlive", "threadId", "getName").contains(method)
                || qualifiedName.equals("java.lang.Iterable") && method.equals("forEach")
                || qualifiedName.startsWith("java.time.")
                || qualifiedName.equals("java.math.BigInteger")
                || qualifiedName.equals("java.math.BigDecimal")
                || qualifiedName.startsWith("java.util.stream.")
                || qualifiedName.equals("java.util.Optional")
                || qualifiedName.equals("java.util.OptionalInt")
                || qualifiedName.equals("java.util.OptionalLong")
                || qualifiedName.equals("java.util.OptionalDouble")) return true;
        if (qualifiedName.startsWith("java.util.") && Set.of(
                "size", "isEmpty", "indexOf", "lastIndexOf", "peek", "peekFirst", "peekLast", "element",
                "first", "last", "keySet", "values", "entrySet", "iterator", "listIterator",
                "spliterator", "stream", "parallelStream", "getTime", "before", "after",
                "compareTo", "of", "ofEntries",
                "copyOf", "forEach", "forEachOrdered")
                .contains(method)) return true;
        if (qualifiedName.equals("java.util.Objects") && Set.of(
                "isNull", "nonNull", "requireNonNull", "toIdentityString")
                .contains(method)) return true;
        if (qualifiedName.equals("java.util.Arrays") && Set.of(
                "asList", "binarySearch", "compare", "compareUnsigned", "copyOf", "copyOfRange",
                "deepEquals", "deepHashCode", "deepToString", "equals", "hashCode", "mismatch",
                "stream", "toString").contains(method)) return true;
        if (qualifiedName.equals("java.util.Collections") && Set.of(
                "binarySearch", "disjoint", "enumeration", "frequency", "indexOfSubList",
                "lastIndexOfSubList", "list", "max", "min", "nCopies", "singleton",
                "singletonList", "singletonMap", "unmodifiableCollection", "unmodifiableList",
                "unmodifiableMap", "unmodifiableSet").contains(method)) return true;
        return isSupportedRecordOperation(executable, type);
    }

    private static boolean isSupportedRecordOperation(ExecutableElement executable, TypeElement type) {
        if (type.getKind() != ElementKind.RECORD) return false;
        String method = executable.getSimpleName().toString();
        if (method.equals("equals") && executable.getParameters().size() == 1) return true;
        if ((method.equals("hashCode") || method.equals("toString")) && executable.getParameters().isEmpty()) return true;
        return type.getRecordComponents().stream()
                .anyMatch(component -> component.getSimpleName().contentEquals(method)
                        && executable.getParameters().isEmpty());
    }

    private static String words(String camelCase) {
        String separated = camelCase.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        String business = java.util.Arrays.stream(separated.split("\\s+"))
                .filter(word -> !word.equals("id") && !word.equals("ids"))
                .collect(Collectors.joining(" "));
        return business.isBlank() ? "value" : business;
    }

    private static String identifierLabel(String identifier) {
        if (identifier.equals("i") || identifier.equals("idx") || identifier.equals("index")) return "entry";
        if (identifier.length() == 1) return "item";
        return words(identifier);
    }

    private static boolean technicalPosition(Tree tree) {
        Tree unwrapped = unwrapParentheses(tree);
        return unwrapped instanceof IdentifierTree identifier
                && (identifier.getName().contentEquals("i")
                || identifier.getName().contentEquals("idx")
                || identifier.getName().contentEquals("index"));
    }

    private static boolean followingPosition(Tree tree) {
        Tree unwrapped = unwrapParentheses(tree);
        if (!(unwrapped instanceof BinaryTree binary) || binary.getKind() != Tree.Kind.PLUS) return false;
        return technicalPosition(binary.getLeftOperand()) && isOne(binary.getRightOperand())
                || technicalPosition(binary.getRightOperand()) && isOne(binary.getLeftOperand());
    }

    private static boolean isOne(Tree tree) {
        return tree instanceof LiteralTree literal && literal.getValue() instanceof Number number
                && number.intValue() == 1;
    }

    private static boolean technicalIdentifier(String identifier) {
        String separated = identifier.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ').toLowerCase(Locale.ROOT);
        return java.util.Arrays.stream(separated.split("\\s+"))
                .anyMatch(part -> part.equals("id") || part.equals("ids"));
    }

    private static String hash(String prefix, Object... parts) {
        String joined = prefix + java.util.Arrays.stream(parts).map(String::valueOf)
                .collect(Collectors.joining("\u0000", "\u0000", ""));
        return hashBytes(joined.getBytes(StandardCharsets.UTF_8)).substring(0, 24);
    }

    private static String hashBytes(byte[] input) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
