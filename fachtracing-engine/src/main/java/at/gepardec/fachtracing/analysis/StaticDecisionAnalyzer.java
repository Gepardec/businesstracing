package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
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
import javax.lang.model.util.Types;
import javax.lang.model.type.TypeMirror;
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
import java.util.Set;
import java.util.stream.Collectors;

/** Framework-neutral Java 21 source analyzer for {@code @FachTracing} entry points. */
public final class StaticDecisionAnalyzer {
    /** Parses and attributes sources, then analyzes the first annotated method in source order. */
    public AnalysisManifest.AnalysisResult analyze(AnalysisRequest request) {
        List<AnalysisManifest.AnalysisResult> results = analyzeAll(request);
        if (results.isEmpty()) throw new IllegalArgumentException("No @FachTracing method found");
        return results.getFirst();
    }

    /** Parses and attributes sources, then analyzes every annotated method in deterministic source order. */
    public List<AnalysisManifest.AnalysisResult> analyzeAll(AnalysisRequest request) {
        Objects.requireNonNull(request, "request");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK is required for source analysis");

        var compilerDiagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(
                compilerDiagnostics, Locale.ROOT, request.charset())) {
            Iterable<? extends JavaFileObject> sources = files.getJavaFileObjectsFromPaths(request.sourceFiles());
            List<String> options = new ArrayList<>(List.of("-proc:none", "--release", "21"));
            if (!request.compilationClasspath().isEmpty()) {
                options.add("-classpath");
                options.add(request.compilationClasspath().stream()
                        .map(Path::toString)
                        .collect(Collectors.joining(System.getProperty("path.separator"))));
            }
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
                throw new IllegalArgumentException("Source attribution failed: " + String.join(" | ", errors));
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
                var extractor = new Extractor(trees, task.getTypes(), index, builder, diagnostics);
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

    private static final class Extractor {
        private final Trees trees;
        private final Types types;
        private final SourceIndex index;
        private final DecisionGraphBuilder builder;
        private final List<AnalysisManifest.AnalysisDiagnostic> diagnostics;
        private final List<String> pendingFailureNodes = new ArrayList<>();
        private String rootStop;

        private Extractor(
                Trees trees,
                Types types,
                SourceIndex index,
                DecisionGraphBuilder builder,
                List<AnalysisManifest.AnalysisDiagnostic> diagnostics) {
            this.trees = trees;
            this.types = types;
            this.index = index;
            this.builder = builder;
            this.diagnostics = diagnostics;
        }

        private String addEntry(MethodLocation method) {
            return builder.addNode(BusinessDecisionGraph.NodeKind.ENTRY, "Start", Map.of(),
                    mapping(method, method.method()), AnalysisManifest.ProbeKind.ENTRY,
                    ownerHint(method.path()), method.method().getName().toString());
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

        private Extraction extract(
                MethodLocation location,
                String predecessor,
                boolean root,
                Set<ExecutableElement> activeMethods) {
            Element methodElement = trees.getElement(location.path());
            if (methodElement instanceof ExecutableElement executable && !activeMethods.add(executable)) {
                return new Extraction(predecessor, List.of(new Tail(predecessor, "result")));
            }
            var dependencies = new DependencyGraphBuilder().build(location.method());
            Set<Tree> slice = new BackwardDecisionSlicer().slice(dependencies);
            var flow = new FlowScanner(location, root, activeMethods, dependencies, slice, predecessor);
            flow.scan(new TreePath(location.path(), location.method().getBody()), null);
            if (methodElement instanceof ExecutableElement executable) activeMethods.remove(executable);
            return new Extraction(flow.lastNode(), flow.exits());
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

            var dependencies = new DependencyGraphBuilder().build(location.method());
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
                                            implementationMethod.method().getName().toString());
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
                            source.treeKind(), description + " is outside the walking-skeleton analysis subset"));
                }

