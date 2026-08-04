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
    private final long retryDelayMillis;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Object lifecycle = new Object();
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
        Objects.requireNonNull(retryDelay, "retryDelay");
        if (retryDelay.isNegative()) throw new IllegalArgumentException("retryDelay must not be negative");
        try { this.retryDelayMillis = retryDelay.toMillis(); }
        catch (ArithmeticException tooLarge) {
            throw new IllegalArgumentException("retryDelay is too large", tooLarge);
        }
        worker = Thread.ofPlatform().name("fachtracing-record-delivery").daemon(true).start(this::run);
    }

    /** Admits an immutable envelope without repository I/O. */
    public boolean offer(DecisionRecordEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        if (policy == AdmissionPolicy.BLOCK) return blockingOffer(envelope);
        synchronized (lifecycle) {
            if (!running.get()) { counters.rejected.incrementAndGet(); return false; }
            boolean accepted = queue.offer(envelope);
            if (accepted) counters.accepted.incrementAndGet();
            else if (policy == AdmissionPolicy.FAIL_OPEN) counters.admissionDropped.incrementAndGet();
            else counters.rejected.incrementAndGet();
            return accepted;
        }
    }

    private boolean blockingOffer(DecisionRecordEnvelope envelope) {
        synchronized (lifecycle) {
            while (running.get()) {
                if (queue.offer(envelope)) {
                    counters.accepted.incrementAndGet();
                    return true;
                }
                try { lifecycle.wait(); }
                catch (InterruptedException interrupted) {
                    counters.rejected.incrementAndGet();
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            counters.rejected.incrementAndGet();
            return false;
        }
    }

    public DeliveryCounters counters() { return counters.snapshot(); }

    /** Reports whether the storage worker is active. */
    public boolean workerAlive() { return worker.isAlive(); }

    private void run() {
        while (running.get() || !queue.isEmpty()) {
            try {
                DecisionRecordEnvelope envelope = queue.poll(50, TimeUnit.MILLISECONDS);
                if (envelope != null) {
                    synchronized (lifecycle) { lifecycle.notifyAll(); }
                    save(envelope);
                }
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
                try { Thread.sleep(retryDelayMillis); }
                catch (InterruptedException interrupted) {
                    counters.dropped.incrementAndGet();
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override public void close() {
        if (Thread.currentThread() == worker) {
            throw new IllegalStateException("delivery worker cannot close itself");
        }
        synchronized (lifecycle) { running.set(false); lifecycle.notifyAll(); }
        worker.interrupt();
        boolean interrupted = false;
        while (worker.isAlive()) {
            try { worker.join(); }
            catch (InterruptedException ignored) { interrupted = true; worker.interrupt(); }
        }
        while (!queue.isEmpty()) {
            DecisionRecordEnvelope ignored = queue.poll();
            if (ignored != null) counters.dropped.incrementAndGet();
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    public enum AdmissionPolicy { FAIL_OPEN, BLOCK, REJECT_NEW_TRACE }
    public record DeliveryCounters(
            long accepted, long saved, long retried, long rejected, long admissionDropped, long dropped) {
        /** Accepted records that are neither saved nor accounted as dropped. */
        public long unresolvedAccepted() { return accepted - saved - dropped; }
    }
    private static final class Counters {
        private final AtomicLong accepted = new AtomicLong(); private final AtomicLong saved = new AtomicLong();
        private final AtomicLong retried = new AtomicLong(); private final AtomicLong rejected = new AtomicLong();
        private final AtomicLong admissionDropped = new AtomicLong(); private final AtomicLong dropped = new AtomicLong();
        private DeliveryCounters snapshot() { return new DeliveryCounters(accepted.get(), saved.get(), retried.get(),
                rejected.get(), admissionDropped.get(), dropped.get()); }
    }
}
