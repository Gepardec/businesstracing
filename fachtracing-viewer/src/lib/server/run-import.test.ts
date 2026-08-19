import { describe, expect, it } from 'vitest';
import type { Pool } from 'pg';
import { RunImportRepository } from './run-import-repository.server';

function bytes(result = 'BUSINESS_ACTION'): Buffer {
  return Buffer.from(JSON.stringify({
    schema: 'fachtracing-decision-record/v1', recordId: 'self-record', executionId: 'self-execution',
    graphId: 'self-graph', graphVersion: 1, startedAt: '2026-08-19T10:00:00Z',
    completedAt: '2026-08-19T10:00:01Z', status: 'SUCCEEDED', terminalStatus: 'SUCCEEDED',
    completeness: 'COMPLETE', finalDecision: { type: 'category', canonicalValue: result, displayValue: result },
    failure: null, observations: [{ sequence: 1, nodeId: 'entry', outcome: 'entered', evidence: {}, selectedEdgeId: null }],
    coverageGaps: [], correlationKeys: {
      application: { type: 'string', canonicalValue: 'fachtracing', displayValue: 'Fachtracing' }
    }, redactionPolicyId: 'self-dogfood-v1'
  }));
}

class FakePool {
  payload: Buffer | null = null;
  correlations = 0;
  readonly client = {
    query: async (sql: string, values?: unknown[]) => {
      if (sql.startsWith('select 1 from fachtracing_graph')) return { rows: [{ '?column?': 1 }], rowCount: 1 };
      if (sql.startsWith('select payload')) return { rows: this.payload ? [{ payload: this.payload }] : [], rowCount: this.payload ? 1 : 0 };
      if (sql.startsWith('insert into fachtracing_decision_record')) this.payload = values?.[8] as Buffer;
      if (sql.startsWith('insert into fachtracing_correlation')) this.correlations += 1;
      return { rows: [], rowCount: 0 };
    },
    release: () => undefined
  };
  async connect() { return this.client; }
}

describe('run import service', () => {
  it('imports generated correlations idempotently and rejects changed bytes', async () => {
    const pool = new FakePool();
    const service = new RunImportRepository(pool as unknown as Pool);
    const original = bytes();
    await expect(service.importRuns([{ path: 'self.decision.json', bytes: original }])).resolves.toBe(1);
    await expect(service.importRuns([{ path: 'self.decision.json', bytes: original }])).resolves.toBe(1);
    expect(pool.correlations).toBe(1);
    await expect(service.importRuns([{ path: 'changed.decision.json', bytes: bytes('TECHNICAL_PREDICATE') }]))
      .rejects.toThrow(/contract conflict/);
  });
});
