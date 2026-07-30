package at.gepardec.fachtracing;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.explain.DecisionExplanationProjector;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.DecisionExecution;
import at.gepardec.fachtracing.mermaid.MermaidRenderer;
import at.gepardec.fachtracing.plantuml.PlantUmlRenderer;
import at.gepardec.fachtracing.runtime.RuntimeCollector;
import at.gepardec.fachtracing.runtime.TraceRuntime;
import at.gepardec.fachtracing.store.DecisionRecordRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Public orchestration facade for analysis, capture, explanation, rendering, and storage. */
public final class FachtracingEngine {
    private final StaticDecisionAnalyzer analyzer;
    private final RuntimeCollector collector;
    private final DecisionExplanationProjector explanations;
    private final PlantUmlRenderer plantUml;
    private final MermaidRenderer mermaid;
    private final DecisionRecordRepository records;

    /** Creates an engine around the application's storage port and an in-memory capture queue. */
    public FachtracingEngine(DecisionRecordRepository records) {
        this(new StaticDecisionAnalyzer(), new RuntimeCollector(),
                new DecisionExplanationProjector(), new PlantUmlRenderer(), new MermaidRenderer(), records);
    }

    /** Creates an explicitly composed engine for application integration and tests. */
    public FachtracingEngine(
            StaticDecisionAnalyzer analyzer,
            RuntimeCollector collector,
            DecisionExplanationProjector explanations,
            PlantUmlRenderer plantUml,
            DecisionRecordRepository records) {
        this(analyzer, collector, explanations, plantUml, new MermaidRenderer(), records);
    }

    /** Creates an explicitly composed engine with both diagram renderers. */
    public FachtracingEngine(
            StaticDecisionAnalyzer analyzer,
            RuntimeCollector collector,
            DecisionExplanationProjector explanations,
            PlantUmlRenderer plantUml,
            MermaidRenderer mermaid,
            DecisionRecordRepository records) {
        this.analyzer = Objects.requireNonNull(analyzer, "analyzer");
        this.collector = Objects.requireNonNull(collector, "collector");
        this.explanations = Objects.requireNonNull(explanations, "explanations");
        this.plantUml = Objects.requireNonNull(plantUml, "plantUml");
        this.mermaid = Objects.requireNonNull(mermaid, "mermaid");
        this.records = Objects.requireNonNull(records, "records");
    }

    /** Learns a decision graph from arbitrary annotated Java sources. */
    public AnalysisManifest.AnalysisResult analyze(AnalysisRequest request) {
        return analyzer.analyze(request);
    }

    /** Activates runtime capture for one analyzed graph and its value/redaction policy. */
    public void activate(
            AnalysisManifest.AnalysisResult analysis,
            DecisionExecution.DecisionValueCodec values) {
        collector.register(analysis.graph(), values);
        TraceRuntime.configure(collector);
    }

    /** Projects and persists the next completed capture, if one is available. */
    public Optional<DecisionRecordRepository.DecisionRecordId> saveNext(BusinessDecisionGraph graph) {
        return collector.pollCompleted().map(execution -> save(graph, execution));
    }

    /** Projects and persists one immutable execution. */
    public DecisionRecordRepository.DecisionRecordId save(
            BusinessDecisionGraph graph,
            DecisionExecution execution) {
        var explanation = explanations.project(graph, execution);
        var id = new DecisionRecordRepository.DecisionRecordId(UUID.randomUUID().toString());
        var record = new DecisionRecordRepository.DecisionRecord(id, graph, execution, explanation,
                plantUml.structure(graph), plantUml.execution(graph, execution),
                mermaid.structure(graph), mermaid.execution(graph, execution));
        return records.save(record);
    }

    /** Retrieves a previously saved decision record by opaque ID. */
    public Optional<DecisionRecordRepository.DecisionRecord> find(
            DecisionRecordRepository.DecisionRecordId id) {
        return records.findById(id);
    }

    /** Exposes the in-memory collector only for embedding probes and asynchronous drain coordination. */
    public RuntimeCollector collector() { return collector; }
}
