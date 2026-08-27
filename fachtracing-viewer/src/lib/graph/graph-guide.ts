import type { GraphNode } from '$contracts/graph-contract';
import type { GraphPresentation } from './graph-presentation';

export interface GraphGuideConnection {
  readonly edgeId: string;
  readonly nodeId: string;
  readonly nodeLabel: string;
  readonly outcome: string | null;
}

export interface GraphGuideContext {
  readonly node: GraphNode;
  readonly memberLabels: readonly string[];
  readonly incoming: readonly GraphGuideConnection[];
  readonly outgoing: readonly GraphGuideConnection[];
}

export function businessOutcome(outcome: string): string | null {
  const raw = outcome.split(';', 1)[0].trim();
  const normalized = raw.toLowerCase();
  if (!raw || normalized === 'next') return null;
  if (normalized === 'true' || normalized === 'yes') return 'Yes';
  if (normalized === 'false' || normalized === 'no') return 'No';
  return raw;
}

export function graphGuideContext(presentation: GraphPresentation, nodeId: string | null): GraphGuideContext | null {
  if (!nodeId) return null;
  const node = presentation.graph.nodes.find((item) => item.id === nodeId);
  const info = presentation.nodes.get(nodeId);
  if (!node || !info) return null;
  const nodeById = new Map(presentation.graph.nodes.map((item) => [item.id, item]));
  const connection = (edge: typeof presentation.graph.edges[number], otherNodeId: string): GraphGuideConnection => ({
    edgeId: edge.id,
    nodeId: otherNodeId,
    nodeLabel: nodeById.get(otherNodeId)?.label ?? otherNodeId,
    outcome: businessOutcome(edge.outcome)
  });
  return {
    node,
    memberLabels: info.memberLabels,
    incoming: presentation.graph.edges.filter((edge) => edge.to === nodeId).map((edge) => connection(edge, edge.from)),
    outgoing: presentation.graph.edges.filter((edge) => edge.from === nodeId).map((edge) => connection(edge, edge.to))
  };
}
