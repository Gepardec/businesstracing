import { describe, expect, it } from 'vitest';
import type { GraphModel } from '$contracts/graph-contract';
import type { RunModel } from '$contracts/run-contract';
import { deriveRunHighlight, explainObservation } from './run-highlight';

const graph = {
  id: 'g', version: 1, nodes: [{ id: 'a', label: 'route exists' }, { id: 'b', label: 'route accepted' }],
  edges: [{ id: 'e', from: 'a', to: 'b' }]
} as unknown as GraphModel;
const run = { observations: [
  { sequence: 1, nodeId: 'a', outcome: 'true', evidence: {}, selectedEdgeId: null },
  { sequence: 2, nodeId: 'b', outcome: 'completed', evidence: {}, selectedEdgeId: null },
  { sequence: 3, nodeId: 'a', outcome: 'true', evidence: { route: { type: 'string', canonicalValue: '17', displayValue: 'Route 17' } }, selectedEdgeId: 'e' }
] } as unknown as RunModel;

describe('run highlighting', () => {
  it('keeps repeated visits and resolves direct connecting edges', () => {
    const highlight = deriveRunHighlight(graph, run, 2);
    expect(highlight.activeSequence).toBe(3);
    expect(highlight.activeStepNumber).toBe(3);
    expect(highlight.pathNodeIds).toEqual(new Set(['a', 'b']));
    expect(highlight.pathEdgeIds).toEqual(new Set(['e']));
  });

  it('does not invent absent evidence', () => {
    expect(explainObservation(graph, run.observations[0])).toContain('No additional evidence was recorded');
    expect(explainObservation(graph, run.observations[2])).toContain('route was Route 17');
  });
});
