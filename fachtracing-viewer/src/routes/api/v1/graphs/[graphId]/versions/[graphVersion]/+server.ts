import type { RequestHandler } from './$types';
import { GraphCatalogRepository } from '$lib/server/graph-catalog-repository.server';
import { noStoreJson, problem } from '$lib/server/problem.server';

export const GET: RequestHandler = async ({ params }) => {
  if (params.graphId.length > 200 || !/^\d+$/.test(params.graphVersion)) return problem(400, 'Graph identity is invalid');
  const version = Number(params.graphVersion);
  if (!Number.isSafeInteger(version) || version < 1) return problem(400, 'Graph identity is invalid');
  try {
    const graph = await new GraphCatalogRepository().getGraph(params.graphId, version);
    return graph ? noStoreJson(graph) : problem(404, 'Graph version was not found');
  } catch {
    return problem(503, 'Graph catalog is unavailable');
  }
};
