package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Indexes the local definitions that can reach each source tree. */
final class ReachingDefinitionIndex {
    private ReachingDefinitionIndex() { }

    /** Builds immutable use-site definition snapshots for one method. */
    static Map<Tree, Map<String, List<Tree>>> build(
            MethodTree method,
            Set<String> assignedNames,
            Map<String, List<Tree>> nonLocalDefinitions) {
        if (method.getBody() == null) return Map.of();
        return new Builder(method, assignedNames, nonLocalDefinitions).build();
    }

    private static final class Builder extends TreeScanner<Void, Void> {
        private final MethodTree method;
        private final Set<String> assignedNames;
        private final Map<String, List<Tree>> nonLocalDefinitions;
        private final Set<Tree> initializers = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<Tree, Map<String, List<Tree>>> definitionsBefore = new IdentityHashMap<>();
        private State state = new State();

        private Builder(
                MethodTree method,
                Set<String> assignedNames,
                Map<String, List<Tree>> nonLocalDefinitions) {
            this.method = method;
            this.assignedNames = Set.copyOf(assignedNames);
            this.nonLocalDefinitions = Map.copyOf(nonLocalDefinitions);
        }

        private Map<Tree, Map<String, List<Tree>>> build() {
            scan(method.getBody(), null);
            definitionsBefore.put(method, snapshot());
            return Collections.unmodifiableMap(new IdentityHashMap<>(definitionsBefore));
        }

        @Override public Void scan(Tree tree, Void unused) {
            if (tree == null) return null;
            definitionsBefore.put(tree, snapshot());
            return super.scan(tree, unused);
        }

        @Override public Void visitVariable(VariableTree node, Void unused) {
            scan(node.getModifiers(), unused);
            scan(node.getType(), unused);
            scan(node.getInitializer(), unused);
            if (state.reachable() && node.getInitializer() != null) {
                initializers.add(node.getInitializer());
                state.assign(node.getName().toString(), node.getInitializer());
            }
            return null;
        }

        @Override public Void visitAssignment(AssignmentTree node, Void unused) {
            scan(node.getVariable(), unused);
            scan(node.getExpression(), unused);
            if (state.reachable() && node.getVariable() instanceof IdentifierTree identifier) {
                state.assign(identifier.getName().toString(), node.getExpression());
            }
            return null;
        }

        @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
            scan(node.getVariable(), unused);
            scan(node.getExpression(), unused);
            if (state.reachable() && node.getVariable() instanceof IdentifierTree identifier) {
                state.assign(identifier.getName().toString(), node);
            }
            return null;
        }

        @Override public Void visitIf(IfTree node, Void unused) {
            scan(node.getCondition(), unused);
            State entry = state.copy();
            state = entry.copy();
            scan(node.getThenStatement(), unused);
            State thenState = state.copy();
            state = entry.copy();
            scan(node.getElseStatement(), unused);
            state = State.merge(List.of(thenState, state));
            return null;
        }

