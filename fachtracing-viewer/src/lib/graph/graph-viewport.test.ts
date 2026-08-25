import { describe, expect, it } from 'vitest';
import { directNeighborhood, focusedNodeBounds, neighborhoodBounds, readingViewport, READING_MINIMUM_ZOOM, safeCanvasRect, viewportForBounds } from './graph-viewport';
import { graphFixture } from './graph-fixtures';

describe('graph viewport', () => {
  it('reserves top and bottom overlay bands', () => {
    expect(safeCanvasRect(1_000, 800, [
      { x: 14, y: 14, width: 220, height: 36 },
      { x: 14, y: 650, width: 32, height: 136 },
      { x: 820, y: 660, width: 166, height: 126 }
    ])).toEqual({ x: 16, y: 66, width: 968, height: 568 });
  });

  it('keeps Reading view at its readable zoom floor', () => {
    const viewport = viewportForBounds({ x: 100, y: 200, width: 2_000, height: 2_000 }, { x: 16, y: 66, width: 968, height: 568 }, READING_MINIMUM_ZOOM, 1.2);
    expect(viewport.zoom).toBeGreaterThanOrEqual(READING_MINIMUM_ZOOM);
    expect(viewport.x + 100 * viewport.zoom).toBeLessThan(16);
  });

  it('frames a selected node with its direct topology and routes', () => {
    const graph = graphFixture('neighborhood', ['entry', 'selected', 'successor', 'other', 'outcome'], [
      { id: 'incoming', from: 'entry', to: 'selected' },
      { id: 'outgoing', from: 'selected', to: 'successor' },
      { id: 'unrelated', from: 'other', to: 'outcome' }
    ]);
    const nodes = [
      { id: 'entry', x: 0, y: 0 }, { id: 'selected', x: 300, y: 200 }, { id: 'successor', x: 600, y: 400 },
      { id: 'other', x: 2_000, y: 0 }, { id: 'outcome', x: 2_000, y: 400 }
    ].map((node) => ({ ...node, width: 232, height: 92, ports: [], occurrence: null, incomingCount: 0, outgoingCount: 0 }));
    const route = (id: string, points: Array<{ x: number; y: number }>) => ({
      id, sourceNodeId: '', targetNodeId: '', points, labelPosition: points[0], labelAnchor: points[0], displayLabel: null,
      sourcePort: { id: `${id}-source`, nodeId: '', edgeId: id, role: 'source' as const, side: 'south' as const, slot: 0, point: points[0] },
      targetPort: { id: `${id}-target`, nodeId: '', edgeId: id, role: 'target' as const, side: 'north' as const, slot: 0, point: points.at(-1)! },
      sharedSegmentIds: [], crossingIds: [], length: 1, shortestCandidateLength: 1, bends: 0, long: false, corridor: 'normal' as const
    });
    const bounds = neighborhoodBounds(graph, nodes, [
      route('incoming', [{ x: 116, y: 92 }, { x: 416, y: 200 }]),
      route('outgoing', [{ x: 416, y: 292 }, { x: 716, y: 400 }]),
      route('unrelated', [{ x: 2_116, y: 92 }, { x: 2_116, y: 400 }])
    ], 'selected')!;
    expect(bounds.x).toBeLessThan(0);
    expect(bounds.x + bounds.width).toBeLessThan(1_000);
  });

  it('keeps the focused node visible when its complete neighborhood cannot fit at reading scale', () => {
    const safeRect = { x: 16, y: 66, width: 968, height: 568 };
    const focus = { x: 5_000, y: 2_000, width: 380, height: 240 };
    const viewport = readingViewport({ x: 0, y: 0, width: 12_000, height: 5_000 }, focus, safeRect);
    const visibleFocus = {
      left: viewport.x + focus.x * viewport.zoom,
      top: viewport.y + focus.y * viewport.zoom,
      right: viewport.x + (focus.x + focus.width) * viewport.zoom,
      bottom: viewport.y + (focus.y + focus.height) * viewport.zoom
    };
    expect(viewport.zoom).toBeGreaterThanOrEqual(READING_MINIMUM_ZOOM);
    expect(visibleFocus.left).toBeGreaterThanOrEqual(safeRect.x);
    expect(visibleFocus.top).toBeGreaterThanOrEqual(safeRect.y);
    expect(visibleFocus.right).toBeLessThanOrEqual(safeRect.x + safeRect.width);
    expect(visibleFocus.bottom).toBeLessThanOrEqual(safeRect.y + safeRect.height);
  });

  it('builds focused-node bounds with reading context', () => {
    const bounds = focusedNodeBounds([{ id: 'selected', x: 100, y: 200, width: 232, height: 92, ports: [], occurrence: null, incomingCount: 0, outgoingCount: 0 }], 'selected')!;
    expect(bounds.x).toBeLessThan(100);
    expect(bounds.y).toBeLessThan(200);
    expect(bounds.x + bounds.width).toBeGreaterThan(332);
    expect(bounds.y + bounds.height).toBeGreaterThan(292);
  });

  it('identifies only the direct reading neighborhood', () => {
    const graph = graphFixture('context', ['entry', 'selected', 'successor', 'other'], [
      { id: 'incoming', from: 'entry', to: 'selected' },
      { id: 'outgoing', from: 'selected', to: 'successor' },
      { id: 'unrelated', from: 'successor', to: 'other' }
    ]);
    const context = directNeighborhood(graph, 'selected');
    expect([...context.nodeIds]).toEqual(['selected', 'entry', 'successor']);
    expect([...context.edgeIds]).toEqual(['incoming', 'outgoing']);
  });
});
