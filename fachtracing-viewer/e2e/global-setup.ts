import { createHash } from 'node:crypto';
import { join } from 'node:path';
import pg from 'pg';
import { GraphCatalogRepository } from '../src/lib/server/graph-catalog-repository.server';
import { readGraphDirectory } from '../src/lib/server/graph-import.server';
import { RunImportRepository } from '../src/lib/server/run-import-repository.server';
import { readRunDirectory } from '../src/lib/server/run-import.server';

const graph = {
  schema: 'fachtracing-developer-graph/v1',
  graph: {
    id: 'e2e-route-graph', version: 1, label: 'choose a delivery route', entryNodeId: 'entry', completeness: 'COMPLETE',
    nodes: [
      { id: 'entry', kind: 'ENTRY', label: 'route request received', attributes: {} },
      { id: 'local', kind: 'PREDICATE', label: 'route is local', attributes: {} },
      { id: 'selected', kind: 'OUTCOME', label: 'local route selected', attributes: {} }
    ],
    edges: [
      { id: 'to-check', from: 'entry', to: 'local', outcome: 'next' },
      { id: 'to-result', from: 'local', to: 'selected', outcome: 'yes' }
    ], coverageGaps: []
  },
  sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'playwright', checksum: 'fixture' }], sourceFiles: []
};

const run = {
  schema: 'fachtracing-decision-record/v1', recordId: 'e2e-record', executionId: 'e2e-execution',
  graphId: graph.graph.id, graphVersion: 1, boundaryFingerprint: 'fixture',
  startedAt: '2026-08-19T10:00:00Z', completedAt: '2026-08-19T10:00:01Z', status: 'SUCCEEDED',
  terminalStatus: 'COMPLETED', completeness: 'COMPLETE',
  finalDecision: { type: 'string', canonicalValue: 'local', displayValue: 'Local route' }, failure: null,
  observations: [
    { sequence: 1, nodeId: 'entry', outcome: 'entered', evidence: {}, selectedEdgeId: 'to-check' },
    { sequence: 2, nodeId: 'local', outcome: 'true', evidence: { route: { type: 'string', canonicalValue: 'route-17', displayValue: 'Route 17' } }, selectedEdgeId: 'to-result' },
    { sequence: 3, nodeId: 'selected', outcome: 'completed', evidence: {}, selectedEdgeId: null }
  ], coverageGaps: [], correlationKeys: { routeId: { type: 'string', canonicalValue: 'route-17', displayValue: 'Route 17' } }, redactionPolicyId: 'e2e'
};

export default async function setup(): Promise<void> {
  const connectionString = process.env.FACHTRACING_DATABASE_URL;
  if (!connectionString) return;
  const pool = new pg.Pool({ connectionString, max: 2 });
  try {
    const graphBytes = Buffer.from(`${JSON.stringify(graph)}\n`);
    const runBytes = Buffer.from(`${JSON.stringify(run)}\n`);
    await pool.query('delete from fachtracing_decision_record where record_id = $1', [run.recordId]);
    await pool.query('delete from fachtracing_graph where graph_id = $1 and graph_version = $2', [graph.graph.id, graph.graph.version]);
    await pool.query(`insert into fachtracing_graph
      (graph_id, graph_version, schema_id, media_type, payload, sha256, imported_at)
      values ($1, $2, $3, $4, $5, $6, current_timestamp)`, [graph.graph.id, 1, graph.schema,
      'application/vnd.fachtracing.developer-graph+json;version=1', graphBytes, createHash('sha256').update(graphBytes).digest('hex')]);
    await pool.query(`insert into fachtracing_decision_record
      (record_id, execution_id, graph_id, graph_version, started_at, completed_at, status, schema_id, payload)
      values ($1, $2, $3, $4, $5, $6, $7, $8, $9)`, [run.recordId, run.executionId, run.graphId, run.graphVersion,
      run.startedAt, run.completedAt, run.status, run.schema, runBytes]);
    await pool.query(`insert into fachtracing_correlation
      (record_id, correlation_name, correlation_value, completed_at) values ($1, $2, $3, $4)`,
      [run.recordId, 'routeId', 'route-17', run.completedAt]);
    const dogfood = process.env.FACHTRACING_DOGFOOD_DIRECTORY;
    if (dogfood) {
      await new GraphCatalogRepository(pool).importGraphs(await readGraphDirectory(join(dogfood, 'graphs')));
      await new RunImportRepository(pool).importRuns(await readRunDirectory(join(dogfood, 'runs')));
    }
  } finally { await pool.end(); }
}
