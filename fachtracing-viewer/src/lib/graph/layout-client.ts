import ELK from 'elkjs/lib/elk-api.js';
import ElkWorker from 'elkjs/lib/elk-worker.min.js?worker';
import type { GraphModel } from '$contracts/graph-contract';
import { computeLayoutWith, type LayoutResult } from './layout-definition';

let elk: InstanceType<typeof ELK> | undefined;

export function layoutGraph(graph: GraphModel): Promise<LayoutResult> {
  elk ??= new ELK({ workerFactory: () => new ElkWorker() });
  return computeLayoutWith(elk, graph);
}
