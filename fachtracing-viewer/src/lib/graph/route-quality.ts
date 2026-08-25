import type { GraphModel } from '$contracts/graph-contract';
import type { LayoutPoint } from './edge-route';

export interface QualityNode {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface QualityRoute {
  id: string;
  sourceNodeId?: string;
  targetNodeId?: string;
  points: readonly LayoutPoint[];
  labelPosition?: LayoutPoint;
  displayLabel?: string | null;
  long?: boolean;
  corridor?: 'normal' | 'outer' | 'cycle';
  shortestCandidateLength?: number;
}

export interface LayoutQualityMetrics {
  nodeOverlaps: number;
  unrelatedNodeIntrusions: number;
  labelCollisions: number;
  avoidableCrossings: number;
  unavoidableCrossings: number;
  totalManhattanLength: number;
  totalBends: number;
  backtrackingDistance: number;
  longEdgeCorridorViolations: number;
  branchRegionViolations: number;
  crossingDensity: number;
  parallelCorridorDensity: number;
  maximumNormalDetourRatio: number;
  maximumLongDetourRatio: number;
  avoidableDetours: number;
}

export interface RouteDetour {
  routeId: string;
  corridor: 'normal' | 'outer' | 'cycle';
  length: number;
  shortestCandidateLength: number;
  ratio: number;
  avoidable: boolean;
}

export interface LayoutQualityFailure {
  metric: keyof LayoutQualityMetrics;
  actual: number;
  maximum: number;
  evidence: readonly string[];
  message: string;
}

export interface QualityBranchRegion {
  sourceNodeId: string;
  memberNodeIds: readonly string[];
  convergenceNodeId: string | null;
}

export function manhattanLength(points: readonly LayoutPoint[]): number {
  return points.slice(1).reduce((total, point, index) => total + Math.abs(point.x - points[index].x) + Math.abs(point.y - points[index].y), 0);
}

export function bendCount(points: readonly LayoutPoint[]): number {
  let bends = 0;
  for (let index = 1; index < points.length - 1; index += 1) {
    const previous = points[index - 1];
    const point = points[index];
    const next = points[index + 1];
    if ((previous.x === point.x) !== (point.x === next.x)) bends += 1;
  }
  return bends;
}

export function parallelClearanceViolations(routes: readonly QualityRoute[], clearance = 12): string[] {
  return parallelClearancePairs(routes, clearance).map(([first, second]) => `${first} is too close to ${second}`);
}

export function parallelClearancePairs(routes: readonly QualityRoute[], clearance = 12): Array<readonly [string, string]> {
  const violations: Array<readonly [string, string]> = [];
  for (let first = 0; first < routes.length; first += 1) {
    for (let second = first + 1; second < routes.length; second += 1) {
      if (routes[first].sourceNodeId && routes[first].sourceNodeId === routes[second].sourceNodeId) continue;
      if (routes[first].targetNodeId && routes[first].targetNodeId === routes[second].targetNodeId) continue;
      if (routes[first].sourceNodeId && routes[first].sourceNodeId === routes[second].targetNodeId) continue;
      if (routes[first].targetNodeId && routes[first].targetNodeId === routes[second].sourceNodeId) continue;
      let violates = false;
      for (let firstSegment = 1; firstSegment < routes[first].points.length && !violates; firstSegment += 1) {
        const firstStart = routes[first].points[firstSegment - 1];
        const firstEnd = routes[first].points[firstSegment];
        for (let secondSegment = 1; secondSegment < routes[second].points.length; secondSegment += 1) {
          const secondStart = routes[second].points[secondSegment - 1];
          const secondEnd = routes[second].points[secondSegment];
          const firstVertical = firstStart.x === firstEnd.x;
          const secondVertical = secondStart.x === secondEnd.x;
          const distance = firstVertical
            ? Math.abs(firstStart.x - secondStart.x)
            : Math.abs(firstStart.y - secondStart.y);
          const overlap = firstVertical
            ? rangesOverlap(firstStart.y, firstEnd.y, secondStart.y, secondEnd.y)
            : rangesOverlap(firstStart.x, firstEnd.x, secondStart.x, secondEnd.x);
          if (firstVertical === secondVertical && overlap && distance < clearance) {
            violates = true;
            break;
          }
        }
      }
      if (violates) violations.push([routes[first].id, routes[second].id]);
    }
  }
  return violations;
}

export function measureRouteDetours(routes: readonly QualityRoute[]): RouteDetour[] {
  return routes.map((route) => {
    const length = manhattanLength(route.points);
    const shortestCandidateLength = Math.max(1, route.shortestCandidateLength ?? length);
    const improvement = length - shortestCandidateLength;
    const ratio = length / shortestCandidateLength;
    return {
      routeId: route.id,
      corridor: route.corridor ?? 'normal',
      length,
      shortestCandidateLength,
      ratio,
      avoidable: improvement >= 48 || ratio >= 1.15
    };
  });
}

function failedMetric(
  metric: keyof LayoutQualityMetrics,
  actual: number,
  maximum: number,
  evidence: readonly string[] = []
): LayoutQualityFailure | null {
  if (actual <= maximum) return null;
  const evidenceText = evidence.length > 0 ? ` (${evidence.join(', ')})` : '';
  return {
    metric,
    actual,
    maximum,
    evidence,
    message: `${metric} is ${actual.toFixed(3)}; maximum is ${maximum.toFixed(3)}${evidenceText}`
  };
}

export function evaluateLayoutQuality(metrics: LayoutQualityMetrics, routes: readonly QualityRoute[]): LayoutQualityFailure[] {
  const detours = measureRouteDetours(routes);
  const normalDetours = detours.filter((route) => route.corridor === 'normal' && route.ratio > 2).map((route) => route.routeId);
  const longDetours = detours.filter((route) => route.corridor !== 'normal' && route.ratio > 3).map((route) => route.routeId);
  const avoidableDetours = detours.filter((route) => route.avoidable).map((route) => route.routeId);
  return [
    failedMetric('nodeOverlaps', metrics.nodeOverlaps, 0),
    failedMetric('unrelatedNodeIntrusions', metrics.unrelatedNodeIntrusions, 0),
    failedMetric('labelCollisions', metrics.labelCollisions, 0),
    failedMetric('avoidableCrossings', metrics.avoidableCrossings, 0),
    failedMetric('branchRegionViolations', metrics.branchRegionViolations, 0),
    failedMetric('longEdgeCorridorViolations', metrics.longEdgeCorridorViolations, 0),
    failedMetric('crossingDensity', metrics.crossingDensity, 0.5),
    failedMetric('maximumNormalDetourRatio', metrics.maximumNormalDetourRatio, 2, normalDetours),
    failedMetric('maximumLongDetourRatio', metrics.maximumLongDetourRatio, 3, longDetours),
    failedMetric('avoidableDetours', metrics.avoidableDetours, 0, avoidableDetours)
  ].filter((failure): failure is LayoutQualityFailure => failure !== null);
}

export function measureBranchRegionViolations(
  graph: GraphModel,
  nodes: readonly QualityNode[],
  routes: readonly QualityRoute[],
  regions: readonly QualityBranchRegion[],
  longEdgeIds: ReadonlySet<string>
): number {
  return branchRegionViolationEdgeIds(graph, nodes, routes, regions, longEdgeIds).length;
}

export function branchRegionViolationEdgeIds(
  graph: GraphModel,
  nodes: readonly QualityNode[],
  routes: readonly QualityRoute[],
  regions: readonly QualityBranchRegion[],
  longEdgeIds: ReadonlySet<string>
): string[] {
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  const routeById = new Map(routes.map((route) => [route.id, route]));
  const violatingEdgeIds = new Set<string>();
  for (const region of regions) {
    const memberIds = new Set([region.sourceNodeId, ...region.memberNodeIds, ...(region.convergenceNodeId ? [region.convergenceNodeId] : [])]);
    const members = [...memberIds].map((nodeId) => nodeById.get(nodeId)).filter((node): node is QualityNode => Boolean(node));
    if (members.length === 0) continue;
    const left = Math.min(...members.map((node) => node.x)) - 96;
    const right = Math.max(...members.map((node) => node.x + node.width)) + 96;
    const top = Math.min(...members.map((node) => node.y)) - 96;
    const bottom = Math.max(...members.map((node) => node.y + node.height)) + 96;
    for (const edge of graph.edges) {
      const route = routeById.get(edge.id);
      if (longEdgeIds.has(edge.id) || route?.long || route?.corridor !== 'normal' || !memberIds.has(edge.from) || !memberIds.has(edge.to)) continue;
      if (route?.points.some((point) => point.x < left || point.x > right || point.y < top || point.y > bottom)) violatingEdgeIds.add(edge.id);
    }
  }
  return [...violatingEdgeIds].sort();
}

function rangesOverlap(firstStart: number, firstEnd: number, secondStart: number, secondEnd: number): boolean {
  return Math.max(Math.min(firstStart, firstEnd), Math.min(secondStart, secondEnd)) < Math.min(Math.max(firstStart, firstEnd), Math.max(secondStart, secondEnd));
}

function segmentEntersNode(start: LayoutPoint, end: LayoutPoint, node: QualityNode): boolean {
  const left = node.x;
  const right = node.x + node.width;
  const top = node.y;
  const bottom = node.y + node.height;
  if (start.x === end.x) return start.x > left && start.x < right && rangesOverlap(start.y, end.y, top, bottom);
  if (start.y === end.y) return start.y > top && start.y < bottom && rangesOverlap(start.x, end.x, left, right);
  return false;
}

function nodeOverlap(first: QualityNode, second: QualityNode): boolean {
  return first.x < second.x + second.width && first.x + first.width > second.x && first.y < second.y + second.height && first.y + first.height > second.y;
}

function estimatedLabelBox(route: QualityRoute): QualityNode | null {
  if (!route.labelPosition || !route.displayLabel) return null;
  const width = Math.min(148, Math.max(30, 20 + route.displayLabel.length * 7));
  const height = 22;
  return {
    id: route.id,
    x: route.labelPosition.x - width / 2,
    y: route.labelPosition.y - height / 2,
    width,
    height
  };
}

function properCrossing(firstStart: LayoutPoint, firstEnd: LayoutPoint, secondStart: LayoutPoint, secondEnd: LayoutPoint): boolean {
  const firstVertical = firstStart.x === firstEnd.x;
  const secondVertical = secondStart.x === secondEnd.x;
  if (firstVertical === secondVertical) return false;
  const verticalStart = firstVertical ? firstStart : secondStart;
  const verticalEnd = firstVertical ? firstEnd : secondEnd;
  const horizontalStart = firstVertical ? secondStart : firstStart;
  const horizontalEnd = firstVertical ? secondEnd : firstEnd;
  return verticalStart.x > Math.min(horizontalStart.x, horizontalEnd.x) && verticalStart.x < Math.max(horizontalStart.x, horizontalEnd.x) &&
    horizontalStart.y > Math.min(verticalStart.y, verticalEnd.y) && horizontalStart.y < Math.max(verticalStart.y, verticalEnd.y);
}

export function measureLayoutQuality(graph: GraphModel, nodes: readonly QualityNode[], routes: readonly QualityRoute[]): LayoutQualityMetrics {
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  const graphEdgeById = new Map(graph.edges.map((edge) => [edge.id, edge]));
  let nodeOverlaps = 0;
  let unrelatedNodeIntrusions = 0;
  let crossings = 0;
  let totalManhattanLength = 0;
  let totalBends = 0;
  let backtrackingDistance = 0;
  let labelCollisions = 0;
  let longEdgeCorridorViolations = 0;
  const routeDetours = measureRouteDetours(routes);
  const normalDetours = routeDetours.filter((route) => route.corridor === 'normal');
  const longDetours = routeDetours.filter((route) => route.corridor !== 'normal');
  const maximumX = Math.max(...nodes.map((node) => node.x + node.width));
  const maximumY = Math.max(...nodes.map((node) => node.y + node.height));

  for (let first = 0; first < nodes.length; first += 1) {
    for (let second = first + 1; second < nodes.length; second += 1) {
      if (nodeOverlap(nodes[first], nodes[second])) nodeOverlaps += 1;
    }
  }

  for (const route of routes) {
    totalManhattanLength += manhattanLength(route.points);
    totalBends += bendCount(route.points);
    const edge = graphEdgeById.get(route.id);
    if (!edge) continue;
    if (route.corridor === 'outer' && route.points.every((point) => point.x >= 0 && point.x <= maximumX && point.y >= 0 && point.y <= maximumY)) {
      longEdgeCorridorViolations += 1;
    }
    const source = nodeById.get(edge.from);
    const target = nodeById.get(edge.to);
    if (source && target && target.y >= source.y) {
      for (let index = 1; index < route.points.length; index += 1) {
        if (route.points[index].y < route.points[index - 1].y) backtrackingDistance += route.points[index - 1].y - route.points[index].y;
      }
    }
    for (let index = 1; index < route.points.length; index += 1) {
      for (const node of nodes) {
        if (node.id === edge.from || node.id === edge.to) continue;
        if (segmentEntersNode(route.points[index - 1], route.points[index], node)) unrelatedNodeIntrusions += 1;
      }
    }
  }


  const labels = routes.map(estimatedLabelBox).filter((label): label is QualityNode => label !== null);
  for (let first = 0; first < labels.length; first += 1) {
    if (nodes.some((node) => nodeOverlap(labels[first], node))) labelCollisions += 1;
    for (const route of routes) {
      if (route.id === labels[first].id) continue;
      if (route.points.slice(1).some((point, index) => segmentEntersNode(route.points[index], point, labels[first]))) labelCollisions += 1;
    }
    for (let second = first + 1; second < labels.length; second += 1) {
      if (nodeOverlap(labels[first], labels[second])) labelCollisions += 1;
    }
  }

  for (let first = 0; first < routes.length; first += 1) {
    for (let second = first + 1; second < routes.length; second += 1) {
      for (let firstSegment = 1; firstSegment < routes[first].points.length; firstSegment += 1) {
        for (let secondSegment = 1; secondSegment < routes[second].points.length; secondSegment += 1) {
          if (properCrossing(routes[first].points[firstSegment - 1], routes[first].points[firstSegment], routes[second].points[secondSegment - 1], routes[second].points[secondSegment])) crossings += 1;
        }
      }
    }
  }

  return {
    nodeOverlaps,
    unrelatedNodeIntrusions,
    labelCollisions,
    avoidableCrossings: crossings,
    unavoidableCrossings: 0,
    totalManhattanLength,
    totalBends,
    backtrackingDistance,
    longEdgeCorridorViolations,
    branchRegionViolations: 0,
    crossingDensity: crossings / Math.max(1, graph.edges.length),
    parallelCorridorDensity: parallelClearancePairs(routes).length / Math.max(1, graph.edges.length),
    maximumNormalDetourRatio: Math.max(1, ...normalDetours.map((route) => route.ratio)),
    maximumLongDetourRatio: Math.max(1, ...longDetours.map((route) => route.ratio)),
    avoidableDetours: routeDetours.filter((route) => route.avoidable).length
  };
}
