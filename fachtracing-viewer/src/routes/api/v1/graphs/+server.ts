import type { RequestHandler } from './$types';
import { GraphCatalogRepository } from '$lib/server/graph-catalog-repository.server';
import { noStoreJson, problem } from '$lib/server/problem.server';

export const GET: RequestHandler = async () => {
  try {
    return noStoreJson({ items: await new GraphCatalogRepository().listGraphs() });
  } catch {
    return problem(503, 'Graph catalog is unavailable');
  }
};
