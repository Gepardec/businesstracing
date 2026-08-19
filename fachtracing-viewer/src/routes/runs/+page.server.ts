import type { PageServerLoad } from './$types';
import { RunRepository } from '$lib/server/run-repository.server';

export const load: PageServerLoad = async () => {
  try {
    const repository = new RunRepository();
    const [page, correlationNames] = await Promise.all([repository.searchRuns({}), repository.correlationNames()]);
    return { page, correlationNames, availabilityError: null };
  } catch {
    return { page: { items: [], nextCursor: null }, correlationNames: [], availabilityError: 'The decision database is unavailable.' };
  }
};
