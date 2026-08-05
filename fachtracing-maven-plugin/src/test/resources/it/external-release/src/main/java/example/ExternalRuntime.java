package example;

import at.gepardec.fachtracing.agent.FachtracingAgent;
import at.gepardec.fachtracing.api.DecisionValueRedactor;
import at.gepardec.fachtracing.explain.DecisionExplanationProjector;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.model.DecisionRecordEnvelope;
import at.gepardec.fachtracing.runtime.DecisionRecordDelivery;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.RuntimeActivationBundle;
import at.gepardec.fachtracing.runtime.TraceRuntime;
import at.gepardec.fachtracing.storage.jdbc.JdbcDecisionRecordRepository;
import org.h2.jdbcx.JdbcDataSource;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public final class ExternalRuntime {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("activation bundle path is required");
        Path activation = Path.of(args[0]).toAbsolutePath().normalize();
        var bundle = RuntimeActivationBundle.fromJson(Files.readAllBytes(activation));
        var decision = bundle.decisions().stream()
                .filter(item -> item.graph().decisionLabel().equals("external approval"))
                .findFirst().orElseThrow();

        var collector = new RuntimeCollector();
        for (var item : bundle.decisions()) collector.register(item.graph(),
                new DecisionExecution.DecisionValueCodec(DecisionValueRedactor.none()));
        TraceRuntime.configure(collector);
        FachtracingAgent.configure(bundle);

        boolean returned = new ExternalDecision().approve(25, "Vienna");
        if (!returned) throw new IllegalStateException("annotated decision returned false");
        DecisionExecution execution = collector.pollCompleted().orElseThrow(() ->
                new IllegalStateException("annotated decision did not produce a runtime trace"));
        if (!execution.finalResult().canonicalValue().equals("true")
                || execution.observations().isEmpty()
                || execution.observations().stream().noneMatch(item -> item.selectedEdgeId() != null)) {
            throw new IllegalStateException("runtime trace does not contain the selected decision path: "
                    + execution.observations() + " / branches " + decision.manifest().branchTargets());
        }
        var projector = new DecisionExplanationProjector();
        var explanation = projector.project(decision.graph(), execution);
        String explanationText = projector.text(explanation);
        if (explanation.steps().isEmpty() || !explanationText.contains("Result: true")) {
            throw new IllegalStateException("runtime trace did not produce an explanation");
        }

        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:external;DB_CLOSE_DELAY=-1");
        var repository = new JdbcDecisionRecordRepository(dataSource);
        repository.migrate();
        var envelope = new DecisionRecordEnvelope(UUID.randomUUID().toString(), execution,
                bundle.boundaryFingerprint(),
                Map.of("case", new DecisionExecution.DecisionValue("string", "redacted-hash", "REDACTED")),
                "external-policy");
        var delivery = new DecisionRecordDelivery(repository, 16,
                DecisionRecordDelivery.AdmissionPolicy.FAIL_OPEN, 2, Duration.ofMillis(1));
        try (delivery) {
            if (!delivery.offer(envelope)) throw new IllegalStateException("record was not accepted");
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (repository.findByExecutionId(execution.executionId()).isEmpty()
                    && System.nanoTime() < deadline) Thread.sleep(1);
        }
        DecisionRecordDelivery.DeliveryCounters counters = delivery.counters();
        var stored = repository.findByExecutionId(execution.executionId()).orElseThrow(() ->
                new IllegalStateException("runtime record was not persisted"));
        if (counters.unresolvedAccepted() != 0) {
            throw new IllegalStateException("accepted runtime record was not accounted for");
        }
        String storedExplanation = projector.text(projector.project(decision.graph(), stored.execution()));
        if (!storedExplanation.equals(explanationText)) {
            throw new IllegalStateException("retrieved runtime record changed its explanation");
        }
        if (FachtracingAgent.pollDiagnostic().isPresent()) {
            throw new IllegalStateException("agent installation reported a diagnostic");
        }
        System.out.println(explanationText);
        System.out.println("EXTERNAL_RUNTIME_TRACE_OK");
    }
}
