import { describe, expect, it } from 'vitest';
import { analyzeTopology } from './topology-analysis';
import { chainFixture, cycleFixture, duplicateLabelFixture, fanInFixture, graphFixture, longShortcutFixture } from './graph-fixtures';

describe('static graph topology analysis', () => {
  it('assigns stable directed ranks from entry to outcome', () => {
    const graph = chainFixture(12);
    const first = analyzeTopology(graph);
    const second = analyzeTopology(graph);
    expect([...second.rankByNodeId]).toEqual([...first.rankByNodeId]);
    expect(first.rankByNodeId.get('entry')).toBe(0);
    expect(first.rankByNodeId.get('outcome')).toBe(11);
    expect(first.spineNodeIds).toHaveLength(12);
  });

  it('collapses cycles for rank calculation and marks return edges as long', () => {
    const graph = cycleFixture();
    const topology = analyzeTopology(graph);
    const cycle = topology.stronglyConnectedComponents.find((component) => component.cyclic);
    expect(cycle?.nodeIds).toEqual(['rule-a', 'rule-b']);
    expect(topology.longEdgeIds).toContain(graph.edges.find((edge) => edge.outcome === 'retry')!.id);
    expect(topology.rankByNodeId.get('outcome')).toBeGreaterThan(topology.rankByNodeId.get('rule-a')!);
  });

  it('finds a feedback edge from graph direction instead of node ID order', () => {
    const graph = graphFixture('ordered-cycle', ['entry', 'z-forward', 'a-return', 'outcome'], [
      { id: 'enter', from: 'entry', to: 'z-forward' },
      { id: 'continue', from: 'z-forward', to: 'a-return' },
      { id: 'retry', from: 'a-return', to: 'z-forward' },
      { id: 'finish', from: 'a-return', to: 'outcome' }
    ]);
    const topology = analyzeTopology(graph);
    expect(topology.longEdgeIds).toContain('retry');
    expect(topology.longEdgeIds).not.toContain('continue');
  });

  it('finds convergence, long edges, and stable duplicate occurrences', () => {
    expect(analyzeTopology(fanInFixture()).convergenceGroups[0].incomingEdgeIds).toHaveLength(12);
    expect(analyzeTopology(longShortcutFixture()).longEdgeIds.size).toBeGreaterThan(0);
    const duplicates = analyzeTopology(duplicateLabelFixture()).duplicateByNodeId;
    expect(duplicates.get('first-check')).toEqual({ index: 1, total: 2 });
    expect(duplicates.get('second-check')).toEqual({ index: 2, total: 2 });
  });
});
