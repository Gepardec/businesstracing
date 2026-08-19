import type { ELK, ElkEdgeSection, ElkExtendedEdge, ElkNode } from 'elkjs/lib/elk-api';
import type { GraphModel } from '$contracts/graph-contract';
import { routeLabelPosition, type LayoutPoint } from './edge-route';

export const NODE_WIDTH = 232;
export const NODE_HEIGHT = 92;

export interface PositionedNode { id: string; x: number; y: number }
export interface RoutedEdge { id: string; points: LayoutPoint[]; labelPosition: LayoutPoint }
export interface LayoutResult { nodes: PositionedNode[]; edges: RoutedEdge[]; width: number; height: number }

function sectionPoints(section: ElkEdgeSection): LayoutPoint[] {
  return [section.startPoint, ...(section.bendPoints ?? []), section.endPoint].map(({ x, y }) => ({ x, y }));
}

function edgePoints(edge: ElkExtendedEdge): LayoutPoint[] {
  const sections = edge.sections ?? [];
  if (sections.length === 0) throw new Error(`ELK did not return a route for edge ${edge.id}.`);
  const byId = new Map(sections.map((section) => [section.id, section]));
  let section: ElkEdgeSection | undefined = sections.find((candidate) => !candidate.incomingSections?.length) ?? sections[0];
  const visited = new Set<string>();
  const points: LayoutPoint[] = [];
  while (section && !visited.has(section.id)) {
    visited.add(section.id);
    for (const point of sectionPoints(section)) {
      const previous = points.at(-1);
      if (!previous || previous.x !== point.x || previous.y !== point.y) points.push(point);
    }
    const nextId: string | undefined = section.outgoingSections?.[0];
    section = nextId ? byId.get(nextId) : undefined;
  }
  if (points.length < 2) throw new Error(`ELK returned an incomplete route for edge ${edge.id}.`);
  return points;
}

export async function computeLayoutWith(elk: Pick<ELK, 'layout'>, graph: GraphModel): Promise<LayoutResult> {
  const result = await elk.layout({
    id: 'root',
    layoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'DOWN',
      'elk.edgeRouting': 'ORTHOGONAL',
      'elk.spacing.nodeNode': '42',
      'elk.spacing.edgeEdge': '16',
      'elk.layered.spacing.edgeEdgeBetweenLayers': '18',
      'elk.layered.spacing.nodeNodeBetweenLayers': '96',
      'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
      'elk.layered.mergeEdges': 'false',
      'elk.padding': '[top=32,left=32,bottom=32,right=32]'
    },
    children: [...graph.nodes].sort((a, b) => a.id.localeCompare(b.id)).map((node) => {
      const incoming = graph.edges.filter((edge) => edge.to === node.id).sort((a, b) => a.id.localeCompare(b.id));
      const outgoing = graph.edges.filter((edge) => edge.from === node.id).sort((a, b) => a.id.localeCompare(b.id));
      return {
        id: node.id,
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
        layoutOptions: { 'elk.portConstraints': 'FIXED_ORDER' },
        ports: [
          ...incoming.map((edge) => ({ id: `${node.id}::in::${edge.id}`, width: 1, height: 1, layoutOptions: { 'elk.port.side': 'NORTH' } })),
          ...outgoing.map((edge) => ({ id: `${node.id}::out::${edge.id}`, width: 1, height: 1, layoutOptions: { 'elk.port.side': 'SOUTH' } }))
        ]
      };
    }),
    edges: [...graph.edges].sort((a, b) => a.id.localeCompare(b.id)).map((edge) => ({
      id: edge.id, sources: [`${edge.from}::out::${edge.id}`], targets: [`${edge.to}::in::${edge.id}`]
    }))
  }) as ElkNode;
  const positions = (result.children ?? []).map((node) => ({ id: node.id, x: node.x ?? 0, y: node.y ?? 0 }));
  const positionById = new Map(positions.map((node) => [node.id, node]));
  const siblingGroups = new Map<string, string[]>();
  for (const edge of [...graph.edges].sort((a, b) => a.id.localeCompare(b.id))) {
    const key = `${edge.from}\u0000${edge.to}`;
    siblingGroups.set(key, [...(siblingGroups.get(key) ?? []), edge.id]);
  }
  const graphEdges = new Map(graph.edges.map((edge) => [edge.id, edge]));
  const routes = (result.edges ?? []).map((edge): RoutedEdge => {
    const modelEdge = graphEdges.get(edge.id);
    if (!modelEdge) throw new Error(`ELK returned an unknown edge ${edge.id}.`);
    const points = edgePoints(edge);
    const siblings = siblingGroups.get(`${modelEdge.from}\u0000${modelEdge.to}`) ?? [edge.id];
    const siblingIndex = siblings.indexOf(edge.id);
    const labelFraction = siblings.length === 1 ? 0.5 : 0.24 + (0.52 * siblingIndex) / (siblings.length - 1);
    const source = positionById.get(modelEdge.from)!;
    const target = positionById.get(modelEdge.to)!;
    const flowCenterX = (source.x + target.x + NODE_WIDTH) / 2;
    return { id: edge.id, points, labelPosition: routeLabelPosition(points, labelFraction, flowCenterX) };
  });
  if (routes.length !== graph.edges.length) throw new Error('ELK did not return every graph edge.');
  return { nodes: positions, edges: routes, width: result.width ?? 0, height: result.height ?? 0 };
}
