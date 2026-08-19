import type { RequestHandler } from './$types';
import { RunRepository } from '$lib/server/run-repository.server';
import { noStoreJson, problem } from '$lib/server/problem.server';

export const GET: RequestHandler = async () => {
  try {
    return noStoreJson({ items: await new RunRepository().correlationNames() });
  } catch {
    return problem(503, 'Decision search is unavailable');
  }
};
