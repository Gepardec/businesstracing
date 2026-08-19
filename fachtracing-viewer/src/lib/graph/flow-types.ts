import type { Edge, Node } from '@xyflow/svelte';
import type { GraphNode } from '$contracts/graph-contract';
import type { LayoutPoint } from './edge-route';

export interface BusinessNodeData extends Record<string, unknown> {
  node: GraphNode;
  onPath: boolean;
  current: boolean;
  dimmed: boolean;
  stepNumber: number | null;
}

export interface BusinessEdgeData extends Record<string, unknown> {
  route: readonly LayoutPoint[];
  labelPosition: LayoutPoint;
  onPath: boolean;
  current: boolean;
  showLabel: boolean;
}

export type BusinessFlowNode = Node<BusinessNodeData, 'business'>;
export type BusinessFlowEdge = Edge<BusinessEdgeData, 'business'>;
