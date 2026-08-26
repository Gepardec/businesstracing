package fixtures.employment;

import at.gepardec.fachtracing.api.FachTracing;

import java.time.LocalDate;
import java.util.List;

public final class EmploymentEligibilityPolicy {
    @FachTracing("employment eligibility")
    public boolean isEligible(String person, List<Employment> employments, LocalDate referenceDate) {
        return employments.stream().anyMatch(employment -> employment.covers(referenceDate));
    }

    public record Employment(LocalDate from, LocalDate until) {
        public boolean covers(LocalDate referenceDate) {
            return !from.isAfter(referenceDate) && (until == null || !referenceDate.isAfter(until));
        }
    }
}
