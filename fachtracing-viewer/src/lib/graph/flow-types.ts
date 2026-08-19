import type { Edge, Node } from '@xyflow/svelte';
import type { GraphNode } from '$contracts/graph-contract';

export interface BusinessNodeData extends Record<string, unknown> {
  node: GraphNode;
  onPath: boolean;
  current: boolean;
  dimmed: boolean;
  sequence: number | null;
}

export interface BusinessEdgeData extends Record<string, unknown> {
  onPath: boolean;
  current: boolean;
}

export type BusinessFlowNode = Node<BusinessNodeData, 'business'>;
export type BusinessFlowEdge = Edge<BusinessEdgeData, 'business'>;
