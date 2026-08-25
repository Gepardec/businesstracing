import type { ELK, ElkNode } from 'elkjs/lib/elk-api';
import type { GraphModel } from '$contracts/graph-contract';
import { measureBranchRegionViolations, measureLayoutQuality, type LayoutQualityMetrics } from './route-quality';
import { analyzeTopology, type DuplicateOccurrence, type TopologyAnalysis } from './topology-analysis';
import { planRoutes, type LayoutJunction, type LayoutPort, type RenderedRoute, type RouteCrossing, type SharedRouteSegment } from './route-planner';

export const NODE_WIDTH = 232;
export const NODE_HEIGHT = 92;
const HORIZONTAL_GAP = 64;
const VERTICAL_GAP = 96;
const COMPONENT_GAP = 96;
const PADDING = 32;

export interface PositionedNode {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  ports: readonly LayoutPort[];
  occurrence: DuplicateOccurrence | null;
  incomingCount: number;
  outgoingCount: number;
}

export interface PositionedRegion {
  id: string;
  label: 'Cycle' | 'Component';
  nodeIds: readonly string[];
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface LayoutResult {
  nodes: PositionedNode[];
  edges: RenderedRoute[];
  junctions: LayoutJunction[];
  sharedSegments: SharedRouteSegment[];
  crossings: RouteCrossing[];
  regions: PositionedRegion[];
  width: number;
  height: number;
  metrics: LayoutQualityMetrics;
}

function placementGraph(graph: GraphModel): ElkNode {
  return {
    id: 'root',
    layoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'DOWN',
      'elk.spacing.nodeNode': String(HORIZONTAL_GAP),
      'elk.layered.spacing.nodeNodeBetweenLayers': String(VERTICAL_GAP),
      'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
      'elk.layered.cycleBreaking.strategy': 'GREEDY',
      'elk.layered.layering.strategy': 'NETWORK_SIMPLEX',
      'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
      'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
      'elk.layered.compaction.postCompaction.strategy': 'LEFT_RIGHT_CONSTRAINT_LOCKING',
      'elk.separateConnectedComponents': 'true',
      'elk.padding': `[top=${PADDING},left=${PADDING},bottom=${PADDING},right=${PADDING}]`
    },
    children: [...graph.nodes]
      .sort((first, second) => first.id.localeCompare(second.id))
      .map((node) => ({ id: node.id, width: NODE_WIDTH, height: NODE_HEIGHT })),
    edges: [...graph.edges]
      .sort((first, second) => first.id.localeCompare(second.id))
      .map((edge) => ({ id: edge.id, sources: [edge.from], targets: [edge.to] }))
  };
}

function compactPositions(result: ElkNode, graph: GraphModel, topology: TopologyAnalysis): { positions: Array<{ id: string; x: number; y: number }>; width: number; height: number } {
  const elkX = new Map((result.children ?? []).map((node) => [node.id, node.x ?? 0]));
  const componentIds = [...new Set(topology.componentByNodeId.values())];
  const entryComponents = new Set(graph.entryNodeIds.map((nodeId) => topology.componentByNodeId.get(nodeId)!));
  componentIds.sort((first, second) => {
    const firstEntry = entryComponents.has(first) ? 0 : 1;
    const secondEntry = entryComponents.has(second) ? 0 : 1;
    return firstEntry - secondEntry || first.localeCompare(second);
  });

  const positions: Array<{ id: string; x: number; y: number }> = [];
  let componentOffset = PADDING;
  let maximumRank = 0;
  for (const componentId of componentIds) {
    const componentNodes = graph.nodes.filter((node) => topology.componentByNodeId.get(node.id) === componentId);
    const rankGroups = new Map<number, string[]>();
    for (const node of componentNodes) {
      const rank = topology.rankByNodeId.get(node.id) ?? 0;
      rankGroups.set(rank, [...(rankGroups.get(rank) ?? []), node.id]);
      maximumRank = Math.max(maximumRank, rank);
    }
    for (const nodeIds of rankGroups.values()) nodeIds.sort((first, second) => elkX.get(first)! - elkX.get(second)! || first.localeCompare(second));
    const componentWidth = Math.max(NODE_WIDTH, ...[...rankGroups.values()].map((nodeIds) => nodeIds.length * NODE_WIDTH + Math.max(0, nodeIds.length - 1) * HORIZONTAL_GAP));
    for (const [rank, nodeIds] of [...rankGroups].sort(([first], [second]) => first - second)) {
      const rankWidth = nodeIds.length * NODE_WIDTH + Math.max(0, nodeIds.length - 1) * HORIZONTAL_GAP;
      const rankOffset = componentOffset + (componentWidth - rankWidth) / 2;
      nodeIds.forEach((nodeId, index) => positions.push({
        id: nodeId,
        x: rankOffset + index * (NODE_WIDTH + HORIZONTAL_GAP),
        y: PADDING + rank * (NODE_HEIGHT + VERTICAL_GAP)
      }));
    }
    componentOffset += componentWidth + COMPONENT_GAP;
  }
  const width = Math.max(PADDING * 2 + NODE_WIDTH, componentOffset - COMPONENT_GAP + PADDING);
  const height = PADDING * 2 + NODE_HEIGHT + maximumRank * (NODE_HEIGHT + VERTICAL_GAP);
  return { positions: positions.sort((first, second) => first.id.localeCompare(second.id)), width, height };
}