                private String add(
                        BusinessDecisionGraph.NodeKind kind,
                        String label,
                        Tree tree,
                        AnalysisManifest.ProbeKind probe) {
                    return builder.addNode(kind, label, Map.of(), mapping(location, tree), probe,
                            ownerHint(location.path()), location.method().getName().toString());
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
            private final List<Tail> exitNodes = new ArrayList<>();
            private List<Tail> frontier;
            private String lastNode;

            private FlowScanner(
                    MethodLocation location,
                    boolean root,
                    Set<ExecutableElement> activeMethods,
                    DependencyGraphBuilder.MethodDependencies dependencies,
                    Set<Tree> slice,
                    String predecessor) {
                this.location = location;
                this.root = root;
                this.activeMethods = activeMethods;
                this.dependencies = dependencies;
                this.slice = slice;
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
                scan(node.getCondition(), unused);
                String predicate = addPredicate(node.getCondition());
                advance(predicate);

                List<Tail> beforeBranch = List.of(new Tail(predicate, "true"));
                frontier = beforeBranch;
                scan(node.getThenStatement(), unused);
                List<Tail> trueTails = List.copyOf(frontier);

                frontier = List.of(new Tail(predicate, "false"));
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
                        "choose by " + expression(node.getExpression()), node.getExpression(),
                        AnalysisManifest.ProbeKind.PREDICATE);
                advance(choice);
                var merged = new ArrayList<Tail>();
                for (CaseTree branch : node.getCases()) {
                    frontier = List.of(new Tail(choice, caseLabel(branch)));
                    if (branch.getStatements() != null) scan(branch.getStatements(), unused);
                    else scan(branch.getBody(), unused);
                    merged.addAll(frontier);
                }
                frontier = List.copyOf(merged);
                return null;
            }

