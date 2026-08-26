import type { GraphEdge, GraphModel } from '$contracts/graph-contract';
import { displayedEdgeLabel } from './edge-label';
import { bendCount, manhattanLength } from './route-quality';
import { routeLabelPosition, type LayoutPoint } from './edge-route';
import type { TopologyAnalysis } from './topology-analysis';

export type PortSide = 'north' | 'east' | 'south' | 'west';

export interface LayoutNodePosition {
  id: string;
  x: number;
  y: number;
}

export interface LayoutPort {
  id: string;
  nodeId: string;
  edgeId: string;
  role: 'source' | 'target';
  side: PortSide;
  slot: number;
  point: LayoutPoint;
}

export interface LayoutJunction {
  id: string;
  targetNodeId: string;
  incomingEdgeIds: readonly string[];
  point: LayoutPoint;
}

export interface SharedRouteSegment {
  id: string;
  junctionId: string;
  incomingEdgeIds: readonly string[];
  points: readonly LayoutPoint[];
  lanePoints: readonly LayoutPoint[];
}

export interface RouteCrossing {
  id: string;
  overEdgeId: string;
  underEdgeId: string;
  point: LayoutPoint;
  overAxis: 'horizontal' | 'vertical';
  radius: number;
}

export interface RenderedRoute {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
  points: LayoutPoint[];
  labelPosition: LayoutPoint;
  labelAnchor: LayoutPoint;
  displayLabel: string | null;
  sourcePort: LayoutPort;
  targetPort: LayoutPort;
  sharedSegmentIds: readonly string[];
  crossingIds: readonly string[];
  length: number;
  shortestCandidateLength: number;
  bends: number;
  long: boolean;
  secondary: boolean;
  branch: boolean;
  corridor: 'normal' | 'outer' | 'cycle';
}

export interface RoutePlan {
  routes: RenderedRoute[];
  junctions: LayoutJunction[];
  sharedSegments: SharedRouteSegment[];
  crossings: RouteCrossing[];
  avoidableCrossings: number;
}

interface NodeBox extends LayoutNodePosition {
  width: number;
  height: number;
}

interface CandidateRoute {
  sourcePort: LayoutPort;
  targetPort: LayoutPort;
  points: LayoutPoint[];
  intrusions: number;
  terminalReversals: number;
  corridorMismatch: number;
  shortSegments: number;
  wrongWayExcursion: number;
  flowPortMismatch: number;
  crossings: number;
  congestion: number;
  length: number;
  bends: number;
  backtracking: number;
  usesOuterCorridor: boolean;
}

