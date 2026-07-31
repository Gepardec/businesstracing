package at.gepardec.fachtracing.runtime;

import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.store.DecisionRecordRepository;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded single-consumer delivery that keeps repository I/O off decision threads. */
public final class DecisionRecordDelivery implements AutoCloseable {
    private final DecisionRecordRepository repository;
    private final ArrayBlockingQueue<DecisionRecordEnvelope> queue;
    private final AdmissionPolicy policy;
    private final int maxRetries;
    private final Duration retryDelay;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Counters counters = new Counters();
    private final Thread worker;

    public DecisionRecordDelivery(
            DecisionRecordRepository repository,
            int capacity,
            AdmissionPolicy policy,
            int maxRetries,
            Duration retryDelay) {
        this.repository = Objects.requireNonNull(repository, "repository");
        if (capacity < 1 || maxRetries < 0) throw new IllegalArgumentException("invalid delivery bounds");
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.maxRetries = maxRetries;
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay");
        worker = Thread.ofPlatform().name("fachtracing-record-delivery").daemon(true).start(this::run);
    }

    /** Admits an immutable envelope without repository I/O. */
    public boolean offer(DecisionRecordEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        if (!running.get()) { counters.rejected.incrementAndGet(); return false; }
        boolean accepted;
        if (policy == AdmissionPolicy.BLOCK) {
            try { queue.put(envelope); accepted = true; }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); accepted = false; }
        } else {
            accepted = queue.offer(envelope);
        }
        if (accepted) counters.accepted.incrementAndGet();
        else if (policy == AdmissionPolicy.FAIL_OPEN) counters.dropped.incrementAndGet();
        else counters.rejected.incrementAndGet();
        return accepted;
    }

    public DeliveryCounters counters() { return counters.snapshot(); }

    private void run() {
        while (running.get() || !queue.isEmpty()) {
            try {
                DecisionRecordEnvelope envelope = queue.poll(50, TimeUnit.MILLISECONDS);
                if (envelope != null) save(envelope);
            } catch (InterruptedException interrupted) {
                if (running.get()) continue;
            }
        }
    }

    private void save(DecisionRecordEnvelope envelope) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                repository.saveEnvelope(envelope);
                counters.saved.incrementAndGet();
                return;
            } catch (RuntimeException failure) {
                if (attempt == maxRetries) { counters.dropped.incrementAndGet(); return; }
                counters.retried.incrementAndGet();
                try { Thread.sleep(retryDelay.toMillis()); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    @Override public void close() {
        running.set(false);
        try { worker.join(10_000); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        while (!queue.isEmpty()) { queue.poll(); counters.dropped.incrementAndGet(); }
    }

    public enum AdmissionPolicy { FAIL_OPEN, BLOCK, REJECT_NEW_TRACE }
    public record DeliveryCounters(long accepted, long saved, long retried, long rejected, long dropped) { }
    private static final class Counters {
        private final AtomicLong accepted = new AtomicLong(); private final AtomicLong saved = new AtomicLong();
        private final AtomicLong retried = new AtomicLong(); private final AtomicLong rejected = new AtomicLong();
        private final AtomicLong dropped = new AtomicLong();
        private DeliveryCounters snapshot() { return new DeliveryCounters(accepted.get(), saved.get(), retried.get(), rejected.get(), dropped.get()); }
    }
}
