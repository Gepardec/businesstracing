import type { GraphEdge, GraphModel, GraphNode, NodeKind } from '$contracts/graph-contract';

type FixtureNode = string | { id: string; kind?: NodeKind; label?: string };
type FixtureEdge = { from: string; to: string; outcome?: string; id?: string };

export function graphFixture(id: string, nodes: readonly FixtureNode[], edges: readonly FixtureEdge[], entryNodeIds: readonly string[] = ['entry']): GraphModel {
  const graphNodes: GraphNode[] = nodes.map((node, index) => {
    const value = typeof node === 'string' ? { id: node } : node;
    return Object.freeze({
      id: value.id,
      kind: value.kind ?? (entryNodeIds.includes(value.id) ? 'ENTRY' : index === nodes.length - 1 ? 'OUTCOME' : 'PREDICATE'),
      label: value.label ?? value.id.replaceAll('-', ' '),
      attributes: Object.freeze({})
    });
  });
  const graphEdges: GraphEdge[] = edges.map((edge, index) => Object.freeze({
    id: edge.id ?? `edge-${String(index).padStart(3, '0')}`,
    from: edge.from,
    to: edge.to,
    outcome: edge.outcome ?? 'next'
  }));
  return Object.freeze({
    schema: 'fachtracing-developer-graph/v1',
    id,
    version: 1,
    label: id.replaceAll('-', ' '),
    entryNodeId: entryNodeIds[0],
    entryNodeIds: Object.freeze([...entryNodeIds]),
    completeness: 'COMPLETE',
    nodes: Object.freeze(graphNodes),
    edges: Object.freeze(graphEdges),
    coverageGaps: Object.freeze([])
  });
}

export function chainFixture(count = 12): GraphModel {
  const nodes = Array.from({ length: count }, (_, index) => ({
    id: index === 0 ? 'entry' : index === count - 1 ? 'outcome' : `rule-${index}`,
    kind: index === 0 ? 'ENTRY' as const : index === count - 1 ? 'OUTCOME' as const : 'PREDICATE' as const
  }));
  return graphFixture('chain-fixture', nodes, nodes.slice(1).map((node, index) => ({ from: nodes[index].id, to: node.id })));
}

export function balancedBranchFixture(): GraphModel {
  return graphFixture('balanced-branch-fixture', ['entry', 'root', 'yes-1', 'no-1', 'yes-2', 'no-2', 'outcome'], [
    { from: 'entry', to: 'root' },
    { from: 'root', to: 'yes-1', outcome: 'true' },
    { from: 'root', to: 'no-1', outcome: 'false' },
    { from: 'yes-1', to: 'yes-2', outcome: 'next' },
    { from: 'no-1', to: 'no-2', outcome: 'next' },
    { from: 'yes-2', to: 'outcome', outcome: 'accepted' },
    { from: 'no-2', to: 'outcome', outcome: 'declined' }
  ]);
}

export function diamondFixture(): GraphModel {
  return graphFixture('diamond-fixture', ['entry', 'rule', 'yes-action', 'no-action', 'outcome'], [
    { from: 'entry', to: 'rule' },
    { from: 'rule', to: 'yes-action', outcome: 'true' },
    { from: 'rule', to: 'no-action', outcome: 'false' },
    { from: 'yes-action', to: 'outcome' },
    { from: 'no-action', to: 'outcome' }
  ]);
}

export function fanInFixture(count = 12): GraphModel {
  const feeders = Array.from({ length: count }, (_, index) => `feeder-${String(index).padStart(2, '0')}`);
  return graphFixture('fan-in-fixture', ['entry', ...feeders, 'outcome'], [
    ...feeders.map((feeder, index) => ({ from: 'entry', to: feeder, outcome: `Branch ${index + 1}` })),
    ...feeders.map((feeder) => ({ from: feeder, to: 'outcome', outcome: 'next' }))
  ]);
}

export function fixedPortDetourFixture(): GraphModel {
  return graphFixture('fixed-port-detour-fixture', ['entry', 'left', 'right', 'outcome'], [
    { from: 'entry', to: 'left', outcome: 'true' },
    { from: 'entry', to: 'right', outcome: 'false' },
    { from: 'left', to: 'outcome' },
    { from: 'right', to: 'outcome' }
  ]);
}

