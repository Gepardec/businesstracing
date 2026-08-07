package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.Collections;
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
        return build(method, Set.of(), (call, definitions) -> callEffects.apply(call));
    }

    /** Extracts dependencies with attributed names that can identify caller-visible state. */
    public MethodDependencies build(
            MethodTree method,
            Set<String> attributedStateRoots,
            CallEffectResolver callEffects) {
        var definitions = new LocalDefinitionResolver();
        var identifiers = new IdentityHashMap<Tree, Set<String>>();
        var parents = new IdentityHashMap<Tree, Tree>();
        var returns = new ArrayList<ReturnTree>();
        var effectsByIdentifier = new LinkedHashMap<String, List<Tree>>();
        var possibleEffectsByIdentifier = new LinkedHashMap<String, List<Tree>>();
        var invocationUsesByDefinition = new IdentityHashMap<Tree, List<MethodInvocationTree>>();
        var aliases = new LocalAliasResolver();

        new TreeScanner<Void, Tree>() {
            @Override public Void scan(Tree tree, Tree parent) {
                if (tree != null && parent != null) parents.put(tree, parent);
                return super.scan(tree, tree);
            }

            @Override public Void visitVariable(VariableTree node, Tree parent) {
                if (node.getInitializer() != null) {
                    definitions.assign(node.getName().toString(), node.getInitializer());
                    aliases.assign(node.getName().toString(), node.getInitializer());
                }
                return super.visitVariable(node, parent);
            }

            @Override public Void visitAssignment(AssignmentTree node, Tree parent) {
                if (node.getVariable() instanceof IdentifierTree identifier) {
                    definitions.assign(identifier.getName().toString(), node.getExpression());
                    aliases.assign(identifier.getName().toString(), node.getExpression());
                }
                return super.visitAssignment(node, parent);
            }

            @Override public Void visitIf(IfTree node, Tree parent) {
                scan(node.getCondition(), node);
                LocalAliasResolver before = aliases.copy();
                LocalDefinitionResolver definitionsBefore = definitions.copy();
                scan(node.getThenStatement(), node);
                LocalAliasResolver thenState = aliases.copy();
                LocalDefinitionResolver thenDefinitions = definitions.copy();
                aliases.replaceWith(before);
                definitions.replaceWith(definitionsBefore);
                if (node.getElseStatement() != null) scan(node.getElseStatement(), node);
                LocalAliasResolver elseState = aliases.copy();
                LocalDefinitionResolver elseDefinitions = definitions.copy();
                aliases.replaceWith(LocalAliasResolver.merge(List.of(thenState, elseState)));
                definitions.replaceWith(LocalDefinitionResolver.mergeBranches(
                        definitionsBefore, thenDefinitions, elseDefinitions,
                        branchDependentAliases(method, attributedStateRoots,
                                definitionsBefore, thenState, elseState)));
                return null;
            }

            @Override public Void visitCompoundAssignment(CompoundAssignmentTree node, Tree parent) {
                if (node.getVariable() instanceof IdentifierTree identifier) {
                    definitions.assign(identifier.getName().toString(), node);
                }
                return super.visitCompoundAssignment(node, parent);
            }

            @Override public Void visitReturn(ReturnTree node, Tree parent) {
                returns.add(node);
                return super.visitReturn(node, parent);
            }

            @Override public Void visitMethodInvocation(MethodInvocationTree node, Tree parent) {
                Map<String, List<Tree>> activeDefinitions = definitions.snapshot();
                recordInvocationDefinitionUses(node, activeDefinitions, invocationUsesByDefinition);
                CallEffects effects = callEffects.resolve(node, activeDefinitions);
                effects.provenWrites().forEach(identifier -> {
                    LocalAliasResolver.Resolution resolution = aliases.resolution(identifier);
                    resolution.provedRoots().forEach(root -> effectsByIdentifier
                            .computeIfAbsent(root, ignored -> new ArrayList<>()).add(node));
                    resolution.possibleRoots().forEach(root -> possibleEffectsByIdentifier
                            .computeIfAbsent(root, ignored -> new ArrayList<>()).add(node));
                });
                effects.possibleWrites().forEach(identifier -> aliases.resolve(identifier).forEach(root ->
                        possibleEffectsByIdentifier.computeIfAbsent(root, ignored -> new ArrayList<>()).add(node)));
                return super.visitMethodInvocation(node, parent);
            }
        }.scan(method, null);

        Map<String, List<Tree>> activeDefinitions = definitions.snapshot();
        for (var alternatives : activeDefinitions.values()) {
            for (Tree definition : alternatives) {
                identifiers.put(definition, collectIdentifiers(definition));
            }
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
        return new MethodDependencies(method, returns, activeDefinitions, identifiers, parents,
                immutableEffects, immutablePossibleEffects, invocationUsesByDefinition);
    }

    private static void recordInvocationDefinitionUses(
            MethodInvocationTree invocation,
            Map<String, List<Tree>> definitions,
            Map<Tree, List<MethodInvocationTree>> invocationUsesByDefinition) {
        for (Tree argument : invocation.getArguments()) {
            Tree unwrapped = unwrap(argument);
            if (!(unwrapped instanceof IdentifierTree identifier)) continue;
            for (Tree definition : definitions.getOrDefault(identifier.getName().toString(), List.of())) {
                invocationUsesByDefinition.computeIfAbsent(definition, ignored -> new ArrayList<>())
                        .add(invocation);
            }
        }
    }

    private static Tree unwrap(Tree tree) {
        Tree current = tree;
        while (true) {
            if (current instanceof ParenthesizedTree parenthesized) {
                current = parenthesized.getExpression();
            } else if (current instanceof TypeCastTree cast) {
                current = cast.getExpression();
            } else {
                return current;
            }
        }
    }

    private static Set<String> branchDependentAliases(
            MethodTree method,
            Set<String> attributedStateRoots,
            LocalDefinitionResolver before,
            LocalAliasResolver thenState,
            LocalAliasResolver elseState) {
        var knownStateRoots = new LinkedHashSet<>(before.snapshot().keySet());
        method.getParameters().forEach(parameter -> knownStateRoots.add(parameter.getName().toString()));
        knownStateRoots.add("this");
        knownStateRoots.add("super");
        knownStateRoots.addAll(attributedStateRoots);
        var aliases = new LinkedHashSet<>(LocalAliasResolver.changedBindings(thenState, elseState));
        aliases.removeIf(alias -> {
            var roots = new LinkedHashSet<>(thenState.resolution(alias).allRoots());
            roots.addAll(elseState.resolution(alias).allRoots());
            roots.remove(alias);
            return Collections.disjoint(roots, knownStateRoots);
        });
        return Set.copyOf(aliases);
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

    /** Resolves call effects with the local definitions that are active at the call site. */
    @FunctionalInterface
    public interface CallEffectResolver {
        /** Returns proven and possible writes for one call. */
        CallEffects resolve(MethodInvocationTree call, Map<String, List<Tree>> activeDefinitions);
    }

    /** Compiler-tree dependence input for backward slicing. */
    public record MethodDependencies(
            MethodTree method,
            List<ReturnTree> returns,
            Map<String, List<Tree>> definitions,
            Map<Tree, Set<String>> identifierUses,
            Map<Tree, Tree> parents,
            Map<String, List<Tree>> effectsByIdentifier,
            Map<String, List<Tree>> possibleEffectsByIdentifier,
            Map<Tree, List<MethodInvocationTree>> invocationUsesByDefinition) {
        /** Creates defensive collections while retaining tree object identity. */
        public MethodDependencies {
            returns = List.copyOf(returns);
            var immutableDefinitions = new LinkedHashMap<String, List<Tree>>();
            definitions.forEach((name, alternatives) ->
                    immutableDefinitions.put(name, List.copyOf(alternatives)));
            definitions = Map.copyOf(immutableDefinitions);
            identifierUses = Map.copyOf(identifierUses);
            parents = Map.copyOf(parents);
            effectsByIdentifier = Map.copyOf(effectsByIdentifier);
            possibleEffectsByIdentifier = Map.copyOf(possibleEffectsByIdentifier);
            var immutableUses = new IdentityHashMap<Tree, List<MethodInvocationTree>>();
            invocationUsesByDefinition.forEach((definition, uses) ->
                    immutableUses.put(definition, List.copyOf(uses)));
            invocationUsesByDefinition = Collections.unmodifiableMap(immutableUses);
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
