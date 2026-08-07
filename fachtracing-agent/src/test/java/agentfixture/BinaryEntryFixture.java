package agentfixture;

import at.gepardec.fachtracing.api.FachTracing;

public final class BinaryEntryFixture {
    @FachTracing("binary runtime decision")
    public boolean decide(int age) {
        return BinaryRule.accepts(age);
    }
}
