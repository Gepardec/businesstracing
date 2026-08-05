package fixtures.loops;

import at.gepardec.fachtracing.api.FachTracing;
import java.util.List;

final class IndexedEntryPolicy {
    @FachTracing("indexed entry decision")
    boolean decide(List<Entry> entries) {
        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            if (entry.age() < 24) {
                return true;
            }
            if (index + 1 < entries.size() && entries.get(index + 1).city().equals("Vienna")) {
                return true;
            }
        }
        return false;
    }

    record Entry(int age, String city) { }
}
