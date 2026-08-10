package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.IfTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.ThrowTree;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Computes the domain-neutral backward slice that can influence a method result. */
public final class BackwardDecisionSlicer {
    /** Returns result expressions, their definitions, and enclosing control decisions. */
    public Set<Tree> slice(DependencyGraphBuilder.MethodDependencies dependencies) {
        return slice(dependencies, Set.of());
    }

    /** Returns the result slice plus writes to caller-relevant state roots. */
    public Set<Tree> slice(
            DependencyGraphBuilder.MethodDependencies dependencies,
            Set<String> effectRoots) {
        return slice(dependencies, effectRoots, Set.of());
    }

    /** Returns the result slice and omits throws handled by compatible local catches. */
    public Set<Tree> slice(
            DependencyGraphBuilder.MethodDependencies dependencies,
            Set<String> effectRoots,
            Set<ThrowTree> locallyCaughtThrows) {
        Set<Tree> relevant = Collections.newSetFromMap(new IdentityHashMap<>());
        var pendingNames = new ArrayDeque<PendingName>();
        effectRoots.forEach(name -> pendingNames.add(new PendingName(name, dependencies.method())));

        for (ReturnTree returned : dependencies.returns()) {
            relevant.add(returned);
            if (returned.getExpression() != null) {
                relevant.add(returned.getExpression());
                enqueueIdentifiers(returned.getExpression(), pendingNames);
                addControlAncestors(returned, dependencies, relevant, pendingNames);
            }
        }

        for (ThrowTree thrown : dependencies.throwStatements()) {
            if (locallyCaughtThrows.contains(thrown)) continue;
            relevant.add(thrown);
            if (thrown.getExpression() != null) {
                relevant.add(thrown.getExpression());
                enqueueIdentifiers(thrown.getExpression(), pendingNames);
            }
            addControlAncestors(thrown, dependencies, relevant, pendingNames);
        }

        Map<Tree, Set<String>> expandedAtUse = new IdentityHashMap<>();
        var expandedEffects = new java.util.HashSet<String>();
        while (!pendingNames.isEmpty()) {
            PendingName pending = pendingNames.removeFirst();
            if (!expandedAtUse.computeIfAbsent(pending.use(), ignored -> new java.util.HashSet<>())
                    .add(pending.name())) continue;
            Map<String, List<Tree>> definitions = dependencies.reachingDefinitions()
                    .getOrDefault(pending.use(), Map.of());
            for (Tree definition : definitions.getOrDefault(pending.name(), List.of())) {
                relevant.add(definition);
                enqueueIdentifiers(definition, pendingNames);
                addControlAncestors(definition, dependencies, relevant, pendingNames);
            }
            if (!expandedEffects.add(pending.name())) continue;
            for (Tree effect : dependencies.effectsByIdentifier().getOrDefault(pending.name(), List.of())) {
                if (!relevant.add(effect)) continue;
                enqueueIdentifiers(effect, pendingNames);
                addControlAncestors(effect, dependencies, relevant, pendingNames);
            }
        }
        return Collections.unmodifiableSet(relevant);
    }

    private static void addControlAncestors(
            Tree child,
            DependencyGraphBuilder.MethodDependencies dependencies,
            Set<Tree> relevant,
            ArrayDeque<PendingName> pendingNames) {
        Tree current = dependencies.parents().get(child);
        while (current != null && current != dependencies.method()) {
            Tree condition = switch (current) {
                case IfTree decision -> decision.getCondition();
                case SwitchTree choice -> choice.getExpression();
                case SwitchExpressionTree choice -> choice.getExpression();
                default -> null;
            };
            if (condition != null) {
                relevant.add(current);
                relevant.add(condition);
                enqueueIdentifiers(condition, pendingNames);
            }
            current = dependencies.parents().get(current);
        }
    }

    private static void enqueueIdentifiers(Tree use, ArrayDeque<PendingName> pendingNames) {
        DependencyGraphBuilder.collectIdentifiers(use).forEach(name ->
                pendingNames.add(new PendingName(name, use)));
    }

    private record PendingName(String name, Tree use) { }
}
