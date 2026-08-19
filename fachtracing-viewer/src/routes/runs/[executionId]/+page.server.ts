import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { assertRunMatchesGraph, parseDecisionRecordJson } from '$contracts/run-contract';
import { GraphCatalogRepository } from '$lib/server/graph-catalog-repository.server';
import { RunRepository } from '$lib/server/run-repository.server';

export const load: PageServerLoad = async ({ params }) => {
  if (!params.executionId || params.executionId.length > 200) error(400, 'Execution ID is invalid');
  let payload: Buffer | null;
  try { payload = await new RunRepository().getRun(params.executionId); }
  catch { error(503, 'Decision record is unavailable'); }
  if (!payload) error(404, 'Decision record was not found');
  const run = parseDecisionRecordJson(payload);
  let graph;
  try { graph = await new GraphCatalogRepository().getGraph(run.graphId, run.graphVersion); }
  catch { error(503, 'Graph catalog is unavailable'); }
  if (!graph) error(409, `Graph ${run.graphId}@${run.graphVersion} has not been imported`);
  try { assertRunMatchesGraph(run, graph); }
  catch { error(409, 'The decision record does not match its graph version'); }
  return { run, graph };
};
