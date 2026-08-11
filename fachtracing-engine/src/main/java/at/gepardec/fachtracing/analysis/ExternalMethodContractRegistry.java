package at.gepardec.fachtracing.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Validates providers and resolves zero or one exact method contract without priority rules. */
public final class ExternalMethodContractRegistry {
    private static final ExternalMethodContractRegistry EMPTY =
            new ExternalMethodContractRegistry(Map.of(), List.of());

    private final Map<ExternalMethodReference, List<Match>> matches;
    private final List<Provider> providers;

    private ExternalMethodContractRegistry(
            Map<ExternalMethodReference, List<Match>> matches,
            List<Provider> providers) {
        this.matches = matches;
        this.providers = providers;
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
        var registeredProviders = new ArrayList<Provider>();
        for (ExternalMethodContractProvider provider : orderedProviders) {
            String providerId = providerId(provider);
            registeredProviders.add(new Provider(providerId, provider));
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
                && registeredProviders.isEmpty()
                ? EMPTY : new ExternalMethodContractRegistry(
                        Map.copyOf(immutable), List.copyOf(registeredProviders));
    }

    /** Resolves an exact method key. More than one match is an explicit conflict. */
    public Resolution resolve(ExternalMethodReference method) {
        return resolution(matches.getOrDefault(Objects.requireNonNull(method, "method"), List.of()));
    }

    /** Resolves exact and contextual matches. More than one provider match is a conflict. */
    public Resolution resolve(
            ExternalMethodReference method,
            Set<String> ownerTypeBinaryNames) {
        Objects.requireNonNull(method, "method");
        ownerTypeBinaryNames = Set.copyOf(Objects.requireNonNull(
                ownerTypeBinaryNames, "ownerTypeBinaryNames"));
        var found = new ArrayList<>(matches.getOrDefault(method, List.of()));
        var matchedProviders = found.stream().map(Match::providerId)
                .collect(java.util.stream.Collectors.toSet());
        for (Provider provider : providers) {
            if (matchedProviders.contains(provider.providerId())) continue;
            Optional<ExternalMethodContract> contextual = Objects.requireNonNull(
                    provider.provider().contextualContract(method, ownerTypeBinaryNames),
                    "contextual contract from " + provider.providerId());
            contextual.ifPresent(contract -> {
                if (!contract.method().equals(method)) {
                    throw new IllegalArgumentException("contextual contract must use the requested exact method key");
                }
                found.add(new Match(provider.providerId(), contract));
            });
        }
        return resolution(found);
    }

    private static Resolution resolution(List<Match> found) {
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

    private record Provider(String providerId, ExternalMethodContractProvider provider) { }
}