            @Override public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitSwitchExpression(node, unused);
                scan(node.getExpression(), unused);
                String choice = add(BusinessDecisionGraph.NodeKind.CHOICE,
                        "choose by " + expression(node.getExpression()), node.getExpression(),
                        AnalysisManifest.ProbeKind.PREDICATE);
                advance(choice);
                var merged = new ArrayList<Tail>();
                for (CaseTree branch : node.getCases()) {
                    frontier = List.of(new Tail(choice, caseLabel(branch)));
                    if (branch.getStatements() != null) scan(branch.getStatements(), unused);
                    else scan(branch.getBody(), unused);
                    merged.addAll(frontier);
                }
                frontier = List.copyOf(merged);
                return null;
            }

            @Override public Void visitVariable(VariableTree node, Void unused) {
                if (node.getInitializer() == null || !relevant(node.getInitializer(), slice, dependencies)) {
                    return super.visitVariable(node, unused);
                }
                scan(node.getInitializer(), unused);
                String id = add(BusinessDecisionGraph.NodeKind.COMPUTATION, derivationLabel(node), node, null);
                advance(id);
                return null;
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
                String predicate = addPredicate(node.getCondition());
                advance(predicate);
                frontier = List.of(new Tail(predicate, "true"));
                scan(node.getTrueExpression(), unused);
                String trueValue = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                        "use " + expression(node.getTrueExpression()), node.getTrueExpression(), null);
                advance(trueValue);
                List<Tail> trueTails = List.copyOf(frontier);
                frontier = List.of(new Tail(predicate, "false"));
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
                if (!relevant(node, slice, dependencies)) return super.visitMethodInvocation(node, unused);
                Element called = trees.getElement(getCurrentPath());
                if (!(called instanceof ExecutableElement executable)) return super.visitMethodInvocation(node, unused);
                if (isSupportedLibraryOperation(executable)) return super.visitMethodInvocation(node, unused);

                scan(node.getMethodSelect(), unused);
                scan(node.getArguments(), unused);
                TypeElement owner = (TypeElement) executable.getEnclosingElement();
                TypeMirror receiverType = dispatchReceiverType(node);
                boolean dynamic = executable.getModifiers().contains(Modifier.ABSTRACT)
                        || hasDecisionBearingOverrides(executable, owner);
                if (dynamic) {
                    if (isDecisionFreeValueAccess(executable, owner)) return null;
                    String dispatch = add(BusinessDecisionGraph.NodeKind.DISPATCH,
                            "select applicable decision rule", node, AnalysisManifest.ProbeKind.DISPATCH);
                    advance(dispatch);
                    var resultTails = new ArrayList<Tail>();
                    int candidate = 0;
                    for (TypeElement implementation : index.types()) {
                        if (!implementation.getKind().isClass()
                                || implementation.getModifiers().contains(Modifier.ABSTRACT)
                                || !isCompatibleDispatchTarget(implementation, owner, receiverType)) {
                            continue;
                        }
                        String alternative = builder.addNode(
                                BusinessDecisionGraph.NodeKind.COMPUTATION,
                                businessRuleLabel(implementation),
                                Map.of("candidate", Integer.toString(++candidate)), null, null, "", "");
                        String candidateEdge = builder.addEdge(dispatch, alternative, "candidate " + candidate);
                        MethodLocation implementationMethod = implementationOf(executable, implementation);
                        if (implementationMethod == null) {
                            resultTails.add(new Tail(alternative, "result"));
                            continue;
                        }
                        builder.addDispatchTarget(dispatch, candidateEdge, implementation.toString(),
                                implementationMethod.method().getName().toString());
                        Extraction linked = extract(implementationMethod, alternative, false, activeMethods);
                        resultTails.addAll(linked.exits());
                    }
                    if (candidate == 0) {
                        addCoverageGap(node, "decision-rule implementations are unavailable");
                    } else {
                        frontier = List.copyOf(resultTails);
                    }
                } else {
                    MethodLocation callee = index.methods().get(executable);
                    if (callee != null && isDecisionFreeProjection(callee)) return null;
                    if (callee != null && !activeMethods.contains(executable)) {
                        String call = add(BusinessDecisionGraph.NodeKind.COMPUTATION,
                                "evaluate " + words(executable.getSimpleName().toString()), node, null);
                        advance(call);
                        Extraction linked = extract(callee, call, false, activeMethods);
                        frontier = linked.exits();
                    } else if (callee == null) {
                        addCoverageGap(node, "called decision logic is unavailable");
                    }
                }
                return null;
            }

            @Override public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
                if (!relevant(node, slice, dependencies)) return super.visitLambdaExpression(node, unused);
                Tree parent = dependencies.parents().get(node);
                if (!(parent instanceof MethodInvocationTree invocation)
                        || !isPredicateOperation(invocation)
                        || !(node.getBody() instanceof Tree body)) {
                    return super.visitLambdaExpression(node, unused);
                }
                scan(body, unused);
                String predicate = addPredicate(body);
                advance(predicate);
                return null;
            }

            @Override public Void visitReturn(ReturnTree node, Void unused) {
                if (!slice.contains(node)) return super.visitReturn(node, unused);
                if (node.getExpression() != null) {
                    scan(node.getExpression(), unused);
                    if (isPredicateExpression(node.getExpression())) {
                        String predicate = addPredicate(node.getExpression());
                        advance(predicate);
                    }
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
                builder.addProbe(id, AnalysisManifest.ProbeKind.OUTCOME,
                        ownerHint(location.path()), runtimeMemberHint(node), mapping(location, node));
                String returned = "returns " + (node.getExpression() == null
                        ? "no value"
                        : (isPredicateExpression(node.getExpression()) ? "whether " : "")
                                + expression(node.getExpression()));
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
                if (relevant(node, slice, dependencies)) addCoverageGap(node, "try statement");
                return super.visitTry(node, unused);
            }

            @Override public Void visitSynchronized(SynchronizedTree node, Void unused) {
                if (relevant(node, slice, dependencies)) addCoverageGap(node, "synchronized statement");
                return super.visitSynchronized(node, unused);
            }

            private String add(
                    BusinessDecisionGraph.NodeKind kind,
                    String label,
                    Tree tree,
                    AnalysisManifest.ProbeKind probe) {
                return builder.addNode(kind, label, Map.of(), mapping(location, tree), probe,
                        ownerHint(location.path()), runtimeMemberHint(tree));
            }

            private String addPredicate(Tree condition) {
                List<Tree> atomicPredicates = atomicPredicates(condition);
                String id = add(BusinessDecisionGraph.NodeKind.PREDICATE,
                        expression(condition), condition, AnalysisManifest.ProbeKind.PREDICATE);
                for (int index = 1; index < atomicPredicates.size(); index++) {
                    builder.addProbe(id, AnalysisManifest.ProbeKind.PREDICATE,
                            ownerHint(location.path()), runtimeMemberHint(condition));
                }
                builder.setBranchCompletions(id, exactBranchCompletions(condition));
                return id;
            }

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
                        source.treeKind(), description + " is outside the walking-skeleton analysis subset"));
            }

            private String caseLabel(CaseTree branch) {
                if (branch.getExpressions().isEmpty()) return "default";
                return branch.getExpressions().stream().map(StaticDecisionAnalyzer::expression)
                        .collect(Collectors.joining(" or "));
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
                return "derive " + subject + " as " + expression(initializer);
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
        if (unwrapped instanceof BinaryTree binary && binary.getKind() == Tree.Kind.CONDITIONAL_OR) {
            return "neither " + renderExpression(binary.getLeftOperand()) + " nor "
                    + renderExpression(binary.getRightOperand());
        }
        if (unwrapped instanceof BinaryTree binary && binary.getKind() == Tree.Kind.CONDITIONAL_AND) {
            return "not all of " + renderExpression(binary);
        }
        return "not " + renderExpression(unwrapped);
    }

    private static String renderBinary(BinaryTree binary) {
        if (isNullComparison(binary)) return renderNullComparison(binary, false);
        return renderExpression(binary.getLeftOperand()) + binaryOperator(binary.getKind())
                + renderExpression(binary.getRightOperand());
    }

    private static boolean isNullComparison(BinaryTree binary) {
        return (binary.getKind() == Tree.Kind.EQUAL_TO || binary.getKind() == Tree.Kind.NOT_EQUAL_TO)
                && (isNullLiteral(binary.getLeftOperand()) || isNullLiteral(binary.getRightOperand()));
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

    private static List<Tree> atomicPredicates(Tree tree) {
        Tree unwrapped = unwrapParentheses(tree);
        if (unwrapped instanceof UnaryTree unary && unary.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            return atomicPredicates(unary.getExpression());
        }
        if (unwrapped instanceof BinaryTree binary
                && (binary.getKind() == Tree.Kind.CONDITIONAL_AND
                || binary.getKind() == Tree.Kind.CONDITIONAL_OR)) {
            var predicates = new ArrayList<Tree>();
            predicates.addAll(atomicPredicates(binary.getLeftOperand()));
            predicates.addAll(atomicPredicates(binary.getRightOperand()));
            return List.copyOf(predicates);
        }
        return List.of(unwrapped);
    }

    private static List<AnalysisManifest.BranchCompletion> exactBranchCompletions(Tree tree) {
        Tree unwrapped = unwrapParentheses(tree);
        if (unwrapped instanceof UnaryTree unary && unary.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            return containsShortCircuit(unary.getExpression())
                    || containsUnsupportedBranching(unary.getExpression())
                    ? List.of()
                    : List.of(AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
        }
        if (!(unwrapped instanceof BinaryTree binary)
                || (binary.getKind() != Tree.Kind.CONDITIONAL_AND
                && binary.getKind() != Tree.Kind.CONDITIONAL_OR)) {
            return containsUnsupportedBranching(unwrapped)
                    ? List.of()
                    : List.of(AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
        }
        var operands = new ArrayList<Tree>();
        flattenShortCircuit(unwrapped, binary.getKind(), operands);
        if (operands.stream().anyMatch(operand -> containsShortCircuit(operand)
                || containsUnsupportedBranching(operand))) return List.of();
        var completions = new ArrayList<AnalysisManifest.BranchCompletion>();
        AnalysisManifest.BranchCompletion shortCircuit = binary.getKind() == Tree.Kind.CONDITIONAL_AND
                ? AnalysisManifest.BranchCompletion.JUMP_FALSE
                : AnalysisManifest.BranchCompletion.JUMP_TRUE;
        for (int index = 1; index < operands.size(); index++) completions.add(shortCircuit);
        completions.add(AnalysisManifest.BranchCompletion.BOTH_OUTCOMES);
        return List.copyOf(completions);
    }

    private static void flattenShortCircuit(Tree tree, Tree.Kind kind, List<Tree> operands) {
        Tree unwrapped = unwrapParentheses(tree);
        if (unwrapped instanceof BinaryTree binary && binary.getKind() == kind) {
            flattenShortCircuit(binary.getLeftOperand(), kind, operands);
            flattenShortCircuit(binary.getRightOperand(), kind, operands);
        } else {
            operands.add(unwrapped);
        }
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

    private static boolean containsUnsupportedBranching(Tree tree) {
        final boolean[] found = { false };
        new TreeScanner<Void, Void>() {
            @Override public Void scan(Tree candidate, Void unused) {
                if (candidate != null && (candidate.getKind() == Tree.Kind.CONDITIONAL_EXPRESSION
                        || candidate.getKind() == Tree.Kind.SWITCH_EXPRESSION)) {
                    found[0] = true;
                }
                return found[0] ? null : super.scan(candidate, unused);
            }
        }.scan(tree, null);
        return found[0];
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
        List<String> arguments = call.getArguments().stream().map(StaticDecisionAnalyzer::renderExpression).toList();
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
            case "get" -> "item";
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
        if (qualifiedName.startsWith("java.") || qualifiedName.startsWith("javax.")) return true;
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
        if (identifier.equals("i") || identifier.equals("idx")) return "index";
        if (identifier.length() == 1) return "item";
        return words(identifier);
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
