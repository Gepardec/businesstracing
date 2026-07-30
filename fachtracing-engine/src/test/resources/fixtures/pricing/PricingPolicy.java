package fixtures.pricing;

import at.gepardec.fachtracing.api.FachTracing;

public final class PricingPolicy {
    @FachTracing("order price")
    public int decide(int quantity, int unitPrice, CustomerGroup group) {
        int subtotal = quantity * unitPrice + fixedAdjustment();
        return applyDiscount(subtotal, group);
    }

    private int fixedAdjustment() {
        return 2 * 3;
    }

    private int applyDiscount(int subtotal, CustomerGroup group) {
        if (group == CustomerGroup.PREFERRED) {
            return subtotal * 90 / 100;
        }
        return subtotal;
    }

    public enum CustomerGroup { STANDARD, PREFERRED }
}
