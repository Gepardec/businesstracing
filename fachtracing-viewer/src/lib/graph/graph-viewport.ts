import type { GraphModel } from '$contracts/graph-contract';
import type { PositionedNode } from './layout-definition';
import type { RenderedRoute } from './route-planner';

export interface CanvasRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface GraphViewport {
  x: number;
  y: number;
  zoom: number;
}

export interface GraphNeighborhood {
  nodeIds: ReadonlySet<string>;
  edgeIds: ReadonlySet<string>;
}

export const READING_MINIMUM_ZOOM = 0.86;
export const OVERVIEW_DETAIL_ZOOM = 0.72;

export function safeCanvasRect(width: number, height: number, overlays: readonly CanvasRect[], gutter = 16): CanvasRect {
  let top = gutter;
  let bottom = height - gutter;
  for (const overlay of overlays) {
    if (overlay.y <= gutter * 2) top = Math.max(top, overlay.y + overlay.height + gutter);
    if (overlay.y + overlay.height >= height - gutter * 2) bottom = Math.min(bottom, overlay.y - gutter);
  }
  if (bottom - top < 160) {
    top = gutter;
    bottom = height - gutter;
  }
  return {
    x: gutter,
    y: top,
    width: Math.max(1, width - gutter * 2),
    height: Math.max(1, bottom - top)
  };
}

export function viewportForBounds(bounds: CanvasRect, safeRect: CanvasRect, minimumZoom: number, maximumZoom: number): GraphViewport {
  const width = Math.max(1, bounds.width);
  const height = Math.max(1, bounds.height);
  const fitZoom = Math.min(safeRect.width / width, safeRect.height / height);
  const zoom = Math.max(minimumZoom, Math.min(maximumZoom, fitZoom));
  return {
    x: safeRect.x + (safeRect.width - width * zoom) / 2 - bounds.x * zoom,
    y: safeRect.y + (safeRect.height - height * zoom) / 2 - bounds.y * zoom,
    zoom
  };
}

export function readingViewport(
  neighborhood: CanvasRect,
  focus: CanvasRect,
  safeRect: CanvasRect,
  minimumZoom = READING_MINIMUM_ZOOM,
  maximumZoom = 1.2
): GraphViewport {
  const neighborhoodFit = Math.min(safeRect.width / Math.max(1, neighborhood.width), safeRect.height / Math.max(1, neighborhood.height));
  if (neighborhoodFit >= minimumZoom) return viewportForBounds(neighborhood, safeRect, minimumZoom, maximumZoom);
  return viewportForBounds(focus, safeRect, minimumZoom, minimumZoom);
}

export function directNeighborhood(graph: GraphModel, focusNodeId: string): GraphNeighborhood {
  const nodeIds = new Set([focusNodeId]);
  const edgeIds = new Set<string>();
  for (const edge of graph.edges) {
    if (edge.from !== focusNodeId && edge.to !== focusNodeId) continue;
    nodeIds.add(edge.from);
    nodeIds.add(edge.to);
    edgeIds.add(edge.id);
  }
  return { nodeIds, edgeIds };
}

export function focusedNodeBounds(
  nodes: readonly PositionedNode[],
  focusNodeId: string,
  contextPixels = 64,
  minimumZoom = READING_MINIMUM_ZOOM
): CanvasRect | null {
  const node = nodes.find((item) => item.id === focusNodeId);
  if (!node) return null;
  const margin = contextPixels / minimumZoom;
  return {
    x: node.x - margin,
    y: node.y - margin,
    width: node.width + margin * 2,
    height: node.height + margin * 2
  };
}

function extendBounds(bounds: CanvasRect | null, point: { x: number; y: number }): CanvasRect {
  if (!bounds) return { x: point.x, y: point.y, width: 0, height: 0 };
  const right = Math.max(bounds.x + bounds.width, point.x);
  const bottom = Math.max(bounds.y + bounds.height, point.y);
  const x = Math.min(bounds.x, point.x);
  const y = Math.min(bounds.y, point.y);
  return { x, y, width: right - x, height: bottom - y };
}

export function neighborhoodBounds(
  graph: GraphModel,
  nodes: readonly PositionedNode[],
  routes: readonly RenderedRoute[],
  focusNodeId: string,
  contextPixels = 64,
  minimumZoom = READING_MINIMUM_ZOOM
): CanvasRect | null {
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  if (!nodeById.has(focusNodeId)) return null;
  const { nodeIds, edgeIds } = directNeighborhood(graph, focusNodeId);
  let bounds: CanvasRect | null = null;
  for (const nodeId of nodeIds) {
    const node = nodeById.get(nodeId);
    if (!node) continue;
    bounds = extendBounds(bounds, { x: node.x, y: node.y });
    bounds = extendBounds(bounds, { x: node.x + node.width, y: node.y + node.height });
  }
  for (const route of routes) {
    if (!edgeIds.has(route.id)) continue;
    for (const point of route.points) bounds = extendBounds(bounds, point);
  }
  if (!bounds) return null;
  const margin = contextPixels / minimumZoom;
  return {
    x: bounds.x - margin,
    y: bounds.y - margin,
    width: bounds.width + margin * 2,
    height: bounds.height + margin * 2
  };
}
