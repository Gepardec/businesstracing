package at.gepardec.fachtracing.agent;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Exact JDK asynchronous invocation signatures and their callback argument positions. */
final class AsyncInvocationCatalog {
    private static final String RUNNABLE = "Ljava/lang/Runnable;";
    private static final String CALLABLE = "Ljava/util/concurrent/Callable;";
    private static final String SUPPLIER = "Ljava/util/function/Supplier;";
    private static final String FUNCTION = "Ljava/util/function/Function;";
    private static final String CONSUMER = "Ljava/util/function/Consumer;";
    private static final String BI_FUNCTION = "Ljava/util/function/BiFunction;";
    private static final String BI_CONSUMER = "Ljava/util/function/BiConsumer;";
    private static final String EXECUTOR = "Ljava/util/concurrent/Executor;";
    private static final String STAGE = "Ljava/util/concurrent/CompletionStage;";
    private static final String FUTURE = "Ljava/util/concurrent/Future;";
    private static final String COMPLETABLE_FUTURE = "Ljava/util/concurrent/CompletableFuture;";
    private static final Set<String> EXECUTOR_OWNERS = Set.of(
            "java/util/concurrent/Executor",
            "java/util/concurrent/ExecutorService",
            "java/util/concurrent/ExecutorCompletionService",
            "java/util/concurrent/ScheduledExecutorService",
            "java/util/concurrent/AbstractExecutorService",
            "java/util/concurrent/ThreadPoolExecutor",
            "java/util/concurrent/ScheduledThreadPoolExecutor");
    private static final Set<String> STAGE_OWNERS = Set.of(
            "java/util/concurrent/CompletionStage",
            "java/util/concurrent/CompletableFuture");
    private static final List<Binding> BINDINGS = bindings();

    private AsyncInvocationCatalog() { }

    static Optional<Binding> find(String owner, String method, String descriptor) {
        return BINDINGS.stream().filter(binding -> binding.owner().equals(owner)
                && binding.method().equals(method) && binding.descriptor().equals(descriptor)).findFirst();
    }

    static boolean isUnmatchedBoundary(String owner, String method, String descriptor) {
        if (!isKnownOwner(owner) || nonBoundary(method)) return false;
        for (Type argument : Type.getArgumentTypes(descriptor)) {
            if (WrapperKind.forDescriptor(argument.getDescriptor()) != null) return true;
        }
        return false;
    }

    private static boolean isKnownOwner(String owner) {
        return EXECUTOR_OWNERS.contains(owner) || STAGE_OWNERS.contains(owner)
                || owner.equals("java/util/concurrent/ForkJoinPool")
                || owner.equals("java/lang/Thread")
                || owner.startsWith("java/lang/Thread$Builder");
    }

    private static boolean nonBoundary(String method) {
        return Set.of("get", "join", "start", "shutdown", "shutdownNow", "close", "awaitTermination",
                "isDone", "isCancelled", "isShutdown", "isTerminated", "state", "resultNow",
                "exceptionNow").contains(method);
    }

    private static List<Binding> bindings() {
        var result = new ArrayList<Binding>();
        for (String owner : EXECUTOR_OWNERS) {
            add(result, owner, "execute", "(" + RUNNABLE + ")V", 0);
            add(result, owner, "submit", "(" + RUNNABLE + ")" + FUTURE, 0);
            add(result, owner, "submit", "(" + CALLABLE + ")" + FUTURE, 0);
            add(result, owner, "submit", "(" + RUNNABLE + "Ljava/lang/Object;)" + FUTURE, 0);
        }
        add(result, "java/util/concurrent/ForkJoinPool", "submit",
                "(" + RUNNABLE + ")Ljava/util/concurrent/ForkJoinTask;", 0);
        add(result, "java/util/concurrent/ForkJoinPool", "submit",
                "(" + CALLABLE + ")Ljava/util/concurrent/ForkJoinTask;", 0);

        add(result, "java/util/concurrent/CompletableFuture", "runAsync",
                "(" + RUNNABLE + ")" + COMPLETABLE_FUTURE, 0);
        add(result, "java/util/concurrent/CompletableFuture", "runAsync",
                "(" + RUNNABLE + EXECUTOR + ")" + COMPLETABLE_FUTURE, 0);
        add(result, "java/util/concurrent/CompletableFuture", "supplyAsync",
                "(" + SUPPLIER + ")" + COMPLETABLE_FUTURE, 0);
        add(result, "java/util/concurrent/CompletableFuture", "supplyAsync",
                "(" + SUPPLIER + EXECUTOR + ")" + COMPLETABLE_FUTURE, 0);

        for (String owner : STAGE_OWNERS) addStageBindings(result, owner,
                owner.equals("java/util/concurrent/CompletionStage") ? STAGE : COMPLETABLE_FUTURE);

        add(result, "java/lang/Thread", "<init>", "(" + RUNNABLE + ")V", 0);
        add(result, "java/lang/Thread", "<init>",
                "(Ljava/lang/ThreadGroup;" + RUNNABLE + ")V", 1);
        add(result, "java/lang/Thread", "startVirtualThread",
                "(" + RUNNABLE + ")Ljava/lang/Thread;", 0);
        for (String owner : List.of(
                "java/lang/Thread$Builder", "java/lang/Thread$Builder$OfPlatform",
                "java/lang/Thread$Builder$OfVirtual")) {
            add(result, owner, "start", "(" + RUNNABLE + ")Ljava/lang/Thread;", 0);
        }
        return List.copyOf(result);
    }

