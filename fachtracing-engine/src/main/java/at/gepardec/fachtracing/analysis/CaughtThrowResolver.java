package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.UnionType;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds source throws that a compatible local catch handles. */
final class CaughtThrowResolver {
    private CaughtThrowResolver() { }

    /** Returns throws caught inside the current method. */
    static Set<ThrowTree> resolve(
            TreePath methodPath,
            List<ThrowTree> throwsInMethod,
            Map<Tree, Tree> parents,
            Trees trees,
            Types types) {
        Set<ThrowTree> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ThrowTree thrown : throwsInMethod) {
            TreePath expressionPath = TreePath.getPath(methodPath, thrown.getExpression());
            TypeMirror thrownType = expressionPath == null ? null : trees.getTypeMirror(expressionPath);
            if (!usable(thrownType)) continue;
            if (hasCompatibleCatch(thrown, thrownType, parents, methodPath, trees, types)) {
                result.add(thrown);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static boolean hasCompatibleCatch(
            ThrowTree thrown,
            TypeMirror thrownType,
            Map<Tree, Tree> parents,
            TreePath methodPath,
            Trees trees,
            Types types) {
        Tree child = thrown;
        Tree current = parents.get(child);
        while (current != null && current != methodPath.getLeaf()) {
            if (current instanceof TryTree guarded
                    && (child == guarded.getBlock() || guarded.getResources().contains(child))) {
                for (CatchTree caught : guarded.getCatches()) {
                    TreePath catchPath = TreePath.getPath(methodPath, caught.getParameter().getType());
                    TypeMirror catchType = catchPath == null ? null : trees.getTypeMirror(catchPath);
                    if (accepts(thrownType, catchType, types)) return true;
                }
            }
            child = current;
            current = parents.get(current);
        }
        return false;
    }

    private static boolean accepts(TypeMirror thrownType, TypeMirror catchType, Types types) {
        if (!usable(catchType)) return false;
        if (catchType instanceof UnionType union) {
            return union.getAlternatives().stream().anyMatch(alternative ->
                    types.isAssignable(thrownType, alternative));
        }
        return types.isAssignable(thrownType, catchType);
    }

    private static boolean usable(TypeMirror type) {
        return type != null && type.getKind() != TypeKind.ERROR && type.getKind() != TypeKind.NONE;
    }
}
