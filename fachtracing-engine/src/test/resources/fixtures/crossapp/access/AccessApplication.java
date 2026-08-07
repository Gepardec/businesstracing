package fixtures.crossapp.access;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.HashSet;
import java.util.Set;

public final class AccessApplication {
    @FachTracing("grant role")
    public boolean grant(boolean allowed) {
        Set<Role> set = new HashSet<>();
        if (allowed) {
            set.add(new Role("reader"));
        }
        return set.isEmpty();
    }

    private record Role(String name) { }
}
