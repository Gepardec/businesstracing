import type { GraphModel } from '$contracts/graph-contract';
import type { PositionedNode } from './layout-definition';

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
export const NEIGHBORHOOD_MINIMUM_ZOOM = 0.62;

export function safeCanvasRect(width: number, height: number, overlays: readonly CanvasRect[], gutter = 16): CanvasRect {
  let left = gutter;
  let right = width - gutter;
  let top = gutter;
  let bottom = height - gutter;
  for (const overlay of overlays) {
    const sidePanel = overlay.height >= height * 0.4 && overlay.width <= width * 0.5;
    if (!sidePanel && overlay.y <= gutter * 2) top = Math.max(top, overlay.y + overlay.height + gutter);
    if (!sidePanel && overlay.y + overlay.height >= height - gutter * 2) bottom = Math.min(bottom, overlay.y - gutter);
    if (sidePanel && overlay.x <= gutter * 2) left = Math.max(left, overlay.x + overlay.width + gutter);
    if (sidePanel && overlay.x + overlay.width >= width - gutter * 2) right = Math.min(right, overlay.x - gutter);
  }
  if (bottom - top < 160) {
    top = gutter;
    bottom = height - gutter;
  }
  return {
    x: left,
    y: top,
    width: Math.max(1, right - left),
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
  const contextMinimumZoom = safeRect.height < 420 ? 0.5 : NEIGHBORHOOD_MINIMUM_ZOOM;
  if (neighborhoodFit >= contextMinimumZoom) {
    return viewportForBounds(neighborhood, safeRect, contextMinimumZoom, maximumZoom);
  }
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

export function openingNeighborhood(graph: GraphModel, entryNodeId: string, maximumLinearNodes = 4): GraphNeighborhood {
  const nodeIds = new Set([entryNodeId]);
  const edgeIds = new Set<string>();
  const visited = new Set<string>();
  let currentNodeId = entryNodeId;

  while (!visited.has(currentNodeId)) {
    visited.add(currentNodeId);
    const outgoing = graph.edges.filter((edge) => edge.from === currentNodeId);
    const successorIds = [...new Set(outgoing.map((edge) => edge.to))];
    if (successorIds.length === 0) break;

    if (successorIds.length > 1) {
      for (const edge of outgoing) {
        nodeIds.add(edge.to);
        edgeIds.add(edge.id);
      }
      break;
    }

    const successorId = successorIds[0];
    for (const edge of outgoing.filter((item) => item.to === successorId)) edgeIds.add(edge.id);
    nodeIds.add(successorId);
    if (nodeIds.size >= maximumLinearNodes) break;
    currentNodeId = successorId;
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
  focusNodeId: string,
  contextPixels = 64,
  minimumZoom = READING_MINIMUM_ZOOM
): CanvasRect | null {
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  if (!nodeById.has(focusNodeId)) return null;
  const { nodeIds } = directNeighborhood(graph, focusNodeId);
  let bounds: CanvasRect | null = null;
  for (const nodeId of nodeIds) {
    const node = nodeById.get(nodeId);
    if (!node) continue;
    bounds = extendBounds(bounds, { x: node.x, y: node.y });
    bounds = extendBounds(bounds, { x: node.x + node.width, y: node.y + node.height });
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
