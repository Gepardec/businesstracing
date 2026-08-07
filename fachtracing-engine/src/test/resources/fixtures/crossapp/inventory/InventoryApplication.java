package fixtures.crossapp.inventory;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.concurrent.atomic.AtomicIntegerArray;

public final class InventoryApplication {
    @FachTracing("reserve inventory")
    public boolean reserve(boolean required, int amount) {
        AtomicIntegerArray slots = new AtomicIntegerArray(1);
        if (required) {
            slots.set(0, amount);
        }
        return slots.get(0) > 0;
    }
}