    private static void addStageBindings(List<Binding> result, String owner, String returnType) {
        unary(result, owner, "thenApply", FUNCTION, returnType);
        unary(result, owner, "thenAccept", CONSUMER, returnType);
        unary(result, owner, "thenRun", RUNNABLE, returnType);
        unary(result, owner, "thenCompose", FUNCTION, returnType);
        unary(result, owner, "whenComplete", BI_CONSUMER, returnType);
        unary(result, owner, "handle", BI_FUNCTION, returnType);
        unary(result, owner, "exceptionally", FUNCTION, returnType);
        binaryStage(result, owner, "thenCombine", BI_FUNCTION, returnType);
        binaryStage(result, owner, "thenAcceptBoth", BI_CONSUMER, returnType);
        binaryStage(result, owner, "runAfterBoth", RUNNABLE, returnType);
        binaryStage(result, owner, "applyToEither", FUNCTION, returnType);
        binaryStage(result, owner, "acceptEither", CONSUMER, returnType);
        binaryStage(result, owner, "runAfterEither", RUNNABLE, returnType);
    }

    private static void unary(
            List<Binding> result, String owner, String method, String callback, String returnType) {
        add(result, owner, method, "(" + callback + ")" + returnType, 0);
        add(result, owner, method + "Async", "(" + callback + ")" + returnType, 0);
        add(result, owner, method + "Async", "(" + callback + EXECUTOR + ")" + returnType, 0);
    }

    private static void binaryStage(
            List<Binding> result, String owner, String method, String callback, String returnType) {
        add(result, owner, method, "(" + STAGE + callback + ")" + returnType, 1);
        add(result, owner, method + "Async", "(" + STAGE + callback + ")" + returnType, 1);
        add(result, owner, method + "Async", "(" + STAGE + callback + EXECUTOR + ")" + returnType, 1);
    }

    private static void add(
            List<Binding> result, String owner, String method, String descriptor, int callbackPosition) {
        Type[] arguments = Type.getArgumentTypes(descriptor);
        WrapperKind wrapper = callbackPosition < arguments.length
                ? WrapperKind.forDescriptor(arguments[callbackPosition].getDescriptor()) : null;
        if (wrapper == null) throw new IllegalArgumentException("catalog callback is not supported: " + descriptor);
        result.add(new Binding(owner, method, descriptor, callbackPosition, wrapper,
                Type.getReturnType(descriptor).getDescriptor().equals(FUTURE)));
    }

    record Binding(
            String owner,
            String method,
            String descriptor,
            int callbackPosition,
            WrapperKind wrapper,
            boolean futureResult) { }

    enum WrapperKind {
        RUNNABLE("prepareRunnable"), CALLABLE("prepareCallable"), FUNCTION("prepareFunction"),
        CONSUMER("prepareConsumer"), BI_FUNCTION("prepareBiFunction"),
        BI_CONSUMER("prepareBiConsumer"), SUPPLIER("prepareSupplier");

        private final String runtimeMethod;

        WrapperKind(String runtimeMethod) { this.runtimeMethod = runtimeMethod; }

        String runtimeMethod() { return runtimeMethod; }

        private static WrapperKind forDescriptor(String descriptor) {
            return switch (descriptor) {
                case AsyncInvocationCatalog.RUNNABLE -> WrapperKind.RUNNABLE;
                case AsyncInvocationCatalog.CALLABLE -> WrapperKind.CALLABLE;
                case AsyncInvocationCatalog.FUNCTION -> WrapperKind.FUNCTION;
                case AsyncInvocationCatalog.CONSUMER -> WrapperKind.CONSUMER;
                case AsyncInvocationCatalog.BI_FUNCTION -> WrapperKind.BI_FUNCTION;
                case AsyncInvocationCatalog.BI_CONSUMER -> WrapperKind.BI_CONSUMER;
                case AsyncInvocationCatalog.SUPPLIER -> WrapperKind.SUPPLIER;
                default -> null;
            };
        }
    }
}
