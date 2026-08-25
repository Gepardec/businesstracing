import { describe, expect, it } from 'vitest';
import { analyzeTopology } from './topology-analysis';
import { scorePlacement, selectPlacement, type PlacementCandidate } from './placement-profiles';
import { chainFixture, wideBranchFixture } from './graph-fixtures';

function candidate(profileId: string, width: number, height: number, positions: PlacementCandidate['positions']): PlacementCandidate {
  return { profileId, width, height, positions };
}

describe('placement profiles', () => {
  it('rejects an avoidably extreme aspect ratio for a non-chain graph', () => {
    const graph = wideBranchFixture(18);
    const topology = analyzeTopology(graph);
    const branches = graph.nodes.filter((node) => node.id.startsWith('branch-'));
    const extreme = candidate('extreme', 6_000, 800, [
      { id: 'entry', x: 2_700, y: 0 },
      ...branches.map((node, index) => ({ id: node.id, x: index * 300, y: 250 })),
      { id: 'outcome', x: 2_700, y: 500 }
    ]);
    const balanced = candidate('balanced', 1_800, 1_000, [
      { id: 'entry', x: 750, y: 0 },
      ...branches.map((node, index) => ({ id: node.id, x: (index % 6) * 300, y: 200 + Math.floor(index / 6) * 200 })),
      { id: 'outcome', x: 750, y: 800 }
    ]);
    expect(scorePlacement(graph, topology, extreme, 232, 92).aspectRatioPenalty).toBeGreaterThan(0);
    expect(selectPlacement(graph, topology, [extreme, balanced], 232, 92).profileId).toBe('balanced');
  });

  it('does not apply the aspect-ratio band to a structural chain', () => {
    const graph = chainFixture(5);
    const topology = analyzeTopology(graph);
    const vertical = candidate('vertical', 300, 1_200, graph.nodes.map((node, index) => ({ id: node.id, x: 0, y: index * 200 })));
    expect(scorePlacement(graph, topology, vertical, 232, 92).aspectRatioPenalty).toBe(0);
  });

  it('selects route quality before area', () => {
    const graph = chainFixture(3);
    const topology = analyzeTopology(graph);
    const positions = graph.nodes.map((node, index) => ({ id: node.id, x: 0, y: index * 200 }));
    const compact = candidate('compact', 300, 600, positions);
    const clear = candidate('clear', 600, 1_200, positions);
    const routeScores = new Map([
      ['compact', { unrelatedNodeIntrusions: 0, branchRegionViolations: 0, avoidableCrossings: 1, crossingDensity: 0.5, maximumDetourRatio: 1, totalDetour: 0 }],
      ['clear', { unrelatedNodeIntrusions: 0, branchRegionViolations: 0, avoidableCrossings: 0, crossingDensity: 0, maximumDetourRatio: 1, totalDetour: 0 }]
    ]);
    expect(selectPlacement(graph, topology, [compact, clear], 232, 92, routeScores).profileId).toBe('clear');
  });
});
