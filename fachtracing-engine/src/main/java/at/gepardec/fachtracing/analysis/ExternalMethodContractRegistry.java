package at.gepardec.fachtracing.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Validates providers and resolves zero or one exact method contract without priority rules. */
public final class ExternalMethodContractRegistry {
    private static final ExternalMethodContractRegistry EMPTY = new ExternalMethodContractRegistry(Map.of());

    private final Map<ExternalMethodReference, List<Match>> matches;

    private ExternalMethodContractRegistry(Map<ExternalMethodReference, List<Match>> matches) {
        this.matches = matches;
    }

    /** Creates an empty fail-closed registry. */
    public static ExternalMethodContractRegistry empty() {
        return EMPTY;
    }

    /** Creates a deterministic registry from explicitly enabled providers. */
    public static ExternalMethodContractRegistry of(
            Collection<? extends ExternalMethodContractProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        var orderedProviders = providers.stream()
                .map(provider -> Objects.requireNonNull(provider, "provider"))
                .sorted(Comparator.comparing(ExternalMethodContractRegistry::providerId)
                        .thenComparing(provider -> provider.getClass().getName()))
                .toList();
        var collected = new LinkedHashMap<ExternalMethodReference, List<Match>>();
        for (ExternalMethodContractProvider provider : orderedProviders) {
            String providerId = providerId(provider);
            Collection<ExternalMethodContract> contracts = Objects.requireNonNull(
                    provider.contracts(), "contracts from " + providerId);
            for (ExternalMethodContract contract : contracts) {
                ExternalMethodContract value = Objects.requireNonNull(
                        contract, "contract from " + providerId);
                collected.computeIfAbsent(value.method(), ignored -> new ArrayList<>())
                        .add(new Match(providerId, value));
            }
        }
        var immutable = new LinkedHashMap<ExternalMethodReference, List<Match>>();
        collected.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> immutable.put(entry.getKey(), List.copyOf(entry.getValue())));
        return immutable.isEmpty()
                ? EMPTY : new ExternalMethodContractRegistry(Map.copyOf(immutable));
    }

    /** Resolves an exact method key. More than one match is an explicit conflict. */
    public Resolution resolve(ExternalMethodReference method) {
        List<Match> found = matches.getOrDefault(Objects.requireNonNull(method, "method"), List.of());
        if (found.isEmpty()) return new Resolution(ResolutionKind.ABSENT, Optional.empty(), List.of());
        if (found.size() == 1) {
            return new Resolution(ResolutionKind.RESOLVED,
                    Optional.of(found.getFirst().contract()), List.of(found.getFirst().providerId()));
        }
        return new Resolution(ResolutionKind.CONFLICT, Optional.empty(),
                found.stream().map(Match::providerId).toList());
    }

    private static String providerId(ExternalMethodContractProvider provider) {
        String value = Objects.requireNonNull(provider.providerId(), "providerId");
        if (value.isBlank()) throw new IllegalArgumentException("providerId must not be blank");
        return value;
    }

    /** Exact resolution result. */
    public record Resolution(
            ResolutionKind kind,
            Optional<ExternalMethodContract> contract,
            List<String> providerIds) {
        /** Creates a defensive resolution. */
        public Resolution {
            kind = Objects.requireNonNull(kind, "kind");
            contract = Objects.requireNonNull(contract, "contract");
            providerIds = List.copyOf(providerIds);
            if ((kind == ResolutionKind.RESOLVED) != contract.isPresent()) {
                throw new IllegalArgumentException("only resolved results contain a contract");
            }
        }
    }

    /** Registry result kinds. */
    public enum ResolutionKind { ABSENT, RESOLVED, CONFLICT }

    private record Match(String providerId, ExternalMethodContract contract) { }
}
