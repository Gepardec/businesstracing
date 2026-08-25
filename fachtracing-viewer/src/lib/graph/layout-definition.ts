import type { ELK, ElkNode } from 'elkjs/lib/elk-api';
import type { GraphModel } from '$contracts/graph-contract';
import { measureBranchRegionViolations, measureLayoutQuality, measureRouteDetours, type LayoutQualityMetrics } from './route-quality';
import { placementProfiles, normalizePlacement, selectPlacement, type PlacementProfile, type PlacementRouteScore } from './placement-profiles';
import { analyzeTopology, type DuplicateOccurrence, type TopologyAnalysis } from './topology-analysis';
import { planRoutes, type LayoutJunction, type LayoutPort, type RenderedRoute, type RouteCrossing, type SharedRouteSegment } from './route-planner';

export const NODE_WIDTH = 232;
export const NODE_HEIGHT = 92;
const HORIZONTAL_GAP = 64;
const VERTICAL_GAP = 96;
const PADDING = 32;

export interface LayoutSpacing {
  readonly horizontalGap: number;
  readonly verticalGap: number;
  readonly padding: number;
}

export const DEFAULT_LAYOUT_SPACING: LayoutSpacing = Object.freeze({
  horizontalGap: HORIZONTAL_GAP,
  verticalGap: VERTICAL_GAP,
  padding: PADDING
});

export const LOCAL_LAYOUT_SPACING: LayoutSpacing = Object.freeze({
  horizontalGap: 48,
  verticalGap: 64,
  padding: 24
});

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
  placementProfileId: string;
}

function placementGraph(graph: GraphModel, profile: PlacementProfile, spacing: LayoutSpacing): ElkNode {
  return {
    id: 'root',
    layoutOptions: {
      ...profile.options,
      'elk.spacing.nodeNode': String(spacing.horizontalGap),
      'elk.layered.spacing.nodeNodeBetweenLayers': String(spacing.verticalGap),
      'elk.padding': `[top=${spacing.padding},left=${spacing.padding},bottom=${spacing.padding},right=${spacing.padding}]`
    },
    children: [...graph.nodes]
      .sort((first, second) => first.id.localeCompare(second.id))
      .map((node) => ({ id: node.id, width: NODE_WIDTH, height: NODE_HEIGHT })),
    edges: [...graph.edges]
      .sort((first, second) => first.id.localeCompare(second.id))
      .map((edge) => ({ id: edge.id, sources: [edge.from], targets: [edge.to] }))
  };
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
  }).filter((region) => region.label !== 'Cycle' || !positions.some((position) => {
    if (region.nodeIds.includes(position.id)) return false;
    return position.x < region.x + region.width && position.x + NODE_WIDTH > region.x &&
      position.y < region.y + region.height && position.y + NODE_HEIGHT > region.y;
  }));
}

export async function computeLayoutWith(elk: Pick<ELK, 'layout'>, graph: GraphModel, spacing: LayoutSpacing = DEFAULT_LAYOUT_SPACING): Promise<LayoutResult> {
  const topology = analyzeTopology(graph);
  const candidates = [];
  const plansByProfileId = new Map<string, ReturnType<typeof planRoutes>>();
  const routeScores = new Map<string, PlacementRouteScore>();
  const rejectedProfiles: string[] = [];
  for (const profile of placementProfiles(graph.nodes.length)) {
    const elkResult = await elk.layout(placementGraph(graph, profile, spacing)) as ElkNode;
    const candidate = normalizePlacement(elkResult, profile.id, NODE_WIDTH, NODE_HEIGHT, spacing.padding);
    let plan: ReturnType<typeof planRoutes>;
    try {
      plan = planRoutes(graph, candidate.positions, topology, candidate.width, candidate.height, NODE_WIDTH, NODE_HEIGHT);
    } catch (error) {
      rejectedProfiles.push(`${profile.id}: ${error instanceof Error ? error.message : String(error)}`);
      continue;
    }
    const qualityNodes = candidate.positions.map((position) => ({ ...position, width: NODE_WIDTH, height: NODE_HEIGHT }));
    const routeMetrics = measureLayoutQuality(graph, qualityNodes, plan.routes);
    const detours = measureRouteDetours(plan.routes);
    candidates.push(candidate);
    plansByProfileId.set(profile.id, plan);
    routeScores.set(profile.id, {
      unrelatedNodeIntrusions: routeMetrics.unrelatedNodeIntrusions,
      branchRegionViolations: measureBranchRegionViolations(graph, qualityNodes, plan.routes, topology.branchRegions, topology.longEdgeIds),
      avoidableCrossings: plan.avoidableCrossings,
      crossingDensity: plan.crossings.length / Math.max(1, graph.edges.length),
      maximumDetourRatio: Math.max(1, ...detours.map((route) => route.ratio)),
      totalDetour: detours.reduce((total, route) => total + Math.max(0, route.ratio - 1), 0)
    });
  }
  if (candidates.length === 0) {
    throw new Error(`No placement profile produced a safe graph layout. ${rejectedProfiles.join(' ')}`);
  }
  const compacted = selectPlacement(graph, topology, candidates, NODE_WIDTH, NODE_HEIGHT, routeScores);
  const plan = plansByProfileId.get(compacted.profileId)!;
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
    crossingDensity: plan.crossings.length / Math.max(1, graph.edges.length),
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
    metrics,
    placementProfileId: compacted.profileId
  };
}
