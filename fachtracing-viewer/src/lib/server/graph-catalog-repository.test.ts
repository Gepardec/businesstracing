import { describe, expect, it } from 'vitest';
import type { Pool } from 'pg';
import { GraphCatalogRepository } from './graph-catalog-repository.server';

function bytes(label: string): Uint8Array {
  return Buffer.from(JSON.stringify({
    schema: 'fachtracing-developer-graph/v1',
    graph: {
      id: 'generated-graph', version: 1, label, entryNodeId: 'entry', completeness: 'COMPLETE',
      nodes: [{ id: 'entry', kind: 'ENTRY', label: 'start', attributes: {} }], edges: [], coverageGaps: []
    },
    sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'test', checksum: 'fixture' }], sourceFiles: []
  }));
}

class FakePool {
  sha256: string | null = null;
  readonly client = {
    query: async (sql: string, values?: unknown[]) => {
      if (sql.startsWith('select sha256')) return { rows: this.sha256 ? [{ sha256: this.sha256 }] : [] };
      if (sql.startsWith('insert into fachtracing_graph')) { this.sha256 = String(values?.[5]); return { rows: [] }; }
      return { rows: [] };
    },
    release: () => undefined
  };
  async connect() { return this.client; }
}

describe('immutable graph catalog', () => {
  it('accepts identical bytes and rejects a version conflict', async () => {
    const pool = new FakePool();
    const repository = new GraphCatalogRepository(pool as unknown as Pool);
    await expect(repository.importGraphs([{ path: 'first.json', bytes: bytes('route decision') }])).resolves.toBe(1);
    await expect(repository.importGraphs([{ path: 'same.json', bytes: bytes('route decision') }])).resolves.toBe(1);
    await expect(repository.importGraphs([{ path: 'conflict.json', bytes: bytes('changed decision') }])).rejects.toThrow(/contract conflict/);
  });
});
