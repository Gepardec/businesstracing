import type { GraphModel } from '$contracts/graph-contract';
import type { RunModel, RunObservation } from '$contracts/run-contract';

export interface RunHighlight {
  activeNodeId: string | null;
  activeEdgeId: string | null;
  pathNodeIds: ReadonlySet<string>;
  pathEdgeIds: ReadonlySet<string>;
  activeSequence: number | null;
}

export function deriveRunHighlight(graph: GraphModel, run: RunModel, activeIndex: number): RunHighlight {
  const active = run.observations[activeIndex] ?? null;
  const pathNodeIds = new Set(run.observations.map((item) => item.nodeId));
  const pathEdgeIds = new Set(run.observations.flatMap((item) => item.selectedEdgeId ? [item.selectedEdgeId] : []));
  for (let index = 0; index < run.observations.length - 1; index++) {
    const current = run.observations[index];
    const next = run.observations[index + 1];
    if (current.selectedEdgeId) continue;
    const candidates = graph.edges.filter((edge) => edge.from === current.nodeId && edge.to === next.nodeId);
    if (candidates.length === 1) pathEdgeIds.add(candidates[0].id);
  }
  return {
    activeNodeId: active?.nodeId ?? null,
    activeEdgeId: active?.selectedEdgeId ?? null,
    pathNodeIds,
    pathEdgeIds,
    activeSequence: active?.sequence ?? null
  };
}

export function explainObservation(graph: GraphModel, observation: RunObservation): string {
  const node = graph.nodes.find((item) => item.id === observation.nodeId);
  const evidence = Object.entries(observation.evidence)
    .map(([name, value]) => `${name} was ${value.displayValue}`)
    .join('; ');
  const outcome = observation.outcome;
  if (evidence) return `${evidence}; ${node?.label ?? observation.nodeId} was ${outcome}`;
  return `${node?.label ?? observation.nodeId}: ${outcome}. No additional evidence was recorded.`;
}
