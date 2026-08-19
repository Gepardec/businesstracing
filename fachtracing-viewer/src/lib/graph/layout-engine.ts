import ELK from 'elkjs/lib/elk.bundled.js';
import type { ElkNode } from 'elkjs/lib/elk-api';
import type { GraphModel } from '$contracts/graph-contract';

export const NODE_WIDTH = 232;
export const NODE_HEIGHT = 92;

export interface PositionedNode { id: string; x: number; y: number }
export interface LayoutResult { nodes: PositionedNode[]; width: number; height: number }

export async function computeLayout(graph: GraphModel): Promise<LayoutResult> {
  const elk = new ELK();
  const result = await elk.layout({
    id: 'root',
    layoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'DOWN',
      'elk.edgeRouting': 'ORTHOGONAL',
      'elk.spacing.nodeNode': '42',
      'elk.layered.spacing.nodeNodeBetweenLayers': '78',
      'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
      'elk.padding': '[top=32,left=32,bottom=32,right=32]'
    },
    children: [...graph.nodes].sort((a, b) => a.id.localeCompare(b.id)).map((node) => ({
      id: node.id, width: NODE_WIDTH, height: NODE_HEIGHT
    })),
    edges: [...graph.edges].sort((a, b) => a.id.localeCompare(b.id)).map((edge) => ({
      id: edge.id, sources: [edge.from], targets: [edge.to]
    }))
  }) as ElkNode;
  const positions = (result.children ?? []).map((node) => ({ id: node.id, x: node.x ?? 0, y: node.y ?? 0 }));
  return { nodes: positions, width: result.width ?? 0, height: result.height ?? 0 };
}
