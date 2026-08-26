import type { RequestHandler } from './$types';
import { RunRepository } from '$lib/server/run-repository.server';
import { problem } from '$lib/server/problem.server';

export const GET: RequestHandler = async ({ params }) => {
  if (!params.executionId || params.executionId.length > 200) return problem(400, 'Execution ID is invalid');
  try {
    const payload = await new RunRepository().getRun(params.executionId);
    return payload ? new Response(new Uint8Array(payload), {
      headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' }
    }) : problem(404, 'Decision record was not found');
  } catch {
    return problem(503, 'Decision record is unavailable');
  }
};
