import type { Pool, PoolClient } from 'pg';
import { parseDecisionRecordJson, type RunModel } from '$contracts/run-contract';
import { database } from './database.server';
import type { RunImport } from './run-import.server';

function sameBytes(left: Uint8Array, right: Uint8Array): boolean {
  return left.byteLength === right.byteLength && left.every((value, index) => value === right[index]);
}

async function requireGraph(client: PoolClient, run: RunModel): Promise<void> {
  const result = await client.query(
    'select 1 from fachtracing_graph where graph_id = $1 and graph_version = $2',
    [run.graphId, run.graphVersion]
  );
  if (result.rowCount !== 1) throw new Error(`run ${run.executionId} references a graph that is not imported`);
}

async function insertRun(client: PoolClient, run: RunModel, bytes: Buffer): Promise<void> {
  await client.query(`insert into fachtracing_decision_record
    (record_id, execution_id, graph_id, graph_version, started_at, completed_at, status, schema_id, payload)
    values ($1, $2, $3, $4, $5, $6, $7, $8, $9)`, [run.recordId, run.executionId,
    run.graphId, run.graphVersion, run.startedAt, run.completedAt, run.status, run.schema, bytes]);
  for (const [name, item] of Object.entries(run.correlationKeys).sort(([left], [right]) => left.localeCompare(right))) {
    await client.query(`insert into fachtracing_correlation
      (record_id, correlation_name, correlation_value, completed_at) values ($1, $2, $3, $4)`,
      [run.recordId, name, item.canonicalValue, run.completedAt]);
  }
}

export class RunImportRepository {
  constructor(private readonly pool: Pool = database()) {}

  async importRuns(items: readonly RunImport[]): Promise<number> {
    for (const item of items) await this.importRun(item);
    return items.length;
  }

  private async importRun(item: RunImport): Promise<void> {
    const run = parseDecisionRecordJson(item.bytes);
    const client = await this.pool.connect();
    try {
      await client.query('begin');
      await requireGraph(client, run);
      const existing = await client.query<{ payload: Buffer }>(
        'select payload from fachtracing_decision_record where record_id = $1 or execution_id = $2',
        [run.recordId, run.executionId]
      );
      if (existing.rows.length > 0) {
        if (!sameBytes(existing.rows[0].payload, item.bytes)) throw new Error(`run contract conflict: ${item.path}`);
      } else {
        await insertRun(client, run, item.bytes);
      }
      await client.query('commit');
    } catch (error) {
      await client.query('rollback');
      throw error;
    } finally {
      client.release();
    }
  }
}
