package fixtures.authorization;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.Set;

public final class RecordAuthorizationPolicy {
    record Actor(String employeeId) { }

    @FachTracing("actor may approve")
    public boolean mayApprove(Actor actor, Actor creator, Set<Actor> approvers) {
        return creator != null && approvers.contains(actor)
                && !actor.employeeId().equals(creator.employeeId());
    }
}
