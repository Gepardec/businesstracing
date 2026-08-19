import { describe, expect, it } from 'vitest';
import { parseDeveloperGraph, type GraphModel } from '$contracts/graph-contract';
import { computeLayout } from './layout-engine';

function generatedGraph(count: number): GraphModel {
  return parseDeveloperGraph({
    schema: 'fachtracing-developer-graph/v1', sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'test', checksum: 'fixture' }], sourceFiles: [],
    graph: {
      id: `generated-${count}`, version: 1, label: 'generated safety graph', entryNodeId: 'n-000', completeness: 'COMPLETE',
      nodes: Array.from({ length: count }, (_, index) => ({
        id: `n-${String(index).padStart(3, '0')}`, kind: index === 0 ? 'ENTRY' : index === count - 1 ? 'OUTCOME' : 'PREDICATE',
        label: `generated node ${index}`, attributes: {}
      })),
      edges: Array.from({ length: count - 1 }, (_, index) => ({
        id: `e-${String(index).padStart(3, '0')}`, from: `n-${String(index).padStart(3, '0')}`,
        to: `n-${String(index + 1).padStart(3, '0')}`, outcome: 'next'
      })), coverageGaps: []
    }
  });
}

describe('ELK graph layout', () => {
  it('is deterministic and top-to-bottom', async () => {
    const graph = generatedGraph(12);
    const first = await computeLayout(graph);
    expect(await computeLayout(graph)).toEqual(first);
    expect(first.nodes.at(-1)!.y).toBeGreaterThan(first.nodes[0].y);
  });

  it('lays out the 250-node safety graph within two seconds', async () => {
    const started = performance.now();
    const result = await computeLayout(generatedGraph(250));
    expect(result.nodes).toHaveLength(250);
    expect(performance.now() - started).toBeLessThan(2_000);
  });
});
