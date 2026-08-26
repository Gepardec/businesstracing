import { describe, expect, it } from 'vitest';
import { compactNeighborhood, compactOpeningNeighborhood, directNeighborhood, focusedNodeBounds, neighborhoodBounds, openingNeighborhood, readingViewport, READING_MINIMUM_ZOOM, safeCanvasRect, viewportForBounds } from './graph-viewport';
import { graphFixture } from './graph-fixtures';

describe('graph viewport', () => {
  it('reserves top and bottom overlay bands', () => {
    expect(safeCanvasRect(1_000, 800, [
      { x: 14, y: 14, width: 220, height: 36 },
      { x: 14, y: 650, width: 32, height: 136 },
      { x: 820, y: 660, width: 166, height: 126 }
    ])).toEqual({ x: 16, y: 66, width: 968, height: 568 });
  });

  it('reserves a persistent explanation panel on the right', () => {
    expect(safeCanvasRect(1_200, 800, [
      { x: 14, y: 14, width: 220, height: 36 },
      { x: 860, y: 14, width: 326, height: 772 }
    ])).toEqual({ x: 16, y: 66, width: 828, height: 718 });
  });

  it('reserves a wide explanation panel as a bottom band', () => {
    expect(safeCanvasRect(800, 800, [
      { x: 14, y: 450, width: 772, height: 336 }
    ])).toEqual({ x: 16, y: 16, width: 768, height: 418 });
  });

  it('keeps Reading view at its readable zoom floor', () => {
    const viewport = viewportForBounds({ x: 100, y: 200, width: 2_000, height: 2_000 }, { x: 16, y: 66, width: 968, height: 568 }, READING_MINIMUM_ZOOM, 1.2);
    expect(viewport.zoom).toBeGreaterThanOrEqual(READING_MINIMUM_ZOOM);
    expect(viewport.x + 100 * viewport.zoom).toBeLessThan(16);
  });

  it('frames a selected node with its direct topology without remote route corridors', () => {
    const graph = graphFixture('neighborhood', ['entry', 'selected', 'successor', 'other', 'outcome'], [
      { id: 'incoming', from: 'entry', to: 'selected' },
      { id: 'outgoing', from: 'selected', to: 'successor' },
      { id: 'unrelated', from: 'other', to: 'outcome' }
    ]);
    const nodes = [
      { id: 'entry', x: 0, y: 0 }, { id: 'selected', x: 300, y: 200 }, { id: 'successor', x: 600, y: 400 },
      { id: 'other', x: 2_000, y: 0 }, { id: 'outcome', x: 2_000, y: 400 }
    ].map((node) => ({ ...node, width: 232, height: 92, ports: [], occurrence: null, incomingCount: 0, outgoingCount: 0 }));
    const bounds = neighborhoodBounds(graph, nodes, 'selected')!;
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

  it('fits direct context at a bounded context zoom before it falls back to one node', () => {
    const safeRect = { x: 16, y: 66, width: 968, height: 568 };
    const neighborhood = { x: 1_000, y: 800, width: 1_200, height: 700 };
    const focus = { x: 1_450, y: 1_050, width: 232, height: 92 };
    const viewport = readingViewport(neighborhood, focus, safeRect);
    expect(viewport.zoom).toBeCloseTo(568 / 700);
    expect(viewport.x + neighborhood.x * viewport.zoom).toBeGreaterThanOrEqual(safeRect.x);
    expect(viewport.y + neighborhood.y * viewport.zoom).toBeGreaterThanOrEqual(safeRect.y);
    expect(viewport.x + (neighborhood.x + neighborhood.width) * viewport.zoom).toBeLessThanOrEqual(safeRect.x + safeRect.width);
    expect(viewport.y + (neighborhood.y + neighborhood.height) * viewport.zoom).toBeLessThanOrEqual(safeRect.y + safeRect.height);
  });

  it('does not shrink a local context below the reading floor', () => {
    const safeRect = { x: 16, y: 66, width: 760, height: 380 };
    const neighborhood = { x: 0, y: 0, width: 620, height: 650 };
    const focus = { x: 194, y: 279, width: 232, height: 92 };
    const viewport = readingViewport(neighborhood, focus, safeRect);
    expect(viewport.zoom).toBe(READING_MINIMUM_ZOOM);
    expect(viewport.x + focus.x * viewport.zoom).toBeGreaterThanOrEqual(safeRect.x);
    expect(viewport.y + focus.y * viewport.zoom).toBeGreaterThanOrEqual(safeRect.y);
  });

  it('frames the focus instead of shrinking complete context above a compact guide', () => {
    const safeRect = { x: 16, y: 110, width: 757, height: 286 };
    const neighborhood = { x: 0, y: 0, width: 1_216, height: 576 };
    const focus = { x: 384, y: 238, width: 232, height: 112 };
    const viewport = readingViewport(neighborhood, focus, safeRect);
    expect(viewport.zoom).toBe(READING_MINIMUM_ZOOM);
    expect(viewport.x + focus.x * viewport.zoom).toBeGreaterThanOrEqual(safeRect.x);
    expect(viewport.y + focus.y * viewport.zoom).toBeGreaterThanOrEqual(safeRect.y);
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

  it('keeps every outgoing alternative before it adds compact incoming context', () => {
    const graph = graphFixture('compact-context', ['incoming-a', 'incoming-b', 'selected', 'yes', 'no'], [
      { id: 'incoming-a', from: 'incoming-a', to: 'selected' },
      { id: 'incoming-b', from: 'incoming-b', to: 'selected' },
      { id: 'yes', from: 'selected', to: 'yes' },
      { id: 'no', from: 'selected', to: 'no' }
    ]);
    const context = compactNeighborhood(graph, 'selected');
    expect([...context.nodeIds]).toEqual(['selected', 'yes', 'no']);
    expect([...context.edgeIds]).toEqual(['yes', 'no']);
  });

  it('uses spare compact capacity for one stable predecessor', () => {
    const graph = graphFixture('compact-chain', ['incoming-a', 'incoming-b', 'selected', 'next'], [
      { id: 'incoming-a', from: 'incoming-a', to: 'selected' },
      { id: 'incoming-b', from: 'incoming-b', to: 'selected' },
      { id: 'next', from: 'selected', to: 'next' }
    ]);
    const context = compactNeighborhood(graph, 'selected');
    expect([...context.nodeIds]).toEqual(['selected', 'next', 'incoming-a']);
    expect([...context.edgeIds]).toEqual(['next', 'incoming-a']);
  });

  it('shows the first material split in the opening neighborhood', () => {
    const graph = graphFixture('opening', ['entry', 'preparation', 'decision', 'yes', 'no', 'later'], [
      { id: 'prepare', from: 'entry', to: 'preparation' },
      { id: 'decide', from: 'preparation', to: 'decision' },
      { id: 'yes', from: 'decision', to: 'yes' },
      { id: 'no', from: 'decision', to: 'no' },
      { id: 'later', from: 'yes', to: 'later' }
    ]);
    const context = openingNeighborhood(graph, 'entry');
    expect([...context.nodeIds]).toEqual(['entry', 'preparation', 'decision', 'yes', 'no']);
    expect([...context.edgeIds]).toEqual(['prepare', 'decide', 'yes', 'no']);
  });

  it('keeps a compact opening whole instead of showing part of a remote split', () => {
    const graph = graphFixture('compact-opening', ['entry', 'preparation', 'decision', 'yes', 'no'], [
      { id: 'prepare', from: 'entry', to: 'preparation' },
      { id: 'decide', from: 'preparation', to: 'decision' },
      { id: 'yes', from: 'decision', to: 'yes' },
      { id: 'no', from: 'decision', to: 'no' }
    ]);
    const context = compactOpeningNeighborhood(graph, 'entry');
    expect([...context.nodeIds]).toEqual(['entry', 'preparation', 'decision']);
    expect([...context.edgeIds]).toEqual(['prepare', 'decide']);
  });

  it('bounds a branch-free opening neighborhood', () => {
    const graph = graphFixture('opening-chain', ['one', 'two', 'three', 'four', 'five'], [
      { id: 'one-two', from: 'one', to: 'two' },
      { id: 'two-three', from: 'two', to: 'three' },
      { id: 'three-four', from: 'three', to: 'four' },
      { id: 'four-five', from: 'four', to: 'five' }
    ]);
    const context = openingNeighborhood(graph, 'one');
    expect([...context.nodeIds]).toEqual(['one', 'two', 'three', 'four']);
    expect([...context.edgeIds]).toEqual(['one-two', 'two-three', 'three-four']);
  });
});
