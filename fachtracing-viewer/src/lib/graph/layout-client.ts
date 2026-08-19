import type { GraphModel } from '$contracts/graph-contract';
import type { LayoutResult } from './layout-engine';

let token = 0;
let worker: Worker | undefined;

export function layoutGraph(graph: GraphModel): Promise<LayoutResult> {
  worker ??= new Worker(new URL('./layout-worker.ts', import.meta.url), { type: 'module' });
  const requestToken = ++token;
  return new Promise((resolve, reject) => {
    const receive = (event: MessageEvent<{ token: number; layout?: LayoutResult; error?: string }>) => {
      if (event.data.token !== requestToken) return;
      worker?.removeEventListener('message', receive);
      if (event.data.layout) resolve(event.data.layout);
      else reject(new Error(event.data.error ?? 'The graph layout could not be created.'));
    };
    worker?.addEventListener('message', receive);
    worker?.postMessage({ token: requestToken, graph });
  });
}
