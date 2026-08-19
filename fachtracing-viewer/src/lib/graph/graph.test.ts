import { describe, expect, it } from 'vitest';
import { parseDeveloperGraph, type GraphModel } from '$contracts/graph-contract';
import { computeLayout, NODE_HEIGHT, NODE_WIDTH } from './layout-engine';

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

function generatedBranchingGraph(ruleCount = 2, finalParallelEdgeCount = 2): GraphModel {
  const rules = Array.from({ length: ruleCount }, (_, index) => ({ id: `rule-${index}`, kind: 'PREDICATE', label: `generated rule ${index}`, attributes: {} }));
  const edges = [{ id: 'entry-rule-0', from: 'entry', to: rules[0].id, outcome: 'next' }];
  rules.forEach((rule, index) => {
    if (index < rules.length - 1) edges.push({ id: `${rule.id}-continue`, from: rule.id, to: rules[index + 1].id, outcome: 'no' });
    const exitCount = index === rules.length - 1 ? finalParallelEdgeCount : 1;
    for (let exit = 0; exit < exitCount; exit += 1) edges.push({ id: `${rule.id}-outcome-${exit}`, from: rule.id, to: 'outcome', outcome: exit % 2 ? 'yes' : 'no' });
  });
  return parseDeveloperGraph({
    schema: 'fachtracing-developer-graph/v1', sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'test', checksum: 'fixture' }], sourceFiles: [],
    graph: {
      id: 'generated-branching', version: 1, label: 'generated branching graph', entryNodeId: 'entry', completeness: 'COMPLETE',
      nodes: [{ id: 'entry', kind: 'ENTRY', label: 'start', attributes: {} }, ...rules, { id: 'outcome', kind: 'OUTCOME', label: 'stop', attributes: {} }],
      edges,
      coverageGaps: []
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

  it('returns distinct obstacle-safe routes for branches and parallel edges', async () => {
    const graph = generatedBranchingGraph();
    const result = await computeLayout(graph);
    const nodes = new Map(result.nodes.map((node) => [node.id, node]));
    expect(result.edges).toHaveLength(graph.edges.length);
    const parallel = result.edges.filter((edge) => edge.id.startsWith('rule-1-outcome'));
    expect(parallel[0].points).not.toEqual(parallel[1].points);
    for (const route of result.edges) {
      const modelEdge = graph.edges.find((edge) => edge.id === route.id)!;
      for (let index = 1; index < route.points.length; index += 1) {
        const start = route.points[index - 1];
        const end = route.points[index];
        const length = Math.hypot(end.x - start.x, end.y - start.y);
        for (let offset = 0; offset <= length; offset += 2) {
          const ratio = length === 0 ? 0 : offset / length;
          const point = { x: start.x + (end.x - start.x) * ratio, y: start.y + (end.y - start.y) * ratio };
          for (const [nodeId, node] of nodes) {
            if (nodeId === modelEdge.from || nodeId === modelEdge.to) continue;
            const inside = point.x > node.x && point.x < node.x + NODE_WIDTH && point.y > node.y && point.y < node.y + NODE_HEIGHT;
            expect(inside, `${route.id} entered ${nodeId} at ${point.x},${point.y}`).toBe(false);
          }
        }
      }
    }
  });
});
