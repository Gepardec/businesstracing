package fixtures.slicing;

import at.gepardec.fachtracing.api.FachTracing;

public final class ResultSlicePolicy {
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
}

