import { computeLayout } from './layout-engine';
import type { GraphModel } from '$contracts/graph-contract';

self.onmessage = async (event: MessageEvent<{ token: number; graph: GraphModel }>) => {
  try {
    const layout = await computeLayout(event.data.graph);
    self.postMessage({ token: event.data.token, layout });
  } catch {
    self.postMessage({ token: event.data.token, error: 'The graph layout could not be created.' });
  }
};
