package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks proved direct local reference identities in source order. */
final class LocalAliasResolver {
    private final Map<String, Resolution> aliases = new LinkedHashMap<>();

    void assign(String local, Tree value) {
        Resolution roots = identityRoots(value);
        if (roots.allRoots().isEmpty() || roots.equals(Resolution.proved(local))) aliases.remove(local);
        else aliases.put(local, roots);
    }

    Set<String> resolve(String name) {
        return resolution(name).allRoots();
    }

    Resolution resolution(String name) {
        return aliases.getOrDefault(name, Resolution.proved(name));
    }

    LocalAliasResolver copy() {
        var copy = new LocalAliasResolver();
        copy.aliases.putAll(aliases);
        return copy;
    }

    void replaceWith(LocalAliasResolver source) {
        aliases.clear();
        aliases.putAll(source.aliases);
    }

    static LocalAliasResolver merge(List<LocalAliasResolver> branches) {
        var merged = new LocalAliasResolver();
        if (branches.isEmpty()) return merged;
        var names = new LinkedHashSet<String>();
        branches.forEach(branch -> names.addAll(branch.aliases.keySet()));
        for (String name : names) {
            Set<String> proved = new LinkedHashSet<>(branches.getFirst().resolution(name).provedRoots());
            var reachable = new LinkedHashSet<String>();
            for (LocalAliasResolver branch : branches) {
                Resolution resolution = branch.resolution(name);
                proved.retainAll(resolution.provedRoots());
                reachable.addAll(resolution.allRoots());
            }
            reachable.removeAll(proved);
            Resolution resolution = new Resolution(proved, reachable);
            if (!resolution.equals(Resolution.proved(name))) merged.aliases.put(name, resolution);
        }
        return merged;
    }

    private Resolution identityRoots(Tree value) {
        return switch (value) {
            case IdentifierTree identifier -> resolution(identifier.getName().toString());
            case ParenthesizedTree parenthesized -> identityRoots(parenthesized.getExpression());
            case TypeCastTree cast -> identityRoots(cast.getExpression());
            case MemberSelectTree member -> {
                Set<String> roots = DependencyGraphBuilder.collectIdentifiers(member.getExpression());
                var proved = new LinkedHashSet<String>();
                var possible = new LinkedHashSet<String>();
                roots.forEach(root -> {
                    Resolution resolution = resolution(root);
                    proved.addAll(resolution.provedRoots());
                    possible.addAll(resolution.possibleRoots());
                });
                yield new Resolution(proved, possible);
            }
            default -> Resolution.none();
        };
    }

    record Resolution(Set<String> provedRoots, Set<String> possibleRoots) {
        Resolution {
            provedRoots = Set.copyOf(provedRoots);
            var possible = new LinkedHashSet<>(possibleRoots);
            possible.removeAll(provedRoots);
            possibleRoots = Set.copyOf(possible);
        }

        static Resolution proved(String root) {
            return new Resolution(Set.of(root), Set.of());
        }

        static Resolution none() {
            return new Resolution(Set.of(), Set.of());
        }

        Set<String> allRoots() {
            var roots = new LinkedHashSet<>(provedRoots);
            roots.addAll(possibleRoots);
            return Set.copyOf(roots);
        }
    }
}
