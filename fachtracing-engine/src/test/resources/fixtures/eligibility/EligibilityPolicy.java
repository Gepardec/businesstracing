package fixtures.eligibility;

import at.gepardec.fachtracing.api.FachTracing;

public final class EligibilityPolicy {
    @FachTracing("customer eligibility")
    public boolean decide(int age, String location, boolean accountActive) {
        String diagnosticOnly = System.getProperty("eligibility.diagnostic", "off");
        if (age < 24 && location.equals("Vienna")) {
            return accountActive;
        }
        return false;
    }
}
