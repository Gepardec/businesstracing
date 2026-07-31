package example;

import at.gepardec.fachtracing.api.FachTracing;

public final class ApprovalPolicy {
    @FachTracing("approve application")
    public String approve(int age, String location) {
        if (age < 24 && location.equals("Vienna")) return "manual review";
        return "approved";
    }
}
