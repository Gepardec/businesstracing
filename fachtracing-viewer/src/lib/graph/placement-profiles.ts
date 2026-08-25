import type { ElkNode } from 'elkjs/lib/elk-api';
import type { GraphModel } from '$contracts/graph-contract';
import type { TopologyAnalysis } from './topology-analysis';

export interface PlacementProfile {
  id: string;
  options: Readonly<Record<string, string>>;
}

export interface PlacementCandidate {
  profileId: string;
  positions: Array<{ id: string; x: number; y: number }>;
  width: number;
  height: number;
}

export interface PlacementScore {
  nodeOverlaps: number;
  forwardOrderViolations: number;
  unrelatedNodeIntrusions: number;
  branchRegionViolations: number;
  avoidableCrossings: number;
  crossingDensity: number;
  maximumDetourRatio: number;
  totalDetour: number;
  aspectRatioPenalty: number;
  totalEdgeSpan: number;
  area: number;
  profileId: string;
}

export interface PlacementRouteScore {
  unrelatedNodeIntrusions: number;
  branchRegionViolations: number;
  avoidableCrossings: number;
  crossingDensity: number;
  maximumDetourRatio: number;
  totalDetour: number;
}

const EMPTY_ROUTE_SCORE: PlacementRouteScore = {
  unrelatedNodeIntrusions: 0,
  branchRegionViolations: 0,
  avoidableCrossings: 0,
  crossingDensity: 0,
  maximumDetourRatio: 1,
  totalDetour: 0
};

const COMMON_OPTIONS: Readonly<Record<string, string>> = Object.freeze({
  'elk.algorithm': 'layered',
  'elk.direction': 'DOWN',
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  'elk.layered.cycleBreaking.strategy': 'GREEDY',
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  'elk.separateConnectedComponents': 'true'
});

export function placementProfiles(nodeCount: number): PlacementProfile[] {
  const squareLayerBound = Math.max(3, Math.ceil(Math.sqrt(nodeCount)));
  return [
    {
      id: 'network-simplex-balanced',
      options: {
        ...COMMON_OPTIONS,
        'elk.layered.layering.strategy': 'NETWORK_SIMPLEX',
        'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
        'elk.layered.compaction.postCompaction.strategy': 'LEFT_RIGHT_CONSTRAINT_LOCKING'
      }
    },
    {
      id: 'coffman-graham-wide',
      options: {
        ...COMMON_OPTIONS,
        'elk.layered.layering.strategy': 'COFFMAN_GRAHAM',
        'elk.layered.layering.coffmanGraham.layerBound': String(Math.ceil(squareLayerBound * 1.5)),
        'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
        'elk.layered.compaction.postCompaction.strategy': 'LEFT_RIGHT_CONSTRAINT_LOCKING'
      }
    }
  ];
}

export function normalizePlacement(result: ElkNode, profileId: string, nodeWidth: number, nodeHeight: number, padding: number): PlacementCandidate {
  const children = result.children ?? [];
  const minimumX = Math.min(0, ...children.map((node) => node.x ?? 0));
  const minimumY = Math.min(0, ...children.map((node) => node.y ?? 0));
  const positions = children.map((node) => ({
    id: node.id,
    x: (node.x ?? 0) - minimumX + padding,
    y: (node.y ?? 0) - minimumY + padding
  })).sort((first, second) => first.id.localeCompare(second.id));
  return {
    profileId,
    positions,
    width: Math.max(nodeWidth + padding * 2, ...positions.map((position) => position.x + nodeWidth + padding)),
    height: Math.max(nodeHeight + padding * 2, ...positions.map((position) => position.y + nodeHeight + padding))
  };
}

function structuralChain(graph: GraphModel, topology: TopologyAnalysis): boolean {
  const spineRatio = topology.spineNodeIds.length / Math.max(1, graph.nodes.length);
  const maximumBranches = Math.max(0, ...[...topology.outgoingByNodeId.values()].map((edges) => edges.filter((edge) => {
    const target = graph.nodes.find((node) => node.id === edge.to);
    return target?.kind !== 'OUTCOME';
  }).length));
  return spineRatio >= 0.9 && maximumBranches <= 1;
}