        @Override public Void visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
            scan(node.getCondition(), unused);
            State entry = state.copy();
            state = entry.copy();
            scan(node.getTrueExpression(), unused);
            State trueState = state.copy();
            state = entry.copy();
            scan(node.getFalseExpression(), unused);
            state = State.merge(List.of(trueState, state));
            return null;
        }

        @Override public Void visitSwitch(SwitchTree node, Void unused) {
            scan(node.getExpression(), unused);
            scanAlternatives(node.getCases());
            return null;
        }

        @Override public Void visitSwitchExpression(SwitchExpressionTree node, Void unused) {
            scan(node.getExpression(), unused);
            scanAlternatives(node.getCases());
            return null;
        }

        private void scanAlternatives(List<? extends CaseTree> cases) {
            State entry = state.copy();
            var alternatives = new ArrayList<State>();
            alternatives.add(entry);
            for (CaseTree branch : cases) {
                state = entry.copy();
                scan(branch, null);
                alternatives.add(state.copy());
            }
            state = State.merge(alternatives);
        }

        @Override public Void visitForLoop(ForLoopTree node, Void unused) {
            scan(node.getInitializer(), unused);
            scan(node.getCondition(), unused);
            State entry = state.copy();
            state = entry.copy();
            scan(node.getStatement(), unused);
            scan(node.getUpdate(), unused);
            state = State.merge(List.of(entry, state));
            return null;
        }

        @Override public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
            scan(node.getExpression(), unused);
            State entry = state.copy();
            state = entry.copy();
            scan(node.getVariable(), unused);
            scan(node.getStatement(), unused);
            state = State.merge(List.of(entry, state));
            return null;
        }

        @Override public Void visitWhileLoop(WhileLoopTree node, Void unused) {
            scan(node.getCondition(), unused);
            State entry = state.copy();
            state = entry.copy();
            scan(node.getStatement(), unused);
            state = State.merge(List.of(entry, state));
            return null;
        }

        @Override public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
            State entry = state.copy();
            scan(node.getStatement(), unused);
            scan(node.getCondition(), unused);
            state = State.merge(List.of(entry, state));
            return null;
        }

        @Override public Void visitTry(TryTree node, Void unused) {
            scan(node.getResources(), unused);
            State entry = state.copy();
            state = entry.copy();
            scan(node.getBlock(), unused);
            var alternatives = new ArrayList<State>();
            alternatives.add(state.copy());
            node.getCatches().forEach(caught -> {
                state = entry.copy();
                scan(caught, unused);
                alternatives.add(state.copy());
            });
            state = State.merge(alternatives);
            scan(node.getFinallyBlock(), unused);
            return null;
        }

        @Override public Void visitReturn(ReturnTree node, Void unused) {
            scan(node.getExpression(), unused);
            state = state.unreachable();
            return null;
        }

        @Override public Void visitThrow(ThrowTree node, Void unused) {
            scan(node.getExpression(), unused);
            state = state.unreachable();
            return null;
        }

        @Override public Void visitLambdaExpression(LambdaExpressionTree node, Void unused) {
            State outer = state.copy();
            scan(node.getParameters(), unused);
            scan(node.getBody(), unused);
            state = outer;
            return null;
        }

        private Map<String, List<Tree>> snapshot() {
            if (!state.reachable()) return Map.of();
            var result = new LinkedHashMap<String, List<Tree>>();
            state.definitions().forEach((name, definitions) -> {
                List<Tree> visible = definitions.stream()
                        .filter(definition -> !assignedNames.contains(name) || !initializers.contains(definition))
                        .toList();
                if (!visible.isEmpty()) result.put(name, visible);
            });
            nonLocalDefinitions.forEach(result::put);
            return Collections.unmodifiableMap(result);
        }
    }

    private record State(Map<String, LinkedHashSet<Tree>> definitions, boolean reachable) {
        private State() {
            this(new LinkedHashMap<>(), true);
        }

        private State {
            var copy = new LinkedHashMap<String, LinkedHashSet<Tree>>();
            definitions.forEach((name, values) -> copy.put(name, new LinkedHashSet<>(values)));
            definitions = copy;
        }

        private void assign(String name, Tree definition) {
            definitions.put(name, new LinkedHashSet<>(List.of(definition)));
        }

        private State copy() {
            return new State(definitions, reachable);
        }

        private State unreachable() {
            return new State(definitions, false);
        }

        private static State merge(List<State> alternatives) {
            var merged = new LinkedHashMap<String, LinkedHashSet<Tree>>();
            boolean anyReachable = false;
            for (State alternative : alternatives) {
                if (!alternative.reachable()) continue;
                anyReachable = true;
                alternative.definitions().forEach((name, values) ->
                        merged.computeIfAbsent(name, ignored -> new LinkedHashSet<>()).addAll(values));
            }
            return new State(merged, anyReachable);
        }
    }
}
