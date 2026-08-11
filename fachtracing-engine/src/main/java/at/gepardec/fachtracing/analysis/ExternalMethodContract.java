package at.gepardec.fachtracing.analysis;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Trusted, immutable semantic facts for one exact source-unavailable method. */
public record ExternalMethodContract(
        ExternalMethodReference method,
        OperationKind operationKind,
        String businessLabel,
        ResultBehavior resultBehavior,
        StateEffect receiverEffect,
        Map<Integer, StateEffect> argumentEffects,
        Set<String> possibleExceptionTypes) {
    /** Creates and validates one exact semantic contract. */
    public ExternalMethodContract {
        method = Objects.requireNonNull(method, "method");
        operationKind = Objects.requireNonNull(operationKind, "operationKind");
        businessLabel = requireText(businessLabel, "businessLabel");
        resultBehavior = Objects.requireNonNull(resultBehavior, "resultBehavior");
        receiverEffect = Objects.requireNonNull(receiverEffect, "receiverEffect");
        Objects.requireNonNull(argumentEffects, "argumentEffects");
        var effects = new TreeMap<Integer, StateEffect>();
        argumentEffects.forEach((index, effect) -> {
            if (index == null || index < 0) {
                throw new IllegalArgumentException("argument effect indexes must be non-negative");
            }
            effects.put(index, Objects.requireNonNull(effect, "argument effect"));
        });
        argumentEffects = Map.copyOf(effects);
        Objects.requireNonNull(possibleExceptionTypes, "possibleExceptionTypes");
        var exceptions = new TreeSet<String>();
        possibleExceptionTypes.forEach(type -> exceptions.add(requireText(type, "possibleExceptionType")));
        possibleExceptionTypes = Set.copyOf(exceptions);
        if (operationKind == OperationKind.PREDICATE
                && resultBehavior != ResultBehavior.BOOLEAN) {
            throw new IllegalArgumentException("predicate contracts must have a Boolean result");
        }
        if (operationKind == OperationKind.ACTION && resultBehavior == ResultBehavior.BOOLEAN) {
            throw new IllegalArgumentException("action contracts cannot describe a Boolean result");
        }
        String returnDescriptor = method.descriptor()
                .substring(method.descriptor().indexOf(')') + 1);
        if (resultBehavior == ResultBehavior.NONE && !returnDescriptor.equals("V")) {
            throw new IllegalArgumentException("a no-result contract must match a void method");
        }
        if (resultBehavior == ResultBehavior.BOOLEAN && !returnDescriptor.equals("Z")) {
            throw new IllegalArgumentException("a Boolean contract must match a boolean method");
        }
        if (resultBehavior != ResultBehavior.NONE
                && resultBehavior != ResultBehavior.BOOLEAN
                && returnDescriptor.equals("V")) {
            throw new IllegalArgumentException("a value-result contract cannot match a void method");
        }
    }

    /** Creates a pure Boolean predicate contract. */
    public static ExternalMethodContract predicate(ExternalMethodReference method, String label) {
        return new ExternalMethodContract(method, OperationKind.PREDICATE, label,
                ResultBehavior.BOOLEAN, StateEffect.NONE, Map.of(), Set.of());
    }

    /** Creates a pure value-read contract. */
    public static ExternalMethodContract read(
            ExternalMethodReference method, String label, ResultBehavior resultBehavior) {
        return new ExternalMethodContract(method, OperationKind.READ, label,
                resultBehavior, StateEffect.NONE, Map.of(), Set.of());
    }

    /** Contract operation types used by business projection and source analysis. */
    public enum OperationKind { PREDICATE, ACTION, READ }

    /** Proven state effect on a receiver or argument. */
    public enum StateEffect { NONE, MUTATE }

    /** Proven relationship between the call and its result. */
    public enum ResultBehavior { NONE, BOOLEAN, VALUE, RECEIVER, ARGUMENT }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
