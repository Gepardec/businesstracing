type NodeKind = 'ENTRY' | 'PREDICATE' | 'CHOICE' | 'COMPUTATION' | 'DISPATCH' | 'OUTCOME' | 'COVERAGE_GAP';

interface FixtureNode {
  id: string;
  kind: NodeKind;
  label: string;
  attributes: Record<string, never>;
}

interface FixtureEdge {
  id: string;
  from: string;
  to: string;
  outcome: string;
}

export interface UploadFile {
  name: string;
  mimeType: string;
  buffer: Buffer;
}

function node(id: string, kind: NodeKind = 'PREDICATE', label = id.replaceAll('-', ' ')): FixtureNode {
  return { id, kind, label, attributes: {} };
}

function graphFile(name: string, label: string, nodes: FixtureNode[], edges: FixtureEdge[], entryNodeId: string, entryNodeIds = [entryNodeId]): UploadFile {
  const document = {
    schema: 'fachtracing-developer-graph/v1',
    sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: `browser ${name}`, checksum: 'fixture' }],
    sourceFiles: [],
    graph: {
      id: name,
      version: 1,
      label,
      entryNodeId,
      entryNodeIds,
      completeness: 'COMPLETE',
      nodes,
      edges,
      coverageGaps: []
    }
  };
  return { name: `${name}.json`, mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(document)) };
}

export function generatedBranchingGraphFile(ruleCount = 2, finalParallelEdgeCount = 2): UploadFile {
  const rules = Array.from({ length: ruleCount }, (_, index) => node(`rule-${index}`, 'PREDICATE', `generated rule ${index + 1} matches`));
  const edges: FixtureEdge[] = [{ id: 'edge-entry', from: 'entry', to: rules[0].id, outcome: 'next' }];
  rules.forEach((rule, index) => {
    if (index < rules.length - 1) edges.push({ id: `edge-${rule.id}-continue`, from: rule.id, to: rules[index + 1].id, outcome: 'no' });
    const exitCount = index === rules.length - 1 ? finalParallelEdgeCount : 1;
    for (let exit = 0; exit < exitCount; exit += 1) {
      edges.push({ id: `edge-${rule.id}-outcome-${exit}`, from: rule.id, to: 'outcome', outcome: exit % 2 ? 'yes' : 'no' });
    }
  });
  return graphFile('generated-browser-branching', 'generated branching route proof', [
    node('entry', 'ENTRY', 'start'),
    ...rules,
    node('outcome', 'OUTCOME', 'selected result')
  ], edges, 'entry');
}

export function fanInGraphFile(count = 12): UploadFile {
  const feeders = Array.from({ length: count }, (_, index) => node(`feeder-${String(index).padStart(2, '0')}`, 'COMPUTATION', `prepare source ${index + 1}`));
  return graphFile('generated-browser-fan-in', 'twelve source convergence', [
    node('entry', 'ENTRY', 'start'),
    ...feeders,
    node('outcome', 'OUTCOME', 'combined result')
  ], [
    ...feeders.map((feeder, index) => ({ id: `edge-branch-${index}`, from: 'entry', to: feeder.id, outcome: `Source ${index + 1}` })),
    ...feeders.map((feeder, index) => ({ id: `edge-merge-${index}`, from: feeder.id, to: 'outcome', outcome: 'next' }))
  ], 'entry');
}

export function duplicateGraphFile(): UploadFile {
  return graphFile('generated-browser-duplicates', 'repeated checks', [
    node('entry', 'ENTRY', 'start'),
    node('first-check', 'PREDICATE', 'email exists'),
    node('second-check', 'PREDICATE', 'email exists'),
    node('outcome', 'OUTCOME', 'complete')
  ], [
    { id: 'edge-0', from: 'entry', to: 'first-check', outcome: 'next' },
    { id: 'edge-1', from: 'first-check', to: 'second-check', outcome: 'true' },
    { id: 'edge-2', from: 'second-check', to: 'outcome', outcome: 'true' }
  ], 'entry');
}

export function cycleGraphFile(): UploadFile {
  return graphFile('generated-browser-cycle', 'retry cycle', [
    node('entry', 'ENTRY', 'start'),
    node('first-check', 'PREDICATE', 'input is ready'),
    node('retry-check', 'PREDICATE', 'retry is permitted'),
    node('outcome', 'OUTCOME', 'complete')
  ], [
    { id: 'edge-0', from: 'entry', to: 'first-check', outcome: 'next' },
    { id: 'edge-1', from: 'first-check', to: 'retry-check', outcome: 'true' },
    { id: 'edge-2', from: 'retry-check', to: 'first-check', outcome: 'retry' },
    { id: 'edge-3', from: 'retry-check', to: 'outcome', outcome: 'done' }
  ], 'entry');
}

export function longShortcutGraphFile(): UploadFile {
  const rules = Array.from({ length: 4 }, (_, index) => node(`rule-${index + 1}`, 'PREDICATE', `validation step ${index + 1}`));
  return graphFile('generated-browser-long-route', 'long continuation proof', [
    node('entry', 'ENTRY', 'start'),
    ...rules,
    node('outcome', 'OUTCOME', 'complete')
  ], [
    { id: 'edge-entry', from: 'entry', to: 'rule-1', outcome: 'next' },
    { id: 'edge-1', from: 'rule-1', to: 'rule-2', outcome: 'No' },
    { id: 'edge-2', from: 'rule-2', to: 'rule-3', outcome: 'No' },
    { id: 'edge-3', from: 'rule-3', to: 'rule-4', outcome: 'No' },
    { id: 'edge-outcome', from: 'rule-4', to: 'outcome', outcome: 'next' },
    { id: 'edge-shortcut', from: 'rule-1', to: 'outcome', outcome: 'Yes' }
  ], 'entry');
}

export function crossingGraphFile(): UploadFile {
  const entries = ['entry-a', 'entry-b', 'entry-c'];
  const checks = ['check-a', 'check-b', 'check-c'];
  return graphFile('generated-browser-crossing', 'crossing bridge proof', [
    ...entries.map((id) => node(id, 'ENTRY')),
    ...checks.map((id) => node(id)),
    node('outcome', 'OUTCOME')
  ], [
    ...entries.flatMap((entry) => checks.map((check) => ({ id: `${entry}-${check}`, from: entry, to: check, outcome: check.replace('check-', '') }))),
    ...checks.map((check) => ({ id: `${check}-outcome`, from: check, to: 'outcome', outcome: 'next' }))
  ], 'entry-a', entries);
}
