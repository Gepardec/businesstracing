package agentfixture;

import at.gepardec.fachtracing.api.FachTracing;

public final class InstrumentedFixture {
    private final IllegalStateException propagatedFailure = new IllegalStateException("called method failed");
    private int secondOperandEvaluations;
    private final java.util.concurrent.RejectedExecutionException expectedRejection =
            new java.util.concurrent.RejectedExecutionException("expected rejection");

    @FachTracing("evidence decision")
    public boolean decideEvidence(int age, String employeeId) {
        return age < 24;
    }

    public boolean decide(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("age must not be negative");
        }
        if (age < 24) {
            return true;
        }
        return false;
    }

    public boolean decideThroughHelper() {
        failFromHelper();
        return true;
    }

    public Throwable propagatedFailure() {
        return propagatedFailure;
    }

    @FachTracing("compound conjunction")
    public boolean decideAnd(boolean first, boolean second) {
        secondOperandEvaluations = 0;
        if (first
                && evaluateSecondOperand(second)) {
            return true;
        }
        return false;
    }

    @FachTracing("compound disjunction")
    public boolean decideOr(boolean first, boolean second) {
        secondOperandEvaluations = 0;
        if (first
                || evaluateSecondOperand(second)) {
            return true;
        }
        return false;
    }

    @FachTracing("mixed compound")
    public boolean decideMixed(boolean first, boolean second, boolean third) {
        if ((first && second) || third) {
            return true;
        }
        return false;
    }

    @FachTracing("negated compound")
    public boolean decideNegated(boolean first, boolean second, boolean third) {
        if (!(first && (second || third))) {
            return true;
        }
        return false;
    }

    @FachTracing("ternary predicate")
    public boolean decideTernary(boolean selector, boolean first, boolean second) {
        if (selector ? first : second) {
            return true;
        }
        return false;
    }

    @FachTracing("integer switch")
    public String decideIntegerSwitch(int level) {
        return switch (level) {
            case 1 -> "low";
            case 2, 3 -> "medium";
            default -> "high";
        };
    }

    @FachTracing("string switch")
    public int decideStringSwitch(String category) {
        return switch (category) {
            case "standard" -> 1;
            case "preferred" -> 2;
            default -> 0;
        };
    }

    @FachTracing("enum switch")
    public boolean decideEnumSwitch(CustomerGroup group) {
        return switch (group) {
            case STANDARD -> false;
            case PREFERRED -> true;
        };
    }

    public enum CustomerGroup { STANDARD, PREFERRED }

    @FachTracing("pattern switch")
    public String decidePatternSwitch(DecisionInput input) {
        return switch (input) {
            case AgeInput(var age) when age >= 24 -> "adult";
            case AgeInput ignored -> "young";
            case CategoryInput(var category) -> category;
        };
    }

    public sealed interface DecisionInput permits AgeInput, CategoryInput { }
    public record AgeInput(int age) implements DecisionInput { }
    public record CategoryInput(String category) implements DecisionInput { }

    @FachTracing("caught exception decision")
    public boolean decideCaughtException(int age, int regionScore) {
        try {
            if (age < 0) {
                throw new InvalidAge();
            }
            if (regionScore < 0) {
                throw new InvalidRegion();
            }
            return age + regionScore >= 24;
        } catch (InvalidAge | InvalidRegion ignored) {
            return false;
        }
    }

    @FachTracing("finally decision")
    public boolean decideFinally(int age, boolean regional) {
        int score = age;
        try {
            score += 0;
        } finally {
            if (regional) {
                score += 5;
            }
        }
        return score >= 24;
    }

    @FachTracing("conditional finally return")
    public boolean decideConditionalFinallyReturn(boolean primary, boolean override) {
        try {
            return primary;
        } finally {
            if (override) {
                return false;
            }
        }
    }

    @FachTracing("overriding finally return")
    public boolean decideOverridingFinallyReturn(int age) {
        try {
            return age >= 24;
        } finally {
            return false;
        }
    }

    @FachTracing("resource decision")
    public boolean decideResource(String category) throws Exception {
        try (var resource = new TestResource()) {
            return category.length() >= 4;
        }
    }

    @FachTracing("nested exception decision")
    public boolean decideNestedException(int age, boolean regional) {
        try {
            try {
                if (age < 0) {
                    throw new InvalidAge();
                }
                return age >= 24;
            } catch (InvalidAge ignored) {
                return false;
            }
        } finally {
            if (regional && age == 23) {
                return true;
            }
        }
    }

    public static final class InvalidAge extends RuntimeException { }
    public static final class InvalidRegion extends RuntimeException { }
    public static final class TestResource implements AutoCloseable {
        @Override public void close() { }
    }

    @FachTracing("completion stage async")
    public boolean decideStageAsync(int age) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> age)
                .thenApplyAsync(value -> value >= 24)
                .join();
    }

    @FachTracing("executor async")
    public boolean decideExecutorAsync(int age) {
        return java.util.concurrent.ForkJoinPool.commonPool()
                .submit(() -> age >= 24)
                .join();
    }

    @FachTracing("platform thread async")
    public boolean decidePlatformThread(int age) throws Exception {
        var task = new java.util.concurrent.FutureTask<Boolean>(() -> age >= 24);
        var thread = new Thread(task);
        thread.start();
        thread.join();
        return task.get();
    }

    @FachTracing("virtual thread async")
    public boolean decideVirtualThread(int age) throws Exception {
        var task = new java.util.concurrent.FutureTask<Boolean>(() -> age >= 24);
        var thread = Thread.startVirtualThread(task);
        thread.join();
        return task.get();
    }

    @FachTracing("binary stage callback")
    public boolean decideThenCombine(int age) {
        return java.util.concurrent.CompletableFuture.completedFuture(age)
                .thenCombine(java.util.concurrent.CompletableFuture.completedFuture(0),
                        (value, offset) -> value + offset >= 24)
                .join();
    }

    @FachTracing("accept both callback")
    public boolean decideThenAcceptBoth(int age) {
        secondOperandEvaluations = 0;
        java.util.concurrent.CompletableFuture.completedFuture(age)
                .thenAcceptBoth(java.util.concurrent.CompletableFuture.completedFuture(0),
                        (value, offset) -> {
                            if (value + offset >= 24) secondOperandEvaluations++;
                        })
                .join();
        return secondOperandEvaluations > 0;
    }

    @FachTracing("run after both callback")
    public boolean decideRunAfterBoth(int age) {
        secondOperandEvaluations = 0;
        java.util.concurrent.CompletableFuture.completedFuture(age)
                .runAfterBoth(java.util.concurrent.CompletableFuture.completedFuture(0), () -> {
                    if (age >= 24) secondOperandEvaluations++;
                })
                .join();
        return secondOperandEvaluations > 0;
    }

    @FachTracing("thread group callback")
    public boolean decideThreadGroup(int age) throws Exception {
        var task = new java.util.concurrent.FutureTask<Boolean>(() -> age >= 24);
        var thread = new Thread(new ThreadGroup("fixture"), task);
        thread.start();
        thread.join();
        return task.get();
    }

    @FachTracing("caught rejection")
    public boolean decideCaughtRejection(int age) {
        java.util.concurrent.Executor rejecting = ignored -> { throw expectedRejection; };
        try {
            rejecting.execute(() -> { if (age >= 24) secondOperandEvaluations++; });
        } catch (java.util.concurrent.RejectedExecutionException expected) {
            return age >= 24;
        }
        return false;
    }

    @FachTracing("uncaught rejection")
    public boolean decideUncaughtRejection(int age) {
        java.util.concurrent.Executor rejecting = ignored -> { throw expectedRejection; };
        rejecting.execute(() -> { if (age >= 24) secondOperandEvaluations++; });
        return age >= 24;
    }

    @FachTracing("cancelled submission")
    public boolean decideCancelledSubmission(java.util.concurrent.ExecutorService executor, int age) {
        java.util.concurrent.Future<Boolean> future = executor.submit(() -> age >= 24);
        return future.cancel(false) && age >= 24;
    }

    public Throwable expectedRejection() {
        return expectedRejection;
    }

    @FachTracing("scheduled boundary gap")
    public boolean decideUnsupportedScheduledBoundary(int age) throws Exception {
        var executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        boolean result = executor.schedule(
                () -> age >= 24, 0, java.util.concurrent.TimeUnit.MILLISECONDS).get();
        executor.shutdown();
        return result;
    }

    @FachTracing("overload integer")
    public boolean overloaded(int age) {
        return age < 24;
    }

    @FachTracing("overload text")
    public boolean overloaded(String city) {
        return city.equals("Vienna");
    }

    @FachTracing("overload lambda integer")
    public boolean lambdaOverload(int age) {
        return java.util.stream.Stream.of(age).anyMatch(value -> value < 24);
    }

    @FachTracing("overload lambda text")
    public boolean lambdaOverload(String city) {
        return java.util.stream.Stream.of(city).anyMatch(value -> value.equals("Vienna"));
    }

    public int secondOperandEvaluations() {
        return secondOperandEvaluations;
    }

    private boolean evaluateSecondOperand(boolean value) {
        secondOperandEvaluations++;
        return value;
    }

    private void failFromHelper() {
        throw propagatedFailure;
    }
}
