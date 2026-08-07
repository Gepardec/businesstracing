package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Tracks proved direct local reference identities in source order. */
final class LocalAliasResolver {
    private final Map<String, Set<String>> aliases = new LinkedHashMap<>();

    void assign(String local, Tree value) {
        Set<String> roots = identityRoots(value);
        if (roots.isEmpty() || roots.equals(Set.of(local))) aliases.remove(local);
        else aliases.put(local, roots);
    }

    Set<String> resolve(String name) {
        var result = new LinkedHashSet<String>();
        resolve(name, result, new LinkedHashSet<>());
        return Set.copyOf(result);
    }

    private void resolve(String name, Set<String> result, Set<String> visiting) {
        if (!visiting.add(name)) return;
        Set<String> roots = aliases.get(name);
        if (roots == null || roots.isEmpty()) result.add(name);
        else roots.forEach(root -> resolve(root, result, visiting));
        visiting.remove(name);
    }

    private Set<String> identityRoots(Tree value) {
        return switch (value) {
            case IdentifierTree identifier -> resolve(identifier.getName().toString());
            case ParenthesizedTree parenthesized -> identityRoots(parenthesized.getExpression());
            case TypeCastTree cast -> identityRoots(cast.getExpression());
            case MemberSelectTree member -> {
                Set<String> roots = DependencyGraphBuilder.collectIdentifiers(member.getExpression());
                var resolved = new LinkedHashSet<String>();
                roots.forEach(root -> resolved.addAll(resolve(root)));
                yield Set.copyOf(resolved);
            }
            default -> Set.of();
        };
    }
}