function positionedRegions(graph: GraphModel, topology: TopologyAnalysis, positions: readonly { id: string; x: number; y: number }[]): PositionedRegion[] {
  const positionById = new Map(positions.map((position) => [position.id, position]));
  const regions: Array<{ id: string; label: 'Cycle' | 'Component'; nodeIds: readonly string[] }> = [];
  for (const component of topology.stronglyConnectedComponents) {
    if (component.cyclic && component.nodeIds.length > 1) regions.push({ id: `region-${component.id}`, label: 'Cycle', nodeIds: component.nodeIds });
  }
  const entryComponents = new Set(graph.entryNodeIds.map((nodeId) => topology.componentByNodeId.get(nodeId)!));
  for (const componentId of [...new Set(topology.componentByNodeId.values())].sort()) {
    if (entryComponents.has(componentId)) continue;
    regions.push({
      id: `region-${componentId}`,
      label: 'Component',
      nodeIds: graph.nodes.filter((node) => topology.componentByNodeId.get(node.id) === componentId).map((node) => node.id)
    });
  }
  return regions.map((region) => {
    const members = region.nodeIds.map((nodeId) => positionById.get(nodeId)!).filter(Boolean);
    const left = Math.min(...members.map((position) => position.x)) - 18;
    const top = Math.min(...members.map((position) => position.y)) - 28;
    const right = Math.max(...members.map((position) => position.x + NODE_WIDTH)) + 18;
    const bottom = Math.max(...members.map((position) => position.y + NODE_HEIGHT)) + 18;
    return { ...region, x: left, y: top, width: right - left, height: bottom - top };
  });
}

export async function computeLayoutWith(elk: Pick<ELK, 'layout'>, graph: GraphModel): Promise<LayoutResult> {
  const topology = analyzeTopology(graph);
  const elkResult = await elk.layout(placementGraph(graph)) as ElkNode;
  const compacted = compactPositions(elkResult, graph, topology);
  const plan = planRoutes(graph, compacted.positions, topology, compacted.width, compacted.height, NODE_WIDTH, NODE_HEIGHT);
  const portsByNodeId = new Map<string, LayoutPort[]>();
  for (const route of plan.routes) {
    portsByNodeId.set(route.sourcePort.nodeId, [...(portsByNodeId.get(route.sourcePort.nodeId) ?? []), route.sourcePort]);
    portsByNodeId.set(route.targetPort.nodeId, [...(portsByNodeId.get(route.targetPort.nodeId) ?? []), route.targetPort]);
  }
  const nodes = compacted.positions.map((position): PositionedNode => ({
    ...position,
    width: NODE_WIDTH,
    height: NODE_HEIGHT,
    ports: [...new Map((portsByNodeId.get(position.id) ?? []).map((port) => [port.id, port])).values()]
      .sort((first, second) => first.id.localeCompare(second.id)),
    occurrence: topology.duplicateByNodeId.get(position.id) ?? null,
    incomingCount: topology.incomingByNodeId.get(position.id)?.length ?? 0,
    outgoingCount: topology.outgoingByNodeId.get(position.id)?.length ?? 0
  }));
  const measured = measureLayoutQuality(graph, nodes, plan.routes);
  const metrics: LayoutQualityMetrics = {
    ...measured,
    avoidableCrossings: plan.avoidableCrossings,
    unavoidableCrossings: plan.crossings.length,
    branchRegionViolations: measureBranchRegionViolations(graph, nodes, plan.routes, topology.branchRegions, topology.longEdgeIds)
  };
  return {
    nodes,
    edges: plan.routes,
    junctions: plan.junctions,
    sharedSegments: plan.sharedSegments,
    crossings: plan.crossings,
    regions: positionedRegions(graph, topology, compacted.positions),
    width: compacted.width,
    height: compacted.height,
    metrics
  };
}
