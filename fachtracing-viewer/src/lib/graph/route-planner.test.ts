import { describe, expect, it } from 'vitest';
import { computeLayout, NODE_HEIGHT, NODE_WIDTH } from './layout-engine';
import { balancedBranchFixture, chainFixture, crossingFixture, cycleFixture, diamondFixture, duplicateLabelFixture, fanInFixture, fixedPortDetourFixture, longShortcutFixture, multipleEntryFixture } from './graph-fixtures';
import { parallelClearanceViolations } from './route-quality';

describe('static graph route planning', () => {
  it('uses compact ranks for a chain', async () => {
    const layout = await computeLayout(chainFixture());
    expect(new Set(layout.nodes.map((node) => node.x)).size).toBe(1);
    expect(layout.metrics.nodeOverlaps).toBe(0);
    expect(layout.metrics.unrelatedNodeIntrusions).toBe(0);
  });

  it('uses four-side ports when a side route is shorter', async () => {
    const layout = await computeLayout(fixedPortDetourFixture());
    const sidePorts = layout.edges.filter((edge) => edge.sourcePort.side === 'east' || edge.sourcePort.side === 'west');
    expect(sidePorts.length).toBeGreaterThan(0);
    expect(layout.metrics.unrelatedNodeIntrusions).toBe(0);
  });

  it('keeps the first route segment outside its source node', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    for (const route of layout.edges) {
      const [port, lead] = route.points;
      if (route.sourcePort.side === 'east') expect(lead.x).toBeGreaterThan(port.x);
      if (route.sourcePort.side === 'west') expect(lead.x).toBeLessThan(port.x);
      if (route.sourcePort.side === 'north') expect(lead.y).toBeLessThan(port.y);
      if (route.sourcePort.side === 'south') expect(lead.y).toBeGreaterThan(port.y);
    }
  });

  it('keeps normal routes inside the graph instead of using remote outer corridors', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    for (const route of layout.edges.filter((edge) => !edge.long)) {
      expect(route.points.every((point) => point.x >= 0 && point.x <= layout.width && point.y >= 0 && point.y <= layout.height), route.id).toBe(true);
    }
  });

  it('does not force a topology-long shortcut into an outer corridor', async () => {
    const layout = await computeLayout(longShortcutFixture());
    const longRoutes = layout.edges.filter((edge) => edge.long);
    expect(longRoutes.length).toBeGreaterThan(0);
    expect(longRoutes.every((route) => route.points.every((point) => point.x >= 0 && point.x <= layout.width && point.y >= 0 && point.y <= layout.height)), longRoutes.map((route) => route.id).join(', ')).toBe(true);
    expect(longRoutes.every((route) => route.corridor === 'normal')).toBe(true);
    expect(longRoutes.every((route) => route.secondary)).toBe(true);
    expect(longRoutes.every((route) => route.length === route.shortestCandidateLength)).toBe(true);
    expect(layout.metrics.longEdgeCorridorViolations).toBe(0);
  });

  it('keeps the primary forward skeleton direct and marks later links as secondary', async () => {
    const layout = await computeLayout(longShortcutFixture());
    const primary = layout.edges.filter((edge) => !edge.secondary);
    expect(primary).toHaveLength(5);
    expect(primary.every((edge) => edge.corridor === 'normal' && edge.length === edge.shortestCandidateLength)).toBe(true);
    expect(layout.edges.find((edge) => edge.id === 'edge-005')?.secondary).toBe(true);
  });

  it('keeps every branch label attached to its route', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    for (const route of layout.edges.filter((edge) => edge.displayLabel)) {
      expect(Math.hypot(route.labelPosition.x - route.labelAnchor.x, route.labelPosition.y - route.labelAnchor.y), route.id).toBeLessThanOrEqual(24);
    }
    expect(layout.metrics.detachedLabels).toBe(0);
  });

  it('keeps labels clear of route endpoints and gives business rules four readable lines', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    expect(NODE_HEIGHT).toBeGreaterThanOrEqual(108);
    for (const route of layout.edges.filter((edge) => edge.displayLabel)) {
      const sourceDistance = Math.hypot(
        route.labelPosition.x - route.points[0].x,
        route.labelPosition.y - route.points[0].y
      );
      const target = route.points.at(-1)!;
      const targetDistance = Math.hypot(
        route.labelPosition.x - target.x,
        route.labelPosition.y - target.y
      );
      expect(sourceDistance, `${route.id} source`).toBeGreaterThanOrEqual(30);
      expect(targetDistance, `${route.id} target`).toBeGreaterThanOrEqual(30);
    }
  });

  it('separates adjacent ports so rounded exits do not look like duplicate loops', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    const sourcePorts = layout.edges
      .filter((edge) => edge.sourceNodeId === 'root')
      .map((edge) => edge.sourcePort)
      .filter((port) => port.side === 'south')
      .sort((first, second) => first.point.x - second.point.x);

    expect(sourcePorts).toHaveLength(2);
    expect(sourcePorts[1].point.x - sourcePorts[0].point.x).toBeGreaterThanOrEqual(40);
  });

  it('keeps sibling roots in one local branch band around the source', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    const nodeById = new Map(layout.nodes.map((node) => [node.id, node]));
    expect(Math.abs(nodeById.get('yes-1')!.y - nodeById.get('no-1')!.y)).toBeLessThanOrEqual(NODE_HEIGHT);
    const siblingCenter = (nodeById.get('yes-1')!.x + nodeById.get('no-1')!.x + nodeById.get('yes-1')!.width) / 2;
    expect(Math.abs(siblingCenter - (nodeById.get('root')!.x + nodeById.get('root')!.width / 2))).toBeLessThanOrEqual(NODE_WIDTH / 2);
  });

  it('places multiple entries in the first rank and outcomes last', async () => {
    const layout = await computeLayout(multipleEntryFixture());
    const nodeById = new Map(layout.nodes.map((node) => [node.id, node]));
    expect(nodeById.get('entry-a')!.y).toBe(nodeById.get('entry-b')!.y);
    expect(nodeById.get('outcome-a')!.y).toBeGreaterThan(nodeById.get('rule-a')!.y);
    expect(nodeById.get('outcome-b')!.y).toBeGreaterThan(nodeById.get('rule-b')!.y);
  });

  it('shows a multi-node cycle as one presentation-only region', async () => {
    const layout = await computeLayout(cycleFixture());
    expect(layout.regions.filter((region) => region.label === 'Cycle')).toHaveLength(0);
    expect(layout.nodes).toHaveLength(4);
    const loopback = layout.edges.find((edge) => edge.id === 'edge-002')!;
    expect(loopback.long).toBe(true);
    expect(loopback.secondary).toBe(true);
    expect(loopback.feedback).toBe(true);
    expect(loopback.length).toBe(loopback.shortestCandidateLength);
    expect(loopback.corridor).toBe('cycle');
    expect(layout.metrics.wrongWayBoundaryExits).toBe(0);
    expect(layout.metrics.labelCollisions).toBe(0);
  });

  it('keeps branch labels visible and source-adjacent', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    const labels = layout.edges.map((edge) => edge.displayLabel).filter(Boolean);
    expect(labels).toEqual(expect.arrayContaining(['Yes', 'No', 'accepted', 'declined']));
    expect(layout.edges.filter((edge) => edge.sourcePort.nodeId === 'root').every((edge) => edge.displayLabel !== null)).toBe(true);
    expect(layout.metrics.labelCollisions).toBe(0);
    expect(layout.metrics.branchRegionViolations).toBe(0);
    expect(layout.metrics.avoidableCrossings).toBe(0);
    expect(parallelClearanceViolations(layout.edges)).toEqual([]);
  });

  it('keeps every business alternative distinct from quiet topology references', async () => {
    const layout = await computeLayout(balancedBranchFixture());
    const alternatives = layout.edges.filter((edge) => edge.sourceNodeId === 'root');
    const shortcutLayout = await computeLayout(longShortcutFixture());
    const shortcutAlternatives = shortcutLayout.edges.filter((edge) => edge.sourceNodeId === 'rule-1');

    expect(alternatives).toHaveLength(2);
    expect(alternatives.map((edge) => edge.displayLabel).sort()).toEqual(['No', 'Yes']);
    expect(alternatives.every((edge) => edge.branch)).toBe(true);
    expect(shortcutAlternatives).toHaveLength(2);
    expect(shortcutAlternatives.every((edge) => edge.branch)).toBe(true);
    expect(shortcutAlternatives.some((edge) => edge.secondary)).toBe(true);
  });

  it('creates one presentation junction for large fan-in', async () => {
    const layout = await computeLayout(fanInFixture());
    expect(layout.junctions).toHaveLength(1);
    expect(layout.sharedSegments).toHaveLength(1);
    expect(layout.sharedSegments[0].targetNodeId).toBe('outcome');
    expect(layout.junctions[0].incomingEdgeIds).toHaveLength(12);
    expect(layout.edges.filter((edge) => edge.sharedSegmentIds.length > 0)).toHaveLength(12);
    expect(layout.metrics.labelCollisions).toBe(0);
    expect(parallelClearanceViolations(layout.edges)).toEqual([]);
  });

  it('does not create a junction for a two-way diamond', async () => {
    const layout = await computeLayout(diamondFixture());
    expect(layout.junctions).toHaveLength(0);
    expect(layout.sharedSegments).toHaveLength(0);
  });

  it('classifies non-planar route intersections as crossings', async () => {
    const layout = await computeLayout(crossingFixture());
    expect(layout.crossings.length).toBeGreaterThan(0);
    expect(layout.crossings.every((crossing) => crossing.overEdgeId !== crossing.underEdgeId)).toBe(true);
    expect(layout.metrics.avoidableCrossings).toBe(0);
    expect(layout.metrics.unavoidableCrossings).toBe(layout.crossings.length);
    expect(layout.metrics.unrelatedNodeIntrusions).toBe(0);
  });

  it('keeps duplicate occurrence markers stable', async () => {
    const first = await computeLayout(duplicateLabelFixture());
    const second = await computeLayout(duplicateLabelFixture());
    expect(second.nodes).toEqual(first.nodes);
    expect(first.nodes.find((node) => node.id === 'first-check')?.occurrence).toEqual({ index: 1, total: 2 });
  });
});