function overlaps(first: { x: number; y: number }, second: { x: number; y: number }, nodeWidth: number, nodeHeight: number): boolean {
  return first.x < second.x + nodeWidth && first.x + nodeWidth > second.x && first.y < second.y + nodeHeight && first.y + nodeHeight > second.y;
}

export function scorePlacement(
  graph: GraphModel,
  topology: TopologyAnalysis,
  candidate: PlacementCandidate,
  nodeWidth: number,
  nodeHeight: number,
  routeScore: PlacementRouteScore = EMPTY_ROUTE_SCORE
): PlacementScore {
  const positionById = new Map(candidate.positions.map((position) => [position.id, position]));
  let nodeOverlaps = 0;
  for (let first = 0; first < candidate.positions.length; first += 1) {
    for (let second = first + 1; second < candidate.positions.length; second += 1) {
      if (overlaps(candidate.positions[first], candidate.positions[second], nodeWidth, nodeHeight)) nodeOverlaps += 1;
    }
  }
  let forwardOrderViolations = 0;
  let totalEdgeSpan = 0;
  for (const edge of graph.edges) {
    const source = positionById.get(edge.from)!;
    const target = positionById.get(edge.to)!;
    totalEdgeSpan += Math.abs(target.x - source.x) + Math.abs(target.y - source.y);
    if (!topology.longEdgeIds.has(edge.id) && target.y <= source.y) forwardOrderViolations += 1;
  }
  const aspectRatio = candidate.width / Math.max(1, candidate.height);
  const aspectRatioPenalty = graph.nodes.length < 16 || graph.nodes.length > 100 || structuralChain(graph, topology)
    ? 0
    : aspectRatio < 0.25 ? 0.25 - aspectRatio : aspectRatio > 2.25 ? aspectRatio - 2.25 : 0;
  return {
    nodeOverlaps,
    forwardOrderViolations,
    ...routeScore,
    aspectRatioPenalty,
    totalEdgeSpan,
    area: candidate.width * candidate.height,
    profileId: candidate.profileId
  };
}

export function selectPlacement(
  graph: GraphModel,
  topology: TopologyAnalysis,
  candidates: readonly PlacementCandidate[],
  nodeWidth: number,
  nodeHeight: number,
  routeScores: ReadonlyMap<string, PlacementRouteScore> = new Map()
): PlacementCandidate {
  const profileOrder = new Map(candidates.map((candidate, index) => [candidate.profileId, index]));
  const scored = candidates.map((candidate) => ({
    candidate,
    score: scorePlacement(graph, topology, candidate, nodeWidth, nodeHeight, routeScores.get(candidate.profileId))
  }));
  scored.sort((first, second) =>
    first.score.nodeOverlaps - second.score.nodeOverlaps ||
    first.score.forwardOrderViolations - second.score.forwardOrderViolations ||
    first.score.unrelatedNodeIntrusions - second.score.unrelatedNodeIntrusions ||
    first.score.branchRegionViolations - second.score.branchRegionViolations ||
    first.score.avoidableCrossings - second.score.avoidableCrossings ||
    first.score.crossingDensity - second.score.crossingDensity ||
    first.score.maximumDetourRatio - second.score.maximumDetourRatio ||
    first.score.totalDetour - second.score.totalDetour ||
    first.score.aspectRatioPenalty - second.score.aspectRatioPenalty ||
    (graph.nodes.length < 16 ? profileOrder.get(first.score.profileId)! - profileOrder.get(second.score.profileId)! : 0) ||
    first.score.totalEdgeSpan - second.score.totalEdgeSpan ||
    first.score.area - second.score.area ||
    first.score.profileId.localeCompare(second.score.profileId)
  );
  return scored[0].candidate;
}