interface LabelBox {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

const NODE_CLEARANCE = 16;
const PORT_LEAD = 28;
const OUTER_CORRIDOR_GAP = 40;
const PORT_SLOT_GAP = 12;

const LABEL_OFFSETS: readonly LayoutPoint[] = [-24, -16, -8, 0, 8, 16, 24]
  .flatMap((x) => [-24, -16, -8, 0, 8, 16, 24].map((y) => ({ x, y })))
  .filter(({ x, y }) => Math.hypot(x, y) <= 24)
  .sort((first, second) => Math.hypot(first.x, first.y) - Math.hypot(second.x, second.y) || first.y - second.y || first.x - second.x);

function pointEqual(first: LayoutPoint, second: LayoutPoint): boolean {
  return first.x === second.x && first.y === second.y;
}

function simplify(points: readonly LayoutPoint[]): LayoutPoint[] {
  const distinct = points.filter((point, index) => index === 0 || !pointEqual(point, points[index - 1]));
  const result: LayoutPoint[] = [];
  for (const point of distinct) {
    const previous = result.at(-1);
    const beforePrevious = result.at(-2);
    const verticalBetween = beforePrevious && previous && beforePrevious.x === previous.x && previous.x === point.x &&
      previous.y >= Math.min(beforePrevious.y, point.y) && previous.y <= Math.max(beforePrevious.y, point.y);
    const horizontalBetween = beforePrevious && previous && beforePrevious.y === previous.y && previous.y === point.y &&
      previous.x >= Math.min(beforePrevious.x, point.x) && previous.x <= Math.max(beforePrevious.x, point.x);
    if (verticalBetween || horizontalBetween) {
      result[result.length - 1] = point;
    } else {
      result.push(point);
    }
  }
  return result;
}

function portCapacity(side: PortSide, box: NodeBox): number {
  const span = side === 'north' || side === 'south' ? box.width : box.height;
  return Math.max(1, Math.floor((span - 32) / PORT_SLOT_GAP) + 1);
}

function portPoint(box: NodeBox, side: PortSide, edgeIndex: number, edgeCount: number): LayoutPoint {
  const horizontal = side === 'north' || side === 'south';
  const span = horizontal ? box.width : box.height;
  const count = Math.max(1, Math.min(edgeCount, portCapacity(side, box)));
  const slot = Math.min(edgeIndex, count - 1);
  const used = (count - 1) * PORT_SLOT_GAP;
  const offset = (span - used) / 2 + slot * PORT_SLOT_GAP;
  if (side === 'north') return { x: box.x + offset, y: box.y };
  if (side === 'south') return { x: box.x + offset, y: box.y + box.height };
  if (side === 'west') return { x: box.x, y: box.y + offset };
  return { x: box.x + box.width, y: box.y + offset };
}

function createPort(box: NodeBox, edge: GraphEdge, role: 'source' | 'target', side: PortSide, index: number, count: number): LayoutPort {
  return {
    id: `${box.id}::${role}::${edge.id}::${side}`,
    nodeId: box.id,
    edgeId: edge.id,
    role,
    side,
    slot: index,
    point: portPoint(box, side, index, count)
  };
}

function lead(point: LayoutPoint, side: PortSide): LayoutPoint {
  if (side === 'north') return { x: point.x, y: point.y - PORT_LEAD };
  if (side === 'south') return { x: point.x, y: point.y + PORT_LEAD };
  if (side === 'west') return { x: point.x - PORT_LEAD, y: point.y };
  return { x: point.x + PORT_LEAD, y: point.y };
}

function orthogonalCore(
  start: LayoutPoint,
  end: LayoutPoint,
  bounds: { left: number; right: number; top: number; bottom: number },
  lane: number,
  laneXs: readonly number[],
  laneYs: readonly number[],
  includeOuterCorridors: boolean
): LayoutPoint[][] {
  const middleX = (start.x + end.x) / 2;
  const middleY = (start.y + end.y) / 2;
  const left = bounds.left - OUTER_CORRIDOR_GAP - lane * PORT_SLOT_GAP;
  const right = bounds.right + OUTER_CORRIDOR_GAP + lane * PORT_SLOT_GAP;
  const top = bounds.top - OUTER_CORRIDOR_GAP - lane * PORT_SLOT_GAP;
  const bottom = bounds.bottom + OUTER_CORRIDOR_GAP + lane * PORT_SLOT_GAP;
  const candidates: LayoutPoint[][] = [];
  if (start.x === end.x || start.y === end.y) candidates.push([start, end]);
  candidates.push(
    [start, { x: end.x, y: start.y }, end],
    [start, { x: start.x, y: end.y }, end],
    [start, { x: middleX, y: start.y }, { x: middleX, y: end.y }, end],
    [start, { x: start.x, y: middleY }, { x: end.x, y: middleY }, end]
  );
  if (includeOuterCorridors) candidates.push(
    [start, { x: left, y: start.y }, { x: left, y: end.y }, end],
    [start, { x: right, y: start.y }, { x: right, y: end.y }, end],
    [start, { x: start.x, y: top }, { x: end.x, y: top }, end],
    [start, { x: start.x, y: bottom }, { x: end.x, y: bottom }, end]
  );
  for (const x of laneXs) candidates.push([start, { x, y: start.y }, { x, y: end.y }, end]);
  for (const y of laneYs) candidates.push([start, { x: start.x, y }, { x: end.x, y }, end]);
  return candidates.map(simplify);
}

type GridDirection = 'horizontal' | 'vertical' | 'start';

interface GridState {
  readonly xIndex: number;
  readonly yIndex: number;
  readonly direction: GridDirection;
  readonly cost: number;
  readonly estimate: number;
  readonly previous: string | null;
}

function gridStateKey(xIndex: number, yIndex: number, direction: GridDirection): string {
  return `${xIndex}:${yIndex}:${direction}`;
}

function insideExpandedBox(point: LayoutPoint, box: NodeBox): boolean {
  return point.x > box.x - NODE_CLEARANCE && point.x < box.x + box.width + NODE_CLEARANCE &&
    point.y > box.y - NODE_CLEARANCE && point.y < box.y + box.height + NODE_CLEARANCE;
}

function obstacleAvoidingCore(
  start: LayoutPoint,
  end: LayoutPoint,
  boxes: readonly NodeBox[],
  bounds: { left: number; right: number; top: number; bottom: number },
  lane: number
): LayoutPoint[] | null {
  const outerGap = OUTER_CORRIDOR_GAP + lane * PORT_SLOT_GAP;
  const xs = [...new Set([
    start.x, end.x, bounds.left - outerGap, bounds.right + outerGap,
    ...boxes.flatMap((box) => [box.x - NODE_CLEARANCE, box.x + box.width + NODE_CLEARANCE])
  ])].sort((first, second) => first - second);
  const ys = [...new Set([
    start.y, end.y, bounds.top - outerGap, bounds.bottom + outerGap,
    ...boxes.flatMap((box) => [box.y - NODE_CLEARANCE, box.y + box.height + NODE_CLEARANCE])
  ])].sort((first, second) => first - second);
  const startX = xs.indexOf(start.x);
  const startY = ys.indexOf(start.y);
  const endX = xs.indexOf(end.x);
  const endY = ys.indexOf(end.y);
  const queue: GridState[] = [{
    xIndex: startX,
    yIndex: startY,
    direction: 'start',
    cost: 0,
    estimate: Math.abs(start.x - end.x) + Math.abs(start.y - end.y),
    previous: null
  }];
  const bestCost = new Map<string, number>([[gridStateKey(startX, startY, 'start'), 0]]);
  const states = new Map<string, GridState>();

  while (queue.length > 0) {
    queue.sort((first, second) => first.estimate - second.estimate || first.cost - second.cost);
    const current = queue.shift()!;
    const currentKey = gridStateKey(current.xIndex, current.yIndex, current.direction);
    if (current.cost !== bestCost.get(currentKey)) continue;
    states.set(currentKey, current);
    if (current.xIndex === endX && current.yIndex === endY) {
      const path: LayoutPoint[] = [];
      let state: GridState | undefined = current;
      while (state) {
        path.push({ x: xs[state.xIndex], y: ys[state.yIndex] });
        state = state.previous ? states.get(state.previous) : undefined;
      }
      return simplify(path.reverse());
    }

    const neighbours = [
      { xIndex: current.xIndex - 1, yIndex: current.yIndex, direction: 'horizontal' as const },
      { xIndex: current.xIndex + 1, yIndex: current.yIndex, direction: 'horizontal' as const },
      { xIndex: current.xIndex, yIndex: current.yIndex - 1, direction: 'vertical' as const },
      { xIndex: current.xIndex, yIndex: current.yIndex + 1, direction: 'vertical' as const }
    ].filter(({ xIndex, yIndex }) => xIndex >= 0 && xIndex < xs.length && yIndex >= 0 && yIndex < ys.length);
    const currentPoint = { x: xs[current.xIndex], y: ys[current.yIndex] };
    for (const neighbour of neighbours) {
      const point = { x: xs[neighbour.xIndex], y: ys[neighbour.yIndex] };
      if (boxes.some((box) => insideExpandedBox(point, box) || segmentIntersectsBox(currentPoint, point, box))) continue;
      const distance = Math.abs(point.x - currentPoint.x) + Math.abs(point.y - currentPoint.y);
      const bendPenalty = current.direction === 'start' || current.direction === neighbour.direction ? 0 : 8;
      const cost = current.cost + distance + bendPenalty;
      const key = gridStateKey(neighbour.xIndex, neighbour.yIndex, neighbour.direction);
      if (cost >= (bestCost.get(key) ?? Number.POSITIVE_INFINITY)) continue;
      bestCost.set(key, cost);
      queue.push({
        ...neighbour,
        cost,
        estimate: cost + Math.abs(point.x - end.x) + Math.abs(point.y - end.y),
        previous: currentKey
      });
    }
  }
  return null;
}

function internalLaneCoordinates(
  boxes: readonly NodeBox[],
  routed: readonly RenderedRoute[],
  bounds: { left: number; right: number; top: number; bottom: number },
  axis: 'x' | 'y',
  center: number
): number[] {
  const minimum = axis === 'x' ? bounds.left : bounds.top;
  const maximum = axis === 'x' ? bounds.right : bounds.bottom;
  const nodeCoordinates = boxes.flatMap((box) => axis === 'x'
    ? [box.x - NODE_CLEARANCE, box.x + box.width + NODE_CLEARANCE]
    : [box.y - NODE_CLEARANCE, box.y + box.height + NODE_CLEARANCE]);
  const routeCoordinates = routed.flatMap((route) => route.points.slice(1).flatMap((point, index) => {
    const previous = route.points[index];
    const parallel = axis === 'x' ? previous.x === point.x : previous.y === point.y;
    if (!parallel) return [];
    const coordinate = axis === 'x' ? point.x : point.y;
    return [coordinate - PORT_SLOT_GAP, coordinate + PORT_SLOT_GAP];
  }));
  return [...new Set([...nodeCoordinates, ...routeCoordinates])]
    .filter((coordinate) => coordinate >= minimum && coordinate <= maximum)
    .sort((first, second) => Math.abs(first - center) - Math.abs(second - center) || first - second)
    .slice(0, 2);
}

function overlapsRange(firstStart: number, firstEnd: number, secondStart: number, secondEnd: number, inclusive = false): boolean {
  const start = Math.max(Math.min(firstStart, firstEnd), Math.min(secondStart, secondEnd));
  const end = Math.min(Math.max(firstStart, firstEnd), Math.max(secondStart, secondEnd));
  return inclusive ? start <= end : start < end;
}

function segmentIntersectsBox(start: LayoutPoint, end: LayoutPoint, box: NodeBox): boolean {
  const left = box.x - NODE_CLEARANCE;
  const right = box.x + box.width + NODE_CLEARANCE;
  const top = box.y - NODE_CLEARANCE;
  const bottom = box.y + box.height + NODE_CLEARANCE;
  if (start.x === end.x) return start.x > left && start.x < right && overlapsRange(start.y, end.y, top, bottom);
  if (start.y === end.y) return start.y > top && start.y < bottom && overlapsRange(start.x, end.x, left, right);
  return true;
}

function crossingPoint(firstStart: LayoutPoint, firstEnd: LayoutPoint, secondStart: LayoutPoint, secondEnd: LayoutPoint): LayoutPoint | null {
  const firstVertical = firstStart.x === firstEnd.x;
  const secondVertical = secondStart.x === secondEnd.x;
  if (firstVertical === secondVertical) return null;
  const verticalStart = firstVertical ? firstStart : secondStart;
  const verticalEnd = firstVertical ? firstEnd : secondEnd;
  const horizontalStart = firstVertical ? secondStart : firstStart;
  const horizontalEnd = firstVertical ? secondEnd : firstEnd;
  if (!overlapsRange(horizontalStart.x, horizontalEnd.x, verticalStart.x, verticalStart.x, true) ||
      !overlapsRange(verticalStart.y, verticalEnd.y, horizontalStart.y, horizontalStart.y, true)) return null;
  const point = { x: verticalStart.x, y: horizontalStart.y };
  const endpoints = [firstStart, firstEnd, secondStart, secondEnd];
  return endpoints.some((endpoint) => pointEqual(endpoint, point)) ? null : point;
}

function crossingCount(points: readonly LayoutPoint[], routes: readonly RenderedRoute[]): number {
  let count = 0;
  for (const route of routes) {
    for (let first = 1; first < points.length; first += 1) {
      for (let second = 1; second < route.points.length; second += 1) {
        if (crossingPoint(points[first - 1], points[first], route.points[second - 1], route.points[second])) count += 1;
      }
    }
  }
  return count;
}

function congestion(points: readonly LayoutPoint[], routes: readonly RenderedRoute[]): number {
  let penalty = 0;
  for (const route of routes) {
    for (let first = 1; first < points.length; first += 1) {
      for (let second = 1; second < route.points.length; second += 1) {
        const firstStart = points[first - 1];
        const firstEnd = points[first];
        const secondStart = route.points[second - 1];
        const secondEnd = route.points[second];
        if (firstStart.x === firstEnd.x && secondStart.x === secondEnd.x && Math.abs(firstStart.x - secondStart.x) < PORT_SLOT_GAP) {
          const start = Math.max(Math.min(firstStart.y, firstEnd.y), Math.min(secondStart.y, secondEnd.y));
          const end = Math.min(Math.max(firstStart.y, firstEnd.y), Math.max(secondStart.y, secondEnd.y));
          penalty += Math.max(0, end - start) * (PORT_SLOT_GAP - Math.abs(firstStart.x - secondStart.x));
        }
        if (firstStart.y === firstEnd.y && secondStart.y === secondEnd.y && Math.abs(firstStart.y - secondStart.y) < PORT_SLOT_GAP) {
          const start = Math.max(Math.min(firstStart.x, firstEnd.x), Math.min(secondStart.x, secondEnd.x));
          const end = Math.min(Math.max(firstStart.x, firstEnd.x), Math.max(secondStart.x, secondEnd.x));
          penalty += Math.max(0, end - start) * (PORT_SLOT_GAP - Math.abs(firstStart.y - secondStart.y));
        }
      }
    }
  }
  return penalty;
}

function backtracking(points: readonly LayoutPoint[], targetBelow: boolean): number {
  let distance = 0;
  const target = points.at(-1)!;
  if (points.length > 1) {
    const before = Math.abs(points[0].x - target.x) + Math.abs(points[0].y - target.y);
    const after = Math.abs(points[1].x - target.x) + Math.abs(points[1].y - target.y);
    distance += Math.max(0, after - before);
  }
  if (targetBelow) {
    for (let index = 1; index < points.length; index += 1) {
      if (points[index].y < points[index - 1].y) distance += points[index - 1].y - points[index].y;
    }
  }
  return distance;
}

function wrongWayExcursion(points: readonly LayoutPoint[], source: NodeBox, target: NodeBox): number {
  const sourceCenterY = source.y + source.height / 2;
  const targetCenterY = target.y + target.height / 2;
  if (targetCenterY > sourceCenterY) return Math.max(0, source.y - Math.min(...points.map((point) => point.y)));
  if (targetCenterY < sourceCenterY) return Math.max(0, Math.max(...points.map((point) => point.y)) - (source.y + source.height));
  return 0;
}

function flowPortMismatch(sourceSide: PortSide, targetSide: PortSide, source: NodeBox, target: NodeBox): number {
  const forwardGap = target.y - (source.y + source.height);
  const returnGap = source.y - (target.y + target.height);
  if (forwardGap >= PORT_LEAD) {
    return (sourceSide === 'south' ? 0 : 1) + (targetSide === 'north' ? 0 : 1);
  }
  if (returnGap >= PORT_LEAD) {
    return (sourceSide === 'east' || sourceSide === 'west' ? 0 : 1) +
      (targetSide === 'east' || targetSide === 'west' ? 0 : 1);
  }
  const targetIsRight = target.x + target.width / 2 >= source.x + source.width / 2;
  return (sourceSide === (targetIsRight ? 'east' : 'west') ? 0 : 1) +
    (targetSide === (targetIsRight ? 'west' : 'east') ? 0 : 1);
}

function shortSegmentCount(points: readonly LayoutPoint[]): number {
  let count = 0;
  for (let index = 2; index < points.length - 1; index += 1) {
    const length = Math.abs(points[index].x - points[index - 1].x) + Math.abs(points[index].y - points[index - 1].y);
    if (length > 0 && length < 16) count += 1;
  }
  return count;
}

function dot(firstStart: LayoutPoint, firstEnd: LayoutPoint, secondStart: LayoutPoint, secondEnd: LayoutPoint): number {
  return (firstEnd.x - firstStart.x) * (secondEnd.x - secondStart.x) +
    (firstEnd.y - firstStart.y) * (secondEnd.y - secondStart.y);
}

function terminalReversalCount(points: readonly LayoutPoint[]): number {
  if (points.length < 3) return 0;
  const sourceReversal = dot(points[0], points[1], points[1], points[2]) < 0 ? 1 : 0;
  const last = points.length - 1;
  const targetReversal = dot(points[last - 2], points[last - 1], points[last - 1], points[last]) < 0 ? 1 : 0;
  return sourceReversal + targetReversal;
}

function compareCandidates(first: CandidateRoute, second: CandidateRoute): number {
  return first.intrusions - second.intrusions ||
    first.terminalReversals - second.terminalReversals ||
    first.shortSegments - second.shortSegments ||
    first.wrongWayExcursion - second.wrongWayExcursion ||
    first.length - second.length ||
    first.flowPortMismatch - second.flowPortMismatch ||
    first.backtracking - second.backtracking ||
    first.bends - second.bends ||
    first.corridorMismatch - second.corridorMismatch ||
    first.crossings - second.crossings ||
    first.congestion - second.congestion ||
    first.sourcePort.side.localeCompare(second.sourcePort.side) ||
    first.targetPort.side.localeCompare(second.targetPort.side);
}

function compareCandidateGeometry(first: CandidateRoute, second: CandidateRoute): number {
  return first.intrusions - second.intrusions ||
    first.terminalReversals - second.terminalReversals ||
    first.shortSegments - second.shortSegments ||
    first.wrongWayExcursion - second.wrongWayExcursion ||
    first.length - second.length ||
    first.flowPortMismatch - second.flowPortMismatch ||
    first.backtracking - second.backtracking ||
    first.bends - second.bends ||
    first.corridorMismatch - second.corridorMismatch ||
    first.sourcePort.side.localeCompare(second.sourcePort.side) ||
    first.targetPort.side.localeCompare(second.targetPort.side);
}

function labelBox(position: LayoutPoint, label: string): LabelBox {
  const width = Math.min(148, Math.max(30, 14 + label.length * 6));
  const height = 22;
  return {
    left: position.x - width / 2,
    right: position.x + width / 2,
    top: position.y - height / 2,
    bottom: position.y + height / 2
  };
}

function boxesIntersect(first: LabelBox, second: LabelBox, clearance = 0): boolean {
  return first.left - clearance < second.right && first.right + clearance > second.left &&
    first.top - clearance < second.bottom && first.bottom + clearance > second.top;
}

function segmentIntersectsLabelBox(start: LayoutPoint, end: LayoutPoint, box: LabelBox, clearance = 0): boolean {
  const left = box.left - clearance;
  const right = box.right + clearance;
  const top = box.top - clearance;
  const bottom = box.bottom + clearance;
  if (start.x === end.x) return start.x > left && start.x < right && overlapsRange(start.y, end.y, top, bottom);
  if (start.y === end.y) return start.y > top && start.y < bottom && overlapsRange(start.x, end.x, left, right);
  return true;
}

function safeLabelPosition(
  points: readonly LayoutPoint[],
  label: string,
  preferredFraction: number,
  maximumFraction: number,
  flowCenterX: number,
  boxes: readonly NodeBox[],
  placedLabels: readonly RenderedRoute[],
  allRoutes: readonly RenderedRoute[],
  routeId: string
): { position: LayoutPoint; anchor: LayoutPoint } {
  const fractions = [preferredFraction, ...Array.from({ length: 23 }, (_, index) => 0.08 + index * 0.04)]
    .filter((fraction) => fraction <= maximumFraction)
    .filter((fraction, index, values) => values.indexOf(fraction) === index);
  const occupiedLabels = placedLabels
    .filter((route) => route.displayLabel)
    .map((route) => labelBox(route.labelPosition, route.displayLabel!));
  let best: { position: LayoutPoint; anchor: LayoutPoint; collisions: number } | null = null;
  for (const fraction of fractions) {
    const base = routeLabelPosition(points, fraction, flowCenterX);
    for (const offset of LABEL_OFFSETS) {
      const position = { x: base.x + offset.x, y: base.y + offset.y };
      const candidate = labelBox(position, label);
      const nodeCollisions = boxes.filter((box) => boxesIntersect(candidate, {
        left: box.x,
        right: box.x + box.width,
        top: box.y,
        bottom: box.y + box.height
      }, 5)).length;
      const labelCollisions = occupiedLabels.filter((box) => boxesIntersect(candidate, box, 6)).length;
      const routeCollisions = allRoutes.filter((route) => route.id !== routeId).reduce((count, route) =>
        count + route.points.slice(1).filter((point, index) => segmentIntersectsLabelBox(route.points[index], point, candidate, 4)).length, 0);
      const collisions = nodeCollisions * 100 + labelCollisions * 10 + routeCollisions;
      if (collisions === 0) return { position, anchor: base };
      if (!best || collisions < best.collisions) best = { position, anchor: base, collisions };
    }
  }
  return { position: best!.position, anchor: best!.anchor };
}

function candidateRoutes(
  edge: GraphEdge,
  source: NodeBox,
  target: NodeBox,
  sourceIndex: number,
  sourceCount: number,
  targetIndex: number,
  targetCount: number,
  targetEndpoint: LayoutPoint | null,
  boxes: readonly NodeBox[],
  routed: readonly RenderedRoute[],
  bounds: { left: number; right: number; top: number; bottom: number },
  lane: number,
  includeOuterCorridors: boolean,
  requireOuterCorridor: boolean,
  useAllSides = false
): CandidateRoute[] {
  const candidates: CandidateRoute[] = [];
  const targetBelow = target.y >= source.y;
  const sourceSides: readonly PortSide[] = useAllSides
    ? ['south', 'east', 'west', 'north']
    : targetBelow ? ['south', 'east', 'west'] : ['north', 'east', 'west'];
  const targetSides: readonly PortSide[] = targetEndpoint
    ? ['north']
    : useAllSides ? ['north', 'east', 'west', 'south'] : targetBelow ? ['north', 'east', 'west'] : ['south', 'east', 'west'];
  const useInternalLanes = sourceCount > 1 || targetCount > 1;
  const laneXs = useInternalLanes ? internalLaneCoordinates(boxes, routed, bounds, 'x', (source.x + target.x + source.width) / 2) : [];
  const laneYs = useInternalLanes ? internalLaneCoordinates(boxes, routed, bounds, 'y', (source.y + target.y + source.height) / 2) : [];
  for (const sourceSide of sourceSides) {
    if (sourceCount > portCapacity(sourceSide, source)) continue;
    for (const targetSide of targetSides) {
      if (targetCount > portCapacity(targetSide, target)) continue;
      const sourcePort = createPort(source, edge, 'source', sourceSide, sourceIndex, sourceCount);
      const targetPort = createPort(target, edge, 'target', targetSide, targetIndex, targetCount);
      const sourceLead = lead(sourcePort.point, sourceSide);
      const targetLead = targetEndpoint ?? lead(targetPort.point, targetSide);
      for (const core of orthogonalCore(sourceLead, targetLead, bounds, lane, laneXs, laneYs, includeOuterCorridors || useAllSides)) {
        const points = simplify([sourcePort.point, ...core, ...(targetEndpoint ? [] : [targetPort.point])]);
        const usesOuterCorridor = points.some((point) =>
          point.x < bounds.left || point.x > bounds.right || point.y < bounds.top || point.y > bounds.bottom
        );
        const intrusions = points.slice(1).reduce((count, point, index) => count + boxes.filter((box) =>
          box.id !== source.id && box.id !== target.id && segmentIntersectsBox(points[index], point, box)
        ).length, 0);
        const sharedTargetPort: LayoutPort = {
          ...targetPort,
          id: `${target.id}::target::shared::north`,
          side: 'north',
          slot: 0,
          point: { x: target.x + target.width / 2, y: target.y }
        };
        candidates.push({
          sourcePort,
          targetPort: targetEndpoint ? sharedTargetPort : targetPort,
          points,
          intrusions,
          terminalReversals: terminalReversalCount(points),
          corridorMismatch: requireOuterCorridor ? (usesOuterCorridor ? 0 : 1) : (usesOuterCorridor ? 1 : 0),
          shortSegments: shortSegmentCount(points),
          wrongWayExcursion: wrongWayExcursion(points, source, target),
          flowPortMismatch: flowPortMismatch(sourceSide, targetSide, source, target),
          crossings: 0,
          congestion: 0,
          length: manhattanLength(points),
          bends: bendCount(points),
          backtracking: backtracking(points, target.y >= source.y),
          usesOuterCorridor
        });
      }
    }
  }
  return candidates.sort(compareCandidateGeometry);
}

function obstacleAvoidingCandidates(
  edge: GraphEdge,
  source: NodeBox,
  target: NodeBox,
  sourceIndex: number,
  sourceCount: number,
  targetIndex: number,
  targetCount: number,
  targetEndpoint: LayoutPoint | null,
  boxes: readonly NodeBox[],
  routed: readonly RenderedRoute[],
  bounds: { left: number; right: number; top: number; bottom: number },
  lane: number,
  requireOuterCorridor: boolean
): CandidateRoute[] {
  const blockers = boxes.filter((box) => box.id !== source.id && box.id !== target.id);
  const candidates: CandidateRoute[] = [];
  for (const sourceSide of ['south', 'east', 'west', 'north'] as const) {
    if (sourceCount > portCapacity(sourceSide, source)) continue;
    for (const targetSide of (targetEndpoint ? ['north'] : ['north', 'east', 'west', 'south']) as readonly PortSide[]) {
      if (targetCount > portCapacity(targetSide, target)) continue;
      const sourcePort = createPort(source, edge, 'source', sourceSide, sourceIndex, sourceCount);
      const targetPort = createPort(target, edge, 'target', targetSide, targetIndex, targetCount);
      const sourceLead = lead(sourcePort.point, sourceSide);
      const targetLead = targetEndpoint ?? lead(targetPort.point, targetSide);
      const core = obstacleAvoidingCore(sourceLead, targetLead, blockers, bounds, lane);
      if (!core) continue;
      const points = simplify([sourcePort.point, ...core, ...(targetEndpoint ? [] : [targetPort.point])]);
      const usesOuterCorridor = points.some((point) =>
        point.x < bounds.left || point.x > bounds.right || point.y < bounds.top || point.y > bounds.bottom
      );
      const intrusions = points.slice(1).reduce((count, point, index) => count + blockers.filter((box) =>
        segmentIntersectsBox(points[index], point, box)
      ).length, 0);
      const sharedTargetPort: LayoutPort = {
        ...targetPort,
        id: `${target.id}::target::shared::north`,
        side: 'north',
        slot: 0,
        point: { x: target.x + target.width / 2, y: target.y }
      };
      candidates.push({
        sourcePort,
        targetPort: targetEndpoint ? sharedTargetPort : targetPort,
        points,
        intrusions,
        terminalReversals: terminalReversalCount(points),
        corridorMismatch: requireOuterCorridor ? (usesOuterCorridor ? 0 : 1) : (usesOuterCorridor ? 1 : 0),
        shortSegments: shortSegmentCount(points),
        wrongWayExcursion: wrongWayExcursion(points, source, target),
        flowPortMismatch: flowPortMismatch(sourceSide, targetSide, source, target),
        crossings: crossingCount(points, routed),
        congestion: congestion(points, routed),
        length: manhattanLength(points),
        bends: bendCount(points),
        backtracking: backtracking(points, target.y >= source.y),
        usesOuterCorridor
      });
    }
  }
  return candidates.sort(compareCandidates);
}

function rankedCandidates(candidates: readonly CandidateRoute[], routed: readonly RenderedRoute[]): CandidateRoute[] {
  const shortlist = [0, 1].flatMap((corridorMismatch) => candidates
    .filter((candidate) => candidate.corridorMismatch === corridorMismatch)
    .sort(compareCandidateGeometry)
    .slice(0, 6));
  for (const candidate of shortlist) {
    candidate.crossings = crossingCount(candidate.points, routed);
    candidate.congestion = congestion(candidate.points, routed);
  }
  return shortlist.sort(compareCandidates);
}

function routeOrder(first: GraphEdge, second: GraphEdge, topology: TopologyAnalysis): number {
  const firstLong = topology.longEdgeIds.has(first.id) ? 1 : 0;
  const secondLong = topology.longEdgeIds.has(second.id) ? 1 : 0;
  const firstSpan = Math.abs(topology.rankByNodeId.get(first.to)! - topology.rankByNodeId.get(first.from)!);
  const secondSpan = Math.abs(topology.rankByNodeId.get(second.to)! - topology.rankByNodeId.get(second.from)!);
  return firstLong - secondLong || firstSpan - secondSpan ||
    topology.rankByNodeId.get(first.from)! - topology.rankByNodeId.get(second.from)! || first.id.localeCompare(second.id);
}

function detectCrossings(routes: readonly RenderedRoute[], topology: TopologyAnalysis): RouteCrossing[] {
  const crossings: RouteCrossing[] = [];
  for (let first = 0; first < routes.length; first += 1) {
    for (let second = first + 1; second < routes.length; second += 1) {
      for (let firstSegment = 1; firstSegment < routes[first].points.length; firstSegment += 1) {
        for (let secondSegment = 1; secondSegment < routes[second].points.length; secondSegment += 1) {
          const point = crossingPoint(routes[first].points[firstSegment - 1], routes[first].points[firstSegment], routes[second].points[secondSegment - 1], routes[second].points[secondSegment]);
          if (!point) continue;
          const firstSpan = Math.abs(topology.rankByNodeId.get(routes[first].targetPort.nodeId)! - topology.rankByNodeId.get(routes[first].sourcePort.nodeId)!);
          const secondSpan = Math.abs(topology.rankByNodeId.get(routes[second].targetPort.nodeId)! - topology.rankByNodeId.get(routes[second].sourcePort.nodeId)!);
          const over = firstSpan < secondSpan || firstSpan === secondSpan && routes[first].id.localeCompare(routes[second].id) < 0 ? routes[first] : routes[second];
          const under = over === routes[first] ? routes[second] : routes[first];
          const overSegment = over === routes[first] ? [routes[first].points[firstSegment - 1], routes[first].points[firstSegment]] : [routes[second].points[secondSegment - 1], routes[second].points[secondSegment]];
          crossings.push({
            id: `crossing-${crossings.length}`,
            overEdgeId: over.id,
            underEdgeId: under.id,
            point,
            overAxis: overSegment[0].y === overSegment[1].y ? 'horizontal' : 'vertical',
            radius: 5
          });
        }
      }
    }
  }
  return crossings;
}

export function planRoutes(graph: GraphModel, positions: readonly LayoutNodePosition[], topology: TopologyAnalysis, width: number, height: number, nodeWidth: number, nodeHeight: number): RoutePlan {
  const boxes = positions.map((position): NodeBox => ({ ...position, width: nodeWidth, height: nodeHeight }));
  const boxById = new Map(boxes.map((box) => [box.id, box]));
  const junctions: LayoutJunction[] = topology.convergenceGroups.map((group) => {
    const target = boxById.get(group.targetNodeId)!;
    return {
      id: `junction-${group.targetNodeId}`,
      targetNodeId: group.targetNodeId,
      incomingEdgeIds: group.incomingEdgeIds,
      point: { x: target.x + target.width / 2, y: target.y - 52 }
    };
  });
  const junctionByEdgeId = new Map(junctions.flatMap((junction) => junction.incomingEdgeIds.map((edgeId) => [edgeId, junction] as const)));
  const edgeById = new Map(graph.edges.map((edge) => [edge.id, edge]));
  const junctionSlotByEdgeId = new Map<string, LayoutPoint>();
  for (const junction of junctions) {
    const orderedEdgeIds = [...junction.incomingEdgeIds].sort((firstId, secondId) => {
      const firstSource = boxById.get(edgeById.get(firstId)!.from)!;
      const secondSource = boxById.get(edgeById.get(secondId)!.from)!;
      return firstSource.x - secondSource.x || firstId.localeCompare(secondId);
    });
    orderedEdgeIds.forEach((edgeId, index) => junctionSlotByEdgeId.set(edgeId, {
      x: junction.point.x + (index - (orderedEdgeIds.length - 1) / 2) * 20,
      y: junction.point.y
    }));
  }
  const sharedSegments: SharedRouteSegment[] = junctions.map((junction) => {
    const target = boxById.get(junction.targetNodeId)!;
    const slots = junction.incomingEdgeIds.map((edgeId) => junctionSlotByEdgeId.get(edgeId)!);
    return {
      id: `shared-${junction.targetNodeId}`,
      junctionId: junction.id,
      incomingEdgeIds: junction.incomingEdgeIds,
      points: [junction.point, { x: target.x + target.width / 2, y: target.y }],
      lanePoints: [
        { x: Math.min(...slots.map((slot) => slot.x)), y: junction.point.y },
        { x: Math.max(...slots.map((slot) => slot.x)), y: junction.point.y }
      ]
    };
  });
  const sharedByJunctionId = new Map(sharedSegments.map((segment) => [segment.junctionId, segment]));
  const routes: RenderedRoute[] = [];
  const edgeOrder = [...graph.edges].sort((first, second) => routeOrder(first, second, topology));
  const bounds = { left: 0, right: width, top: 0, bottom: height };
  const cyclicComponentByNodeId = new Map(topology.stronglyConnectedComponents
    .filter((component) => component.cyclic)
    .flatMap((component) => component.nodeIds.map((nodeId) => [nodeId, component] as const)));

  function routeEdge(edge: GraphEdge, routed: readonly RenderedRoute[], orderIndex: number): RenderedRoute {
    const source = boxById.get(edge.from)!;
    const target = boxById.get(edge.to)!;
    const sourceEdges = topology.outgoingByNodeId.get(edge.from)!;
    const targetEdges = topology.incomingByNodeId.get(edge.to)!;
    const sourceIndex = sourceEdges.findIndex((candidate) => candidate.id === edge.id);
    const targetIndex = targetEdges.findIndex((candidate) => candidate.id === edge.id);
    const junction = junctionByEdgeId.get(edge.id) ?? null;
    const sourceCycle = cyclicComponentByNodeId.get(edge.from);
    const targetCycle = cyclicComponentByNodeId.get(edge.to);
    const cycleLoopback = topology.longEdgeIds.has(edge.id) && Boolean(sourceCycle && targetCycle && sourceCycle.id === targetCycle.id);
    const routeBounds = cycleLoopback ? (() => {
      const members = sourceCycle!.nodeIds.map((nodeId) => boxById.get(nodeId)!);
      return {
        left: Math.min(...members.map((member) => member.x)),
        right: Math.max(...members.map((member) => member.x + member.width)),
        top: Math.min(...members.map((member) => member.y)),
        bottom: Math.max(...members.map((member) => member.y + member.height))
      };
    })() : bounds;
    const includeOuterCorridors = topology.longEdgeIds.has(edge.id) || cycleLoopback;
    let candidates = candidateRoutes(
      edge, source, target, sourceIndex, sourceEdges.length, targetIndex, targetEdges.length,
      junction ? junctionSlotByEdgeId.get(edge.id)! : null, boxes, routed, routeBounds, orderIndex % 8, includeOuterCorridors, cycleLoopback
    );
    const valid = (candidate: CandidateRoute) => candidate.intrusions === 0 && candidate.terminalReversals === 0 && candidate.shortSegments === 0;
    let validCandidates = candidates.filter(valid);
    if (validCandidates.length === 0) {
      candidates = candidateRoutes(
        edge, source, target, sourceIndex, sourceEdges.length, targetIndex, targetEdges.length,
        junction ? junctionSlotByEdgeId.get(edge.id)! : null, boxes, routed, routeBounds, orderIndex % 8, includeOuterCorridors, cycleLoopback, true
      );
      validCandidates = candidates.filter(valid);
    }
    if (validCandidates.length === 0 || (!includeOuterCorridors && validCandidates.every((candidate) => candidate.usesOuterCorridor))) {
      const obstacleAvoiding = obstacleAvoidingCandidates(
        edge, source, target, sourceIndex, sourceEdges.length, targetIndex, targetEdges.length,
        junction ? junctionSlotByEdgeId.get(edge.id)! : null, boxes, routed, routeBounds, orderIndex % 8, cycleLoopback
      );
      candidates = validCandidates.length === 0 ? obstacleAvoiding : [...candidates, ...obstacleAvoiding].sort(compareCandidates);
      validCandidates = candidates.filter(valid);
    }
    if (cycleLoopback) {
      const perimeterCandidates = validCandidates.filter((candidate) => candidate.usesOuterCorridor);
      if (perimeterCandidates.length > 0) validCandidates = perimeterCandidates;
    }
    if (topology.primaryEdgeIds.has(edge.id) && target.y >= source.y + source.height) {
      const directFlowCandidates = validCandidates.filter((candidate) => candidate.sourcePort.side === 'south' && candidate.targetPort.side === 'north');
      if (directFlowCandidates.length > 0) validCandidates = directFlowCandidates;
    }
    const minimumWrongWayExcursion = Math.min(...validCandidates.map((candidate) => candidate.wrongWayExcursion));
    validCandidates = validCandidates.filter((candidate) => candidate.wrongWayExcursion === minimumWrongWayExcursion);
    const shortestValidLength = Math.min(...validCandidates.map((candidate) => candidate.length));
    const ranked = rankedCandidates(validCandidates, routed);
    const selected = ranked[0];
    if (!selected) {
      const best = candidates[0];
      throw new Error(`No collision-free route exists for edge ${edge.id}. Best candidate has ${best.intrusions} intrusions, ${best.terminalReversals} terminal reversals, and ${best.shortSegments} short segments.`);
    }
    const fallbackOuterCorridor = !topology.longEdgeIds.has(edge.id) && selected.usesOuterCorridor;
    const shortestCandidateLength = shortestValidLength;
    const displayLabel = displayedEdgeLabel(edge.outcome, sourceEdges.length, sourceIndex);
    const sharedSegment = junction ? sharedByJunctionId.get(junction.id)! : null;
    const preferredLabelFraction = sourceEdges.length > 1 ? Math.min(0.42, 70 / Math.max(1, selected.length)) : 0.5;
    const defaultLabelPosition = routeLabelPosition(selected.points, preferredLabelFraction, source.x + source.width / 2);
    return {
      id: edge.id,
      sourceNodeId: edge.from,
      targetNodeId: edge.to,
      points: selected.points,
      labelPosition: defaultLabelPosition,
      labelAnchor: defaultLabelPosition,
      displayLabel,
      sourcePort: selected.sourcePort,
      targetPort: selected.targetPort,
      sharedSegmentIds: sharedSegment ? [sharedSegment.id] : [],
      crossingIds: [],
      length: selected.length,
      shortestCandidateLength,
      bends: selected.bends,
      long: topology.longEdgeIds.has(edge.id) || fallbackOuterCorridor,
      secondary: !topology.primaryEdgeIds.has(edge.id) || cycleLoopback || selected.usesOuterCorridor || target.y + target.height <= source.y,
      branch: new Set(sourceEdges.map((candidate) => candidate.to)).size > 1,
      corridor: cycleLoopback && selected.usesOuterCorridor ? 'cycle' : selected.usesOuterCorridor ? 'outer' : 'normal'
    };
  }

  edgeOrder.forEach((edge, index) => routes.push(routeEdge(edge, routes, index)));

  let avoidableCrossings = 0;
  if (detectCrossings(routes, topology).length > 0) {
    let converged = false;
    for (let pass = 0; pass < 3; pass += 1) {
      let improved = false;
      const crossingEdgeIds = new Set(detectCrossings(routes, topology).flatMap((crossing) => [crossing.overEdgeId, crossing.underEdgeId]));
      for (const edge of edgeOrder.filter((candidate) => crossingEdgeIds.has(candidate.id))) {
        const index = edgeOrder.findIndex((candidate) => candidate.id === edge.id);
        const routeIndex = routes.findIndex((route) => route.id === edge.id);
        const current = routes[routeIndex];
        const otherRoutes = routes.filter((route) => route.id !== edge.id);
        const proposal = routeEdge(edge, otherRoutes, index);
        const currentScore = [
          current.length,
          backtracking(current.points, boxById.get(current.targetPort.nodeId)!.y >= boxById.get(current.sourcePort.nodeId)!.y),
          current.bends,
          crossingCount(current.points, otherRoutes),
          congestion(current.points, otherRoutes)
        ];
        const proposalScore = [
          proposal.length,
          backtracking(proposal.points, boxById.get(proposal.targetPort.nodeId)!.y >= boxById.get(proposal.sourcePort.nodeId)!.y),
          proposal.bends,
          crossingCount(proposal.points, otherRoutes),
          congestion(proposal.points, otherRoutes)
        ];
        const improves = proposalScore.some((value, scoreIndex) =>
          value < currentScore[scoreIndex] && proposalScore.slice(0, scoreIndex).every((prior, priorIndex) => prior === currentScore[priorIndex])
        );
        if (improves) {
          routes[routeIndex] = proposal;
          improved = true;
        }
      }
      if (!improved) {
        converged = true;
        break;
      }
    }

    if (!converged) {
      const crossingEdgeIds = new Set(detectCrossings(routes, topology).flatMap((crossing) => [crossing.overEdgeId, crossing.underEdgeId]));
      avoidableCrossings = edgeOrder.filter((edge) => crossingEdgeIds.has(edge.id)).reduce((count, edge) => {
        const index = edgeOrder.findIndex((candidate) => candidate.id === edge.id);
        const current = routes.find((route) => route.id === edge.id)!;
        const otherRoutes = routes.filter((route) => route.id !== edge.id);
        const proposal = routeEdge(edge, otherRoutes, index);
        const improvement = crossingCount(current.points, otherRoutes) - crossingCount(proposal.points, otherRoutes);
        return count + Math.max(0, improvement);
      }, 0);
    }
  }

  routes.sort((first, second) => first.id.localeCompare(second.id));
  const placedLabels: RenderedRoute[] = [];
  for (const route of routes) {
    if (!route.displayLabel) continue;
    const source = boxById.get(route.sourcePort.nodeId)!;
    const sourceEdges = topology.outgoingByNodeId.get(route.sourcePort.nodeId)!;
    const preferredLabelFraction = sourceEdges.length > 1 ? Math.min(0.42, 70 / Math.max(1, route.length)) : 0.5;
    const labelLayout = safeLabelPosition(
      route.points,
      route.displayLabel,
      preferredLabelFraction,
      0.96,
      source.x + source.width / 2,
      boxes,
      placedLabels,
      routes,
      route.id
    );
    route.labelPosition = labelLayout.position;
    route.labelAnchor = labelLayout.anchor;
    placedLabels.push(route);
  }
  const crossings = detectCrossings(routes, topology);
  const crossingIdsByEdge = new Map<string, string[]>();
  for (const crossing of crossings) {
    crossingIdsByEdge.set(crossing.overEdgeId, [...(crossingIdsByEdge.get(crossing.overEdgeId) ?? []), crossing.id]);
    crossingIdsByEdge.set(crossing.underEdgeId, [...(crossingIdsByEdge.get(crossing.underEdgeId) ?? []), crossing.id]);
  }
  for (const route of routes) route.crossingIds = crossingIdsByEdge.get(route.id) ?? [];
  return { routes, junctions, sharedSegments, crossings, avoidableCrossings };
}
