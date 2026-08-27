import { createHash } from 'node:crypto';
import type { Pool, PoolClient } from 'pg';
import { DEVELOPER_GRAPH_SCHEMA, parseDeveloperGraphJson, type GraphModel } from '$contracts/graph-contract';
import { database } from './database.server';

export const GRAPH_MEDIA_TYPE = 'application/vnd.fachtracing.developer-graph+json;version=1';

export interface GraphSummary {
  id: string;
  version: number;
  label: string;
  completeness: GraphModel['completeness'];
  importedAt: string;
}

export interface GraphImport {
  path: string;
  bytes: Uint8Array;
}

interface GraphRow {
  graph_id: string;
  graph_version: string;
  schema_id: string;
  media_type: string;
  payload: Buffer;
  sha256: string;
  imported_at: Date;
}

function checksum(bytes: Uint8Array): string {
  return createHash('sha256').update(bytes).digest('hex');
}

function verifiedGraph(row: GraphRow): GraphModel {
  if (row.schema_id !== DEVELOPER_GRAPH_SCHEMA || row.media_type !== GRAPH_MEDIA_TYPE) throw new Error('unsupported graph contract');
  if (checksum(row.payload) !== row.sha256) throw new Error('graph checksum does not match');
  const graph = parseDeveloperGraphJson(row.payload);
  if (graph.id !== row.graph_id || graph.version !== Number(row.graph_version)) throw new Error('graph identity does not match');
  return graph;
}

export class GraphCatalogRepository {
  constructor(private readonly pool: Pool = database()) {}

  async listGraphs(): Promise<GraphSummary[]> {
    const result = await this.pool.query<GraphRow>(`select graph_id, graph_version, schema_id, media_type,
      payload, sha256, imported_at from fachtracing_graph order by graph_id, graph_version desc limit 500`);
    return result.rows.map((row) => {
      const graph = verifiedGraph(row);
      return { id: graph.id, version: graph.version, label: graph.label, completeness: graph.completeness, importedAt: row.imported_at.toISOString() };
    });
  }

  async getGraph(graphId: string, graphVersion: number): Promise<GraphModel | null> {
    const result = await this.pool.query<GraphRow>(`select graph_id, graph_version, schema_id, media_type,
      payload, sha256, imported_at from fachtracing_graph where graph_id = $1 and graph_version = $2`, [graphId, graphVersion]);
    return result.rows[0] ? verifiedGraph(result.rows[0]) : null;
  }

  async importGraphs(items: readonly GraphImport[]): Promise<number> {
    const prepared = items.map((item) => {
      const graph = parseDeveloperGraphJson(item.bytes);
      return { ...item, graph, sha256: checksum(item.bytes) };
    });
    const client = await this.pool.connect();
    try {
      await client.query('begin');
      for (const item of prepared) await this.importOne(client, item);
      await client.query('commit');
      return prepared.length;
    } catch (error) {
      await client.query('rollback');
      throw error;
    } finally {
      client.release();
    }
  }

  private async importOne(client: PoolClient, item: GraphImport & { graph: GraphModel; sha256: string }): Promise<void> {
    const existing = await client.query<{ sha256: string }>(
      'select sha256 from fachtracing_graph where graph_id = $1 and graph_version = $2 for update',
      [item.graph.id, item.graph.version]
    );
    if (existing.rows[0]) {
      if (existing.rows[0].sha256 !== item.sha256) throw new Error(`graph contract conflict: ${item.graph.id}@${item.graph.version}`);
      return;
    }
    await client.query(`insert into fachtracing_graph
      (graph_id, graph_version, schema_id, media_type, payload, sha256, imported_at)
      values ($1, $2, $3, $4, $5, $6, current_timestamp)`,
      [item.graph.id, item.graph.version, DEVELOPER_GRAPH_SCHEMA, GRAPH_MEDIA_TYPE, Buffer.from(item.bytes), item.sha256]);
  }
}
