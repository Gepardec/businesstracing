import type { Edge, Node } from '@xyflow/svelte';
import type { GraphNode } from '$contracts/graph-contract';
import type { LayoutPoint } from './edge-route';
import type { DuplicateOccurrence } from './topology-analysis';
import type { LayoutPort } from './route-planner';

export interface BusinessNodeData extends Record<string, unknown> {
  node: GraphNode;
  memberNodeIds: readonly string[];
  memberLabels: readonly string[];
  onPath: boolean;
  current: boolean;
  dimmed: boolean;
  stepNumber: number | null;
  ports: readonly LayoutPort[];
  layoutPosition: LayoutPoint;
  occurrence: DuplicateOccurrence | null;
  incomingCount: number;
  outgoingCount: number;
}

export interface BusinessEdgeData extends Record<string, unknown> {
  memberEdgeIds: readonly string[];
  route: readonly LayoutPoint[];
  labelPosition: LayoutPoint;
  labelAnchor: LayoutPoint;
  onPath: boolean;
  current: boolean;
  inspected: boolean;
  rawOutcome: string;
  long: boolean;
  secondary: boolean;
  feedback: boolean;
  branch: boolean;
  sharedSegmentIds: readonly string[];
}

export type BusinessFlowNode = Node<BusinessNodeData, 'business'>;
export type BusinessFlowEdge = Edge<BusinessEdgeData, 'business'>;
