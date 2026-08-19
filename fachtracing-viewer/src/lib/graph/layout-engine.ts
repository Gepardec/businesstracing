import ELK from 'elkjs/lib/elk.bundled.js';
import type { GraphModel } from '$contracts/graph-contract';
import { computeLayoutWith, type LayoutResult } from './layout-definition';

export { NODE_HEIGHT, NODE_WIDTH, type LayoutResult, type PositionedNode, type RoutedEdge } from './layout-definition';

export async function computeLayout(graph: GraphModel): Promise<LayoutResult> {
  return computeLayoutWith(new ELK(), graph);
}
