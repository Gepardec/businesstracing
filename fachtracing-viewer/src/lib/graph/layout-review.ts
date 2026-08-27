import type { GraphModel } from '$contracts/graph-contract';
import { computeLayout } from './layout-engine';
import { evaluateLayoutQuality, type LayoutQualityFailure, type LayoutQualityMetrics } from './route-quality';

const POC_MAX_LAYOUT_MS = 4_000;
const POC_EVIDENCE_NODE_RANGE = { minimum: 19, maximum: 55 } as const;

export interface GraphReviewFailure extends Omit<LayoutQualityFailure, 'metric'> {
  metric: LayoutQualityFailure['metric'] | 'durationMs';
}

export interface GraphReviewResult {
  source: string;
  sha256?: string;
  schema: GraphModel['schema'];
  graphId: string;
  label: string;
  nodeCount: number;
  edgeCount: number;
  durationMs: number;
  width: number;
  height: number;
  placementProfileId: string;
  metrics: LayoutQualityMetrics;
  failures: readonly GraphReviewFailure[];
}

export function responsivenessFailure(durationMs: number, nodeCount: number): GraphReviewFailure | null {
  if (nodeCount < POC_EVIDENCE_NODE_RANGE.minimum || nodeCount > POC_EVIDENCE_NODE_RANGE.maximum || durationMs <= POC_MAX_LAYOUT_MS) return null;
  return {
    metric: 'durationMs',
    actual: durationMs,
    maximum: POC_MAX_LAYOUT_MS,
    evidence: [],
    message: `POC responsiveness gate took ${Math.round(durationMs)} ms; maximum is ${POC_MAX_LAYOUT_MS} ms`
  };
}

export async function reviewGraph(graph: GraphModel, source: string, sha256?: string): Promise<GraphReviewResult> {
  const started = performance.now();
  const layout = await computeLayout(graph);
  const durationMs = performance.now() - started;
  const qualityFailures = evaluateLayoutQuality(layout.metrics, layout.edges);
  const timingFailure = responsivenessFailure(durationMs, graph.nodes.length);
  return {
    source,
    sha256,
    schema: graph.schema,
    graphId: graph.id,
    label: graph.label,
    nodeCount: graph.nodes.length,
    edgeCount: graph.edges.length,
    durationMs,
    width: layout.width,
    height: layout.height,
    placementProfileId: layout.placementProfileId,
    metrics: layout.metrics,
    failures: timingFailure ? [...qualityFailures, timingFailure] : qualityFailures
  };
}

export function graphReviewExitCode(results: readonly GraphReviewResult[]): number {
  return results.some((result) => result.failures.length > 0) ? 1 : 0;
}

function fixed(value: number, digits = 2): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(digits);
}

export function formatGraphReviewTable(results: readonly GraphReviewResult[]): string {
  const headers = ['Graph', 'Nodes', 'Edges', 'Time', 'Size', 'Cross', 'Primary', 'All', 'Detour', 'Failures'];
  const rows = results.map((result) => [
    result.label,
    String(result.nodeCount),
    String(result.edgeCount),
    `${Math.round(result.durationMs)} ms`,
    `${Math.round(result.width)}x${Math.round(result.height)}`,
    String(result.metrics.unavoidableCrossings),
    fixed(result.metrics.primaryCrossingDensity ?? result.metrics.crossingDensity),
    fixed(result.metrics.crossingDensity),
    fixed(Math.max(result.metrics.maximumNormalDetourRatio, result.metrics.maximumLongDetourRatio)),
    result.failures.length === 0 ? 'PASS' : result.failures.map((failure) => failure.metric).join(', ')
  ]);
  const widths = headers.map((header, index) => Math.max(header.length, ...rows.map((row) => row[index].length)));
  const line = (values: readonly string[]) => values.map((value, index) => value.padEnd(widths[index])).join('  ').trimEnd();
  return [line(headers), line(widths.map((width) => '-'.repeat(width))), ...rows.map(line)].join('\n');
}
