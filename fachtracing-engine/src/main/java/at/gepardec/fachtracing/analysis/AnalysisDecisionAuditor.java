package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Finds graph-eligible source constructs that cannot affect the sliced result. */
final class AnalysisDecisionAuditor {
    private AnalysisDecisionAuditor() { }

    /** Returns the first excluded construct in each excluded source subtree. */
    static List<Tree> excludedConstructs(
            MethodTree method,
            DependencyGraphBuilder.MethodDependencies dependencies,
            Set<Tree> slice) {
        if (method.getBody() == null) return List.of();
        var excluded = new ArrayList<Tree>();
        new TreeScanner<Void, Void>() {
            @Override public Void scan(Tree tree, Void unused) {
                if (tree == null) return null;
                if (graphEligible(tree)
                        && !DecisionRelevance.isRelevant(tree, slice, dependencies)) {
                    excluded.add(tree);
                    return null;
                }
                return super.scan(tree, unused);
            }
        }.scan(method.getBody(), null);
        return List.copyOf(excluded);
    }

    private static boolean graphEligible(Tree tree) {
        return switch (tree.getKind()) {
            case IF, SWITCH, SWITCH_EXPRESSION, VARIABLE, ASSIGNMENT,
                    MULTIPLY_ASSIGNMENT, DIVIDE_ASSIGNMENT, REMAINDER_ASSIGNMENT,
                    PLUS_ASSIGNMENT, MINUS_ASSIGNMENT, LEFT_SHIFT_ASSIGNMENT,
                    RIGHT_SHIFT_ASSIGNMENT, UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
                    AND_ASSIGNMENT, XOR_ASSIGNMENT, OR_ASSIGNMENT,
                    CONDITIONAL_EXPRESSION, METHOD_INVOCATION, LAMBDA_EXPRESSION,
                    MEMBER_REFERENCE, THROW, FOR_LOOP, ENHANCED_FOR_LOOP,
                    WHILE_LOOP, DO_WHILE_LOOP, TRY, SYNCHRONIZED -> true;
            default -> false;
        };
    }
}
