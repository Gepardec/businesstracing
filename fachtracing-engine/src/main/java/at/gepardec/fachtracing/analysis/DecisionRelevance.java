package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import java.util.Set;

/** Applies the result-slice relevance policy to one Java syntax tree. */
final class DecisionRelevance {
    private DecisionRelevance() { }

    /** Tests direct, containing, and expression-bounded relevance. */
    static boolean isRelevant(
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
        Tree current = dependencies.parents().get(tree);
        while (current != null && current != dependencies.method()) {
            if (current instanceof ExpressionTree && slice.contains(current)) return true;
            current = dependencies.parents().get(current);
        }
        return false;
    }
}