export function longShortcutFixture(): GraphModel {
  return graphFixture('long-shortcut-fixture', ['entry', 'rule-1', 'rule-2', 'rule-3', 'rule-4', 'outcome'], [
    { from: 'entry', to: 'rule-1' },
    { from: 'rule-1', to: 'rule-2', outcome: 'false' },
    { from: 'rule-2', to: 'rule-3', outcome: 'false' },
    { from: 'rule-3', to: 'rule-4', outcome: 'false' },
    { from: 'rule-4', to: 'outcome', outcome: 'next' },
    { from: 'rule-1', to: 'outcome', outcome: 'true' }
  ]);
}

export function duplicateLabelFixture(): GraphModel {
  return graphFixture('duplicate-label-fixture', [
    'entry',
    { id: 'first-check', label: 'email exists' },
    { id: 'second-check', label: 'email exists' },
    'outcome'
  ], [
    { from: 'entry', to: 'first-check' },
    { from: 'first-check', to: 'second-check', outcome: 'true' },
    { from: 'second-check', to: 'outcome', outcome: 'true' }
  ]);
}

export function multipleEntryFixture(): GraphModel {
  return graphFixture('multiple-entry-fixture', ['entry-a', 'entry-b', 'rule-a', 'rule-b', 'outcome-a', 'outcome-b'], [
    { from: 'entry-a', to: 'rule-a' },
    { from: 'entry-b', to: 'rule-b' },
    { from: 'rule-a', to: 'outcome-a' },
    { from: 'rule-b', to: 'outcome-b' }
  ], ['entry-a', 'entry-b']);
}

export function cycleFixture(): GraphModel {
  return graphFixture('cycle-fixture', ['entry', 'rule-a', 'rule-b', 'outcome'], [
    { from: 'entry', to: 'rule-a' },
    { from: 'rule-a', to: 'rule-b', outcome: 'true' },
    { from: 'rule-b', to: 'rule-a', outcome: 'retry' },
    { from: 'rule-b', to: 'outcome', outcome: 'done' }
  ]);
}

export function crossingFixture(): GraphModel {
  const entries = ['entry-a', 'entry-b', 'entry-c'];
  const checks = ['check-a', 'check-b', 'check-c'];
  return graphFixture('crossing-fixture', [...entries, ...checks, 'outcome'], [
    ...entries.flatMap((entry) => checks.map((check) => ({ from: entry, to: check, outcome: check.replace('check-', '') }))),
    ...checks.map((check) => ({ from: check, to: 'outcome' }))
  ], entries);
}

export function generatedSafetyFixture(count = 250): GraphModel {
  return chainFixture(count);
}

export function deepBranchFixture(depth = 14): GraphModel {
  const spine = Array.from({ length: depth }, (_, index) => `spine-${String(index).padStart(2, '0')}`);
  const exits = Array.from({ length: depth }, (_, index) => `exit-${String(index).padStart(2, '0')}`);
  return graphFixture('deep-branch-fixture', ['entry', ...spine, ...exits, 'outcome'], [
    { from: 'entry', to: spine[0] },
    ...spine.flatMap((nodeId, index) => [
      { from: nodeId, to: exits[index], outcome: 'stop' },
      { from: nodeId, to: index === spine.length - 1 ? 'outcome' : spine[index + 1], outcome: 'continue' }
    ]),
    ...exits.map((nodeId) => ({ from: nodeId, to: 'outcome' }))
  ]);
}

export function wideBranchFixture(branchCount = 18): GraphModel {
  const branches = Array.from({ length: branchCount }, (_, index) => `branch-${String(index).padStart(2, '0')}`);
  return graphFixture('wide-branch-fixture', ['entry', ...branches, 'outcome'], [
    ...branches.map((nodeId, index) => ({ from: 'entry', to: nodeId, outcome: `branch ${index + 1}` })),
    ...branches.map((nodeId) => ({ from: nodeId, to: 'outcome' }))
  ]);
}

export function denseConvergenceFixture(width = 8): GraphModel {
  const firstLayer = Array.from({ length: width }, (_, index) => `source-${String(index).padStart(2, '0')}`);
  const secondLayer = Array.from({ length: Math.max(3, Math.ceil(width / 2)) }, (_, index) => `check-${String(index).padStart(2, '0')}`);
  return graphFixture('dense-convergence-fixture', ['entry', ...firstLayer, ...secondLayer, 'outcome'], [
    ...firstLayer.map((nodeId, index) => ({ from: 'entry', to: nodeId, outcome: `source ${index + 1}` })),
    ...firstLayer.flatMap((sourceId, sourceIndex) => secondLayer
      .filter((_, targetIndex) => (sourceIndex + targetIndex) % 2 === 0)
      .map((targetId) => ({ from: sourceId, to: targetId }))),
    ...secondLayer.map((nodeId) => ({ from: nodeId, to: 'outcome' }))
  ]);
}
