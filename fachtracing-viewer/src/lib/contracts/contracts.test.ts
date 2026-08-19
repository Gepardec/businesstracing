import { describe, expect, it } from 'vitest';
import { parseDeveloperGraph } from './graph-contract';
import { assertRunMatchesGraph, parseDecisionRecord } from './run-contract';

const graphDocument = {
  schema: 'fachtracing-developer-graph/v1',
  graph: {
    id: 'graph-1', version: 1, label: 'choose route', entryNodeId: 'entry', completeness: 'COMPLETE',
    nodes: [
      { id: 'entry', kind: 'ENTRY', label: 'request received', attributes: {} },
      { id: 'choice', kind: 'PREDICATE', label: 'route is local', attributes: {} },
      { id: 'result', kind: 'OUTCOME', label: 'route selected', attributes: {} }
    ],
    edges: [
      { id: 'e1', from: 'entry', to: 'choice', outcome: 'next' },
      { id: 'e2', from: 'choice', to: 'result', outcome: 'yes' }
    ],
    coverageGaps: []
  },
  sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'test', checksum: 'fixture' }], sourceFiles: []
};

const runDocument = {
  schema: 'fachtracing-decision-record/v1', recordId: 'record-1', executionId: 'execution-1',
  graphId: 'graph-1', graphVersion: 1, startedAt: '2026-08-19T10:00:00Z', completedAt: '2026-08-19T10:00:01Z',
  status: 'SUCCEEDED', terminalStatus: 'COMPLETED', completeness: 'COMPLETE',
  finalDecision: { type: 'string', canonicalValue: 'local', displayValue: 'Local route' }, failure: null,
  observations: [
    { sequence: 2, nodeId: 'choice', outcome: 'true', evidence: { route: { type: 'string', canonicalValue: 'local', displayValue: 'Local' } }, selectedEdgeId: 'e2' },
    { sequence: 1, nodeId: 'entry', outcome: 'entered', evidence: {}, selectedEdgeId: 'e1' }
  ],
  coverageGaps: [], correlationKeys: {}, redactionPolicyId: 'test', futureField: true
};

describe('contract adapters', () => {
  it('preserves graph and ordered run IDs while ignoring unknown fields', () => {
    const graph = parseDeveloperGraph(graphDocument);
    const run = parseDecisionRecord({ ...runDocument, futureField: true });
    assertRunMatchesGraph(run, graph);
    expect(graph.edges.map((edge) => edge.id)).toEqual(['e1', 'e2']);
    expect(run.observations.map((item) => item.nodeId)).toEqual(['entry', 'choice']);
    expect(run.observations[1].selectedEdgeId).toBe('e2');
    expect(run.correlationKeys).toEqual({});
  });

  it('rejects unsupported schemas and dangling references', () => {
    expect(() => parseDeveloperGraph({ ...graphDocument, schema: 'fachtracing-developer-graph/v2' })).toThrow(/unsupported/);
    const dangling = structuredClone(graphDocument);
    dangling.graph.edges[0].to = 'missing';
    expect(() => parseDeveloperGraph(dangling)).toThrow(/unknown node/);
  });

  it('rejects graph and run version mismatches', () => {
    const graph = parseDeveloperGraph(graphDocument);
    const run = parseDecisionRecord({ ...runDocument, graphVersion: 2 });
    expect(() => assertRunMatchesGraph(run, graph)).toThrow(/versions do not match/);
  });
});
