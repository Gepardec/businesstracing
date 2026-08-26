import type { RequestHandler } from './$types';
import { RunRepository, parseRunSearch } from '$lib/server/run-repository.server';
import { noStoreJson, problem } from '$lib/server/problem.server';

const MAX_QUERY_BYTES = 16_384;

export const fallback: RequestHandler = async ({ request }) => {
  if (request.method !== 'QUERY') return problem(405, 'Method is not allowed');
  if (request.headers.get('content-type')?.split(';')[0].trim().toLowerCase() !== 'application/json') {
    return problem(415, 'Content type must be application/json');
  }
  const declared = Number(request.headers.get('content-length') ?? 0);
  if (declared > MAX_QUERY_BYTES) return problem(413, 'Search document is too large');
  try {
    const body = await request.text();
    if (Buffer.byteLength(body, 'utf8') > MAX_QUERY_BYTES) return problem(413, 'Search document is too large');
    const query = parseRunSearch(JSON.parse(body));
    return noStoreJson(await new RunRepository().searchRuns(query), {
      headers: { 'accept-query': 'application/json' }
    });
  } catch (error) {
    if (error instanceof SyntaxError || error instanceof Error && /invalid|unknown|must be/.test(error.message)) {
      return problem(400, 'Search document is invalid');
    }
    return problem(503, 'Decision search is unavailable');
  }
};
