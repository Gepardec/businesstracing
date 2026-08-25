import ELK from 'elkjs/lib/elk-api.js';
import ElkWorker from 'elkjs/lib/elk-worker.min.js?worker';
import type { GraphModel } from '$contracts/graph-contract';
import { computeLayoutWith, LOCAL_LAYOUT_SPACING, type LayoutResult } from './layout-definition';

let elk: InstanceType<typeof ELK> | undefined;

export function layoutGraph(graph: GraphModel, purpose: 'complete' | 'local' = 'complete'): Promise<LayoutResult> {
  elk ??= new ELK({ workerFactory: () => new ElkWorker() });
  return computeLayoutWith(elk, graph, purpose === 'local' ? LOCAL_LAYOUT_SPACING : undefined);
}
