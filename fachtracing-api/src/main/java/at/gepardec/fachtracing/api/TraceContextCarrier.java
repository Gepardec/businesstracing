package at.gepardec.fachtracing.api;

/** Framework-neutral SPI for explicit trace-context capture and restoration. */
public interface TraceContextCarrier {
    /** Captures the current context without making it active on another thread. */
    ContextToken captureContext();

    /** Restores a captured context until the returned scope closes. */
    ContextScope restoreContext(ContextToken token);

    /** Opaque immutable context token. */
    interface ContextToken { }

    /** A restored context scope. */
    @FunctionalInterface
    interface ContextScope extends AutoCloseable {
        @Override void close();
    }
}
