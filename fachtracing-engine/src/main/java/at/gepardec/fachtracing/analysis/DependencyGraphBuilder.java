package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Builds result data dependencies without using names or package classifications. */
public final class DependencyGraphBuilder {
    /** Extracts return sinks, local definitions, identifier uses, and parent relationships. */
    public MethodDependencies build(
            MethodTree method,
            Function<MethodInvocationTree, CallEffects> callEffects) {
        var definitions = new LinkedHashMap<String, Tree>();
        var identifiers = new IdentityHashMap<Tree, Set<String>>();
        var parents = new IdentityHashMap<Tree, Tree>();
        var returns = new ArrayList<ReturnTree>();
        var effectsByIdentifier = new LinkedHashMap<String, List<Tree>>();
        var possibleEffectsByIdentifier = new LinkedHashMap<String, List<Tree>>();
        var aliases = new LocalAliasResolver();

        new TreeScanner<Void, Tree>() {
            @Override public Void scan(Tree tree, Tree parent) {
                if (tree != null && parent != null) parents.put(tree, parent);
                return super.scan(tree, tree);
            }

            @Override public Void visitVariable(VariableTree node, Tree parent) {
                if (node.getInitializer() != null) {
                    definitions.put(node.getName().toString(), node.getInitializer());
                    aliases.assign(node.getName().toString(), node.getInitializer());
                }
                return super.visitVariable(node, parent);
            }

            @Override public Void visitAssignment(AssignmentTree node, Tree parent) {
                if (node.getVariable() instanceof IdentifierTree identifier) {
                    definitions.put(identifier.getName().toString(), node.getExpression());
                    aliases.assign(identifier.getName().toString(), node.getExpression());
                }
                return super.visitAssignment(node, parent);
            }

            @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Tree parent) {
                if (node.getVariable() instanceof IdentifierTree identifier) {
                    definitions.put(identifier.getName().toString(), node);
                }
                return super.visitCompoundAssignment(node, parent);
            }

            @Override public Void visitReturn(ReturnTree node, Tree parent) {
                returns.add(node);
                return super.visitReturn(node, parent);
            }

            @Override public Void visitMethodInvocation(MethodInvocationTree node, Tree parent) {
                CallEffects effects = callEffects.apply(node);
                effects.provenWrites().forEach(identifier -> aliases.resolve(identifier).forEach(root ->
                        effectsByIdentifier.computeIfAbsent(root, ignored -> new ArrayList<>()).add(node)));
                effects.possibleWrites().forEach(identifier -> aliases.resolve(identifier).forEach(root ->
                        possibleEffectsByIdentifier.computeIfAbsent(root, ignored -> new ArrayList<>()).add(node)));
                return super.visitMethodInvocation(node, parent);
            }
        }.scan(method, null);

        for (var definition : definitions.values()) {
            identifiers.put(definition, collectIdentifiers(definition));
        }
        for (var returned : returns) {
            if (returned.getExpression() != null) {
                identifiers.put(returned.getExpression(), collectIdentifiers(returned.getExpression()));
            }
        }

        Map<String, List<Tree>> immutableEffects = new LinkedHashMap<>();
        effectsByIdentifier.forEach((name, effects) -> immutableEffects.put(name, List.copyOf(effects)));
        Map<String, List<Tree>> immutablePossibleEffects = new LinkedHashMap<>();
        possibleEffectsByIdentifier.forEach((name, effects) ->
                immutablePossibleEffects.put(name, List.copyOf(effects)));
        return new MethodDependencies(method, returns, definitions, identifiers, parents,
                immutableEffects, immutablePossibleEffects);
    }

    /** Classified writes for one attributed call. */
    public record CallEffects(Set<String> provenWrites, Set<String> possibleWrites) {
        /** Creates immutable disjoint write sets. */
        public CallEffects {
            provenWrites = Set.copyOf(provenWrites);
            var possible = new LinkedHashSet<>(possibleWrites);
            possible.removeAll(provenWrites);
            possibleWrites = Set.copyOf(possible);
        }

        /** Returns a call with no effect on caller-visible state. */
        public static CallEffects none() { return new CallEffects(Set.of(), Set.of()); }
    }

    /** Compiler-tree dependence input for backward slicing. */
    public record MethodDependencies(
            MethodTree method,
            List<ReturnTree> returns,
            Map<String, Tree> definitions,
            Map<Tree, Set<String>> identifierUses,
            Map<Tree, Tree> parents,
            Map<String, List<Tree>> effectsByIdentifier,
            Map<String, List<Tree>> possibleEffectsByIdentifier) {
        /** Creates defensive collections while retaining tree object identity. */
        public MethodDependencies {
            returns = List.copyOf(returns);
            definitions = Map.copyOf(definitions);
            identifierUses = Map.copyOf(identifierUses);
            parents = Map.copyOf(parents);
            effectsByIdentifier = Map.copyOf(effectsByIdentifier);
            possibleEffectsByIdentifier = Map.copyOf(possibleEffectsByIdentifier);
        }
    }

    static Set<String> collectIdentifiers(Tree tree) {
        var result = new LinkedHashSet<String>();
        new TreeScanner<Void, Void>() {
            @Override public Void visitIdentifier(IdentifierTree node, Void unused) {
                result.add(node.getName().toString());
                return super.visitIdentifier(node, unused);
            }
        }.scan(tree, null);
        return result;
    }
}
