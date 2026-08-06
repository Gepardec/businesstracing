package example;

/** A runtime caller with no annotated decision or graph binding. */
public final class ExternalController {
    public boolean cancel(java.util.concurrent.Future<?> future) {
        return future.cancel(false);
    }
}
