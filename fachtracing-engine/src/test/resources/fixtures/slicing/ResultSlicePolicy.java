package fixtures.slicing;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;

public final class ResultSlicePolicy {
    enum State { OPEN, CLOSED }

    interface UnknownMutator {
        void update(Customer customer);
    }

    record Customer(boolean eligible) { }

    static final class AuditValidator {
        boolean validate(int value) {
            return value > 0;
        }
    }

    static final class FraudValidator {
        boolean validate(int amount) {
            return amount < 1_000;
        }
    }

    static final class CreditValidator {
        boolean validate(int amount) {
            return amount >= 100;
        }
    }

    @FachTracing("ignored audit call")
    public boolean ignoredAudit(int age) {
        AuditValidator auditValidator = new AuditValidator();
        auditValidator.validate(age);
        return age >= 24;
    }

    @FachTracing("irrelevant branch work")
    public boolean irrelevantBranchWork(int age) {
        if (age < 18) {
            recordAudit(age);
            return false;
        }
        return meetsMinimumAge(age);
    }

    @FachTracing("read-only enum queries")
    public boolean readOnlyEnumQueries(State state) {
        state.name();
        state.equals(State.CLOSED);
        return state == State.OPEN;
    }

    @FachTracing("branch definitions and failure")
    public boolean branchDefinitionsAndFailure(State state, boolean override) {
        boolean allowed = false;
        switch (state) {
            case OPEN:
                if (override) {
                    allowed = true;
                }
                break;
            case CLOSED:
                allowed = false;
                break;
            default:
                throw new IllegalArgumentException(state.name());
        }
        return allowed;
    }

    private void recordAudit(int age) {
        int ignoredAge = age + 1;
    }

    private boolean meetsMinimumAge(int age) {
        return age >= 24;
    }

    @FachTracing("unknown customer effect")
    public boolean unknownEffect(UnknownMutator mutator, Customer customer) {
        mutator.update(customer);
        return customer.eligible();
    }

    @FachTracing("distinct validation roles")
    public boolean validationRoles(int amount) {
        FraudValidator fraudValidator = new FraudValidator();
        CreditValidator creditValidator = new CreditValidator();
        return fraudValidator.validate(amount) && creditValidator.validate(amount);
    }

    @FachTracing("deque offer mutation")
    public boolean dequeOfferMutation(int age) {
        Deque<String> reasons = new ArrayDeque<>();
        if (age < 24) {
            reasons.offer("age");
        }
        return reasons.isEmpty();
    }

    @FachTracing("local alias mutation")
    public boolean localAliasMutation(int age) {
        List<String> reasons = new ArrayList<>();
        addAliasReason(reasons, age);
        return reasons.isEmpty();
    }

    private void addAliasReason(List<String> target, int age) {
        List<String> alias = target;
        if (age < 24) {
            alias.add("age");
        }
    }

    @FachTracing("invalidated local alias")
    public boolean invalidatedAlias(int age) {
        List<String> reasons = new ArrayList<>();
        addDetachedReason(reasons, age);
        return reasons.isEmpty();
    }

    private void addDetachedReason(List<String> target, int age) {
        List<String> alias = target;
        alias = new ArrayList<>();
        if (age < 24) {
            alias.add("age");
        }
    }

    @FachTracing("conditional alias mutation")
    public boolean conditionalAliasMutation(boolean detached, int age) {
        List<String> reasons = new ArrayList<>();
        addConditionalAliasReason(reasons, detached, age);
        return reasons.isEmpty();
    }

    private void addConditionalAliasReason(List<String> target, boolean detached, int age) {
        List<String> alias = target;
        if (detached) {
            alias = new ArrayList<>();
        }
        if (age < 24) {
            alias.add("young");
        }
    }

    @FachTracing("method reference mutation")
    public boolean methodReferenceMutation(List<String> candidates) {
        List<String> accepted = new ArrayList<>();
        candidates.stream().forEach(accepted::add);
        return !accepted.isEmpty();
    }

    @FachTracing("unknown platform effect")
    public boolean unknownPlatformEffect(Date date, long epoch) {
        date.setTime(epoch);
        return date.getTime() > 0;
    }
}
