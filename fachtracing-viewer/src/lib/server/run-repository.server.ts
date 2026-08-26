import type { Pool } from 'pg';
import { parseDecisionRecordJson, type RunModel } from '$contracts/run-contract';
import { parseDeveloperGraphJson } from '$contracts/graph-contract';
import type { RunPage, RunSearch, RunSummary } from '$contracts/run-search';
import { database } from './database.server';

const MAX_LIMIT = 50;
const MAX_TEXT = 500;

interface Cursor { completedAt: string; executionId: string }
interface RunRow {
  execution_id: string; graph_id: string; graph_version: string; completed_at: Date;
  status: RunModel['status']; payload: Buffer; graph_payload: Buffer | null;
}

function bounded(value: unknown, name: string, max = MAX_TEXT): string {
  if (typeof value !== 'string' || value.trim() === '' || value.length > max) throw new Error(`${name} is invalid`);
  return value;
}

function dateTime(value: unknown, name: string): string {
  const result = bounded(value, name, 64);
  if (Number.isNaN(Date.parse(result))) throw new Error(`${name} is invalid`);
  return result;
}

function decodeCursor(raw: string): Cursor {
  try {
    const value = JSON.parse(Buffer.from(bounded(raw, 'cursor', 1000), 'base64url').toString('utf8')) as Cursor;
    return { completedAt: dateTime(value.completedAt, 'cursor.completedAt'), executionId: bounded(value.executionId, 'cursor.executionId', 200) };
  } catch {
    throw new Error('cursor is invalid');
  }
}

function encodeCursor(value: Cursor): string {
  return Buffer.from(JSON.stringify(value), 'utf8').toString('base64url');
}

export function parseRunSearch(input: unknown): RunSearch {
  if (input === null || typeof input !== 'object' || Array.isArray(input)) throw new Error('search document must be an object');
  const raw = input as Record<string, unknown>;
  const allowed = new Set(['executionId', 'graphId', 'status', 'completedFrom', 'completedTo', 'correlation', 'cursor', 'limit']);
  for (const key of Object.keys(raw)) if (!allowed.has(key)) throw new Error(`unknown search field: ${key}`);
  const result: RunSearch = {};
  if (raw.executionId !== undefined) result.executionId = bounded(raw.executionId, 'executionId', 200);
  if (raw.graphId !== undefined) result.graphId = bounded(raw.graphId, 'graphId', 200);
  if (raw.status !== undefined) {
    const status = bounded(raw.status, 'status', 20);
    if (!['SUCCEEDED', 'FAILED', 'INCOMPLETE'].includes(status)) throw new Error('status is invalid');
    result.status = status as RunSearch['status'];
  }
  if (raw.completedFrom !== undefined) result.completedFrom = dateTime(raw.completedFrom, 'completedFrom');
  if (raw.completedTo !== undefined) result.completedTo = dateTime(raw.completedTo, 'completedTo');
  if (raw.correlation !== undefined) {
    if (raw.correlation === null || typeof raw.correlation !== 'object' || Array.isArray(raw.correlation)) throw new Error('correlation is invalid');
    const correlation = raw.correlation as Record<string, unknown>;
    if (Object.keys(correlation).some((key) => key !== 'name' && key !== 'value')) throw new Error('correlation is invalid');
    result.correlation = { name: bounded(correlation.name, 'correlation.name', 200), value: bounded(correlation.value, 'correlation.value', 500) };
  }
  if (raw.cursor !== undefined) { bounded(raw.cursor, 'cursor', 1000); result.cursor = String(raw.cursor); }
  if (raw.limit !== undefined) {
    if (!Number.isInteger(raw.limit) || Number(raw.limit) < 1 || Number(raw.limit) > MAX_LIMIT) throw new Error('limit is invalid');
    result.limit = Number(raw.limit);
  }
  return result;
}

export class RunRepository {
  constructor(private readonly pool: Pool = database()) {}

  async correlationNames(): Promise<string[]> {
    const result = await this.pool.query<{ correlation_name: string }>(
      'select distinct correlation_name from fachtracing_correlation order by correlation_name limit 200'
    );
    return result.rows.map((row) => row.correlation_name);
  }

  async getRun(executionId: string): Promise<Buffer | null> {
    const result = await this.pool.query<{ payload: Buffer }>(
      'select payload from fachtracing_decision_record where execution_id = $1', [bounded(executionId, 'executionId', 200)]
    );
    return result.rows[0]?.payload ?? null;
  }

  async searchRuns(raw: RunSearch): Promise<RunPage> {
    const query = parseRunSearch(raw);
    const values: unknown[] = [];
    const where: string[] = [];
    const bind = (value: unknown): string => { values.push(value); return `$${values.length}`; };
    let correlationJoin = '';
    if (query.correlation) {
      correlationJoin = 'join fachtracing_correlation c on c.record_id = r.record_id';
      where.push(`c.correlation_name = ${bind(query.correlation.name)}`);
      where.push(`c.correlation_value = ${bind(query.correlation.value)}`);
    }
    if (query.executionId) where.push(`r.execution_id = ${bind(query.executionId)}`);
    if (query.graphId) where.push(`r.graph_id = ${bind(query.graphId)}`);
    if (query.status) where.push(`r.status = ${bind(query.status)}`);
    if (query.completedFrom) where.push(`r.completed_at >= ${bind(query.completedFrom)}`);
    if (query.completedTo) where.push(`r.completed_at <= ${bind(query.completedTo)}`);
    if (query.cursor) {
      const cursor = decodeCursor(query.cursor);
      const completed = bind(cursor.completedAt);
      const execution = bind(cursor.executionId);
      where.push(`(r.completed_at < ${completed} or (r.completed_at = ${completed} and r.execution_id < ${execution}))`);
    }
    const limit = query.limit ?? MAX_LIMIT;
    values.push(limit + 1);
    const result = await this.pool.query<RunRow>(`select r.execution_id, r.graph_id, r.graph_version,
      r.completed_at, r.status, r.payload, g.payload as graph_payload
      from fachtracing_decision_record r ${correlationJoin}
      left join fachtracing_graph g on g.graph_id = r.graph_id and g.graph_version = r.graph_version
      ${where.length ? `where ${where.join(' and ')}` : ''}
      order by r.completed_at desc, r.execution_id desc limit $${values.length}`, values);
    const hasMore = result.rows.length > limit;
    const rows = result.rows.slice(0, limit);
    const items = rows.map((row): RunSummary => {
      const run = parseDecisionRecordJson(row.payload);
      const graph = row.graph_payload ? parseDeveloperGraphJson(row.graph_payload) : null;
      return {
        executionId: row.execution_id,
        graphId: row.graph_id,
        graphVersion: Number(row.graph_version),
        decisionLabel: graph?.label ?? row.graph_id,
        completedAt: row.completed_at.toISOString(),
        status: row.status,
        finalResult: run.finalDecision?.displayValue ?? run.failure?.displayValue ?? null
      };
    });
    const last = rows.at(-1);
    return {
      items,
      nextCursor: hasMore && last ? encodeCursor({ completedAt: last.completed_at.toISOString(), executionId: last.execution_id }) : null
    };
  }
}
