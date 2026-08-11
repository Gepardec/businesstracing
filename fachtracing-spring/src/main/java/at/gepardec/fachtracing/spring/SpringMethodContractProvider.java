package at.gepardec.fachtracing.spring;

import at.gepardec.fachtracing.analysis.ExternalMethodContract;
import at.gepardec.fachtracing.analysis.ExternalMethodContractProvider;
import at.gepardec.fachtracing.analysis.ExternalMethodReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Application-neutral method semantics for supported Spring APIs. */
public final class SpringMethodContractProvider implements ExternalMethodContractProvider {
    private static final String DATA_INTEGRITY_FAILURE =
            "org.springframework.dao.DataIntegrityViolationException";
    private static final List<ExternalMethodContract> CONTRACTS = buildContracts();

    @Override
    public String providerId() {
        return "spring:framework";
    }

    @Override
    public List<ExternalMethodContract> contracts() {
        return CONTRACTS;
    }

    @Override
    public Optional<ExternalMethodContract> contextualContract(
            ExternalMethodReference method, Set<String> ownerTypeBinaryNames) {
        boolean springDataRepository = ownerTypeBinaryNames.contains(
                "org.springframework.data.repository.Repository");
        boolean derivedPageQuery = method.methodName().matches(
                "(?:find|read|get|query|search|stream).*By.+")
                && method.descriptor().endsWith(")Lorg/springframework/data/domain/Page;");
        if (!springDataRepository || !derivedPageQuery) return Optional.empty();
        return Optional.of(new ExternalMethodContract(method,
                ExternalMethodContract.OperationKind.READ, "find matching records",
                ExternalMethodContract.ResultBehavior.VALUE,
                ExternalMethodContract.StateEffect.NONE, Map.of(),
                Set.of("org.springframework.dao.DataAccessException")));
    }

    private static List<ExternalMethodContract> buildContracts() {
        var contracts = new ArrayList<ExternalMethodContract>();
        contracts.add(predicate("org.springframework.util.StringUtils", "hasText",
                "(Ljava/lang/CharSequence;)Z", "text is present"));
        contracts.add(predicate("org.springframework.util.StringUtils", "hasText",
                "(Ljava/lang/String;)Z", "text is present"));
        contracts.add(check("org.springframework.util.Assert", "notNull",
                "(Ljava/lang/Object;Ljava/lang/String;)V", "required value is present",
                "java.lang.IllegalArgumentException"));
        contracts.add(check("org.springframework.util.Assert", "notNull",
                "(Ljava/lang/Object;Ljava/util/function/Supplier;)V", "required value is present",
                "java.lang.IllegalArgumentException"));
        contracts.add(check("org.springframework.util.Assert", "state",
                "(ZLjava/lang/String;)V", "required condition is met",
                "java.lang.IllegalStateException"));
        contracts.add(check("org.springframework.util.Assert", "state",
                "(ZLjava/util/function/Supplier;)V", "required condition is met",
                "java.lang.IllegalStateException"));

        for (String owner : List.of(
                "org.springframework.validation.Errors",
                "org.springframework.validation.BindingResult")) {
            contracts.add(predicate(owner, "hasErrors", "()Z", "validation has errors"));
            contracts.add(action(owner, "reject", "(Ljava/lang/String;)V",
                    "record validation error", Map.of(), Set.of()));
            contracts.add(action(owner, "reject", "(Ljava/lang/String;Ljava/lang/String;)V",
                    "record validation error", Map.of(), Set.of()));
            contracts.add(action(owner, "reject",
                    "(Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/String;)V",
                    "record validation error", Map.of(), Set.of()));
            contracts.add(action(owner, "rejectValue", "(Ljava/lang/String;Ljava/lang/String;)V",
                    "record field validation error", Map.of(), Set.of()));
            contracts.add(action(owner, "rejectValue",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    "record field validation error", Map.of(), Set.of()));
            contracts.add(action(owner, "rejectValue",
                    "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;Ljava/lang/String;)V",
                    "record field validation error", Map.of(), Set.of()));
        }
        contracts.add(action("org.springframework.validation.BindingResult", "addError",
                "(Lorg/springframework/validation/ObjectError;)V",
                "record validation error", Map.of(), Set.of()));

        for (String owner : List.of(
                "org.springframework.data.util.Streamable",
                "org.springframework.data.domain.Slice",
                "org.springframework.data.domain.Page")) {
            contracts.add(predicate(owner, "isEmpty", "()Z", "result page is empty"));
        }
        for (String owner : List.of(
                "org.springframework.data.domain.Slice",
                "org.springframework.data.domain.Page")) {
            contracts.add(predicate(owner, "hasContent", "()Z", "result page has entries"));
            contracts.add(read(owner, "getNumberOfElements", "()I", "result count"));
            contracts.add(read(owner, "getContent", "()Ljava/util/List;", "result page entries"));
        }
        contracts.add(read("org.springframework.data.domain.Page", "getTotalElements", "()J",
                "total result count"));
        contracts.add(read("org.springframework.data.domain.Page", "getTotalPages", "()I",
                "total page count"));
        contracts.add(read("org.springframework.data.domain.PageRequest", "of",
                "(II)Lorg/springframework/data/domain/PageRequest;", "page request"));
        contracts.add(read("org.springframework.data.domain.PageRequest", "of",
                "(IILorg/springframework/data/domain/Sort;)Lorg/springframework/data/domain/PageRequest;",
                "page request"));
        contracts.add(read("org.springframework.data.domain.PageRequest", "of",
                "(IILorg/springframework/data/domain/Sort$Direction;[Ljava/lang/String;)Lorg/springframework/data/domain/PageRequest;",
                "page request"));
        contracts.add(read("org.springframework.data.domain.PageRequest", "ofSize",
                "(I)Lorg/springframework/data/domain/PageRequest;", "page request"));

        for (String owner : List.of(
                "org.springframework.data.repository.CrudRepository",
                "org.springframework.data.repository.ListCrudRepository",
                "org.springframework.data.jpa.repository.JpaRepository")) {
            contracts.add(persistence(owner, "save", "(Ljava/lang/Object;)Ljava/lang/Object;",
                    "save record"));
        }
        contracts.add(persistence("org.springframework.data.jpa.repository.JpaRepository",
                "saveAndFlush", "(Ljava/lang/Object;)Ljava/lang/Object;", "save record"));

        String redirect = "org.springframework.web.servlet.mvc.support.RedirectAttributes";
        contracts.add(fluentAction(redirect, "addFlashAttribute",
                "(Ljava/lang/String;Ljava/lang/Object;)Lorg/springframework/web/servlet/mvc/support/RedirectAttributes;",
                "add response message"));
        contracts.add(fluentAction(redirect, "addFlashAttribute",
                "(Ljava/lang/Object;)Lorg/springframework/web/servlet/mvc/support/RedirectAttributes;",
                "add response message"));
        return List.copyOf(contracts);
    }

