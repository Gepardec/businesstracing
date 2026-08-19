import ELK from 'elkjs/lib/elk-api.js';
import ElkWorker from 'elkjs/lib/elk-worker.min.js?worker';
import type { GraphModel } from '$contracts/graph-contract';
import { computeLayoutWith, type LayoutResult } from './layout-definition';

const elk = new ELK({ workerFactory: () => new ElkWorker() });

export function layoutGraph(graph: GraphModel): Promise<LayoutResult> {
  return computeLayoutWith(elk, graph);
}
