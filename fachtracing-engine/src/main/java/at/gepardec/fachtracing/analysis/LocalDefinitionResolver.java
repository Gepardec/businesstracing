package at.gepardec.fachtracing.analysis;

import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks active local definitions in source order and across branch joins. */
final class LocalDefinitionResolver {
    private final Map<String, List<Tree>> definitions = new LinkedHashMap<>();

    void assign(String local, Tree definition) {
        definitions.put(local, List.of(definition));
    }

    LocalDefinitionResolver copy() {
        var copy = new LocalDefinitionResolver();
        copy.definitions.putAll(definitions);
        return copy;
    }

    void replaceWith(LocalDefinitionResolver source) {
        definitions.clear();
        definitions.putAll(source.definitions);
    }

    static LocalDefinitionResolver mergeBranches(
            LocalDefinitionResolver before,
            LocalDefinitionResolver thenBranch,
            LocalDefinitionResolver elseBranch,
            Set<String> branchDependentAliases) {
        var merged = new LocalDefinitionResolver();
        var names = new LinkedHashSet<String>();
        names.addAll(before.definitions.keySet());
        names.addAll(thenBranch.definitions.keySet());
        names.addAll(elseBranch.definitions.keySet());
        for (String name : names) {
            if (!branchDependentAliases.contains(name)) {
                List<Tree> selected = sameDefinitions(
                        before.definitions.getOrDefault(name, List.of()),
                        elseBranch.definitions.getOrDefault(name, List.of()))
                        ? thenBranch.definitions.getOrDefault(name, List.of())
                        : elseBranch.definitions.getOrDefault(name, List.of());
                if (!selected.isEmpty()) merged.definitions.put(name, selected);
                continue;
            }
            var reachable = new ArrayList<Tree>();
            for (Tree definition : thenBranch.definitions.getOrDefault(name, List.of())) {
                reachable.add(definition);
            }
            for (Tree definition : elseBranch.definitions.getOrDefault(name, List.of())) {
                if (reachable.stream().noneMatch(existing -> existing == definition)) {
                    reachable.add(definition);
                }
            }
            if (!reachable.isEmpty()) merged.definitions.put(name, List.copyOf(reachable));
        }
        return merged;
    }

    private static boolean sameDefinitions(List<Tree> first, List<Tree> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            if (first.get(index) != second.get(index)) return false;
        }
        return true;
    }

    Map<String, List<Tree>> snapshot() {
        return Map.copyOf(definitions);
    }
}