    private static ExternalMethodContract predicate(
            String owner, String name, String descriptor, String label) {
        return ExternalMethodContract.predicate(new ExternalMethodReference(owner, name, descriptor), label);
    }

    private static ExternalMethodContract read(
            String owner, String name, String descriptor, String label) {
        return ExternalMethodContract.read(new ExternalMethodReference(owner, name, descriptor), label,
                ExternalMethodContract.ResultBehavior.VALUE);
    }

    private static ExternalMethodContract action(
            String owner,
            String name,
            String descriptor,
            String label,
            Map<Integer, ExternalMethodContract.StateEffect> argumentEffects,
            Set<String> possibleExceptions) {
        return new ExternalMethodContract(new ExternalMethodReference(owner, name, descriptor),
                ExternalMethodContract.OperationKind.ACTION, label,
                ExternalMethodContract.ResultBehavior.NONE,
                ExternalMethodContract.StateEffect.MUTATE, argumentEffects, possibleExceptions);
    }

    private static ExternalMethodContract check(
            String owner, String name, String descriptor, String label, String exceptionType) {
        return new ExternalMethodContract(new ExternalMethodReference(owner, name, descriptor),
                ExternalMethodContract.OperationKind.READ, label,
                ExternalMethodContract.ResultBehavior.NONE,
                ExternalMethodContract.StateEffect.NONE, Map.of(),
                Set.of(exceptionType));
    }

    private static ExternalMethodContract persistence(
            String owner, String name, String descriptor, String label) {
        return new ExternalMethodContract(new ExternalMethodReference(owner, name, descriptor),
                ExternalMethodContract.OperationKind.ACTION, label,
                ExternalMethodContract.ResultBehavior.VALUE,
                ExternalMethodContract.StateEffect.MUTATE,
                Map.of(0, ExternalMethodContract.StateEffect.MUTATE),
                Set.of(DATA_INTEGRITY_FAILURE));
    }

    private static ExternalMethodContract fluentAction(
            String owner, String name, String descriptor, String label) {
        return new ExternalMethodContract(new ExternalMethodReference(owner, name, descriptor),
                ExternalMethodContract.OperationKind.ACTION, label,
                ExternalMethodContract.ResultBehavior.RECEIVER,
                ExternalMethodContract.StateEffect.MUTATE, Map.of(), Set.of());
    }
}
