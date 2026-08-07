package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.IfTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
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
        Set<Tree> relevant = Collections.newSetFromMap(new IdentityHashMap<>());
        var pendingNames = new ArrayDeque<String>();
        pendingNames.addAll(effectRoots);

        for (ReturnTree returned : dependencies.returns()) {
            relevant.add(returned);
            if (returned.getExpression() != null) {
                relevant.add(returned.getExpression());
                pendingNames.addAll(DependencyGraphBuilder.collectIdentifiers(returned.getExpression()));
                addControlAncestors(returned, dependencies, relevant, pendingNames);
            }
        }

        var expandedNames = new java.util.HashSet<String>();
        while (!pendingNames.isEmpty()) {
            String name = pendingNames.removeFirst();
            if (!expandedNames.add(name)) continue;
            for (Tree definition : dependencies.definitions().getOrDefault(name, java.util.List.of())) {
                relevant.add(definition);
                pendingNames.addAll(DependencyGraphBuilder.collectIdentifiers(definition));
                addControlAncestors(definition, dependencies, relevant, pendingNames);
            }
            for (Tree effect : dependencies.effectsByIdentifier().getOrDefault(name, java.util.List.of())) {
                if (!relevant.add(effect)) continue;
                pendingNames.addAll(DependencyGraphBuilder.collectIdentifiers(effect));
                addControlAncestors(effect, dependencies, relevant, pendingNames);
            }
        }
        return Collections.unmodifiableSet(relevant);
    }

    private static void addControlAncestors(
            Tree child,
            DependencyGraphBuilder.MethodDependencies dependencies,
            Set<Tree> relevant,
            ArrayDeque<String> pendingNames) {
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
                pendingNames.addAll(DependencyGraphBuilder.collectIdentifiers(condition));
            }
            current = dependencies.parents().get(current);
        }
    }
}
