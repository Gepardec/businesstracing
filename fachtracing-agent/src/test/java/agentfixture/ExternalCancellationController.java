package agentfixture;

/** A caller with no graph binding that cancels traced work. */
public final class ExternalCancellationController {
    public boolean cancel(java.util.concurrent.Future<?> future) {
        return future.cancel(false);
    }
}

