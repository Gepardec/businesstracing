export const DEVELOPER_GRAPH_SCHEMA = 'fachtracing-developer-graph/v1' as const;
export const BUSINESS_GRAPH_SCHEMA = 'fachtracing-business-graph/v1' as const;
export type GraphSchema = typeof DEVELOPER_GRAPH_SCHEMA | typeof BUSINESS_GRAPH_SCHEMA;

export const NODE_KINDS = [
  'ENTRY',
  'PREDICATE',
  'CHOICE',
  'COMPUTATION',
  'DISPATCH',
  'OUTCOME',
  'COVERAGE_GAP'
] as const;

export type NodeKind = (typeof NODE_KINDS)[number];

export interface GraphNode {
  id: string;
  kind: NodeKind;
  label: string;
  attributes: Readonly<Record<string, string>>;
}

export interface GraphEdge {
  id: string;
  from: string;
  to: string;
  outcome: string;
}

export interface CoverageGap {
  nodeId: string;
  description: string;
}

export interface GraphModel {
  schema: GraphSchema;
  id: string;
  version: number;
  label: string;
  entryNodeId: string;
  entryNodeIds: readonly string[];
  completeness: 'COMPLETE' | 'INCOMPLETE';
  nodes: readonly GraphNode[];
  edges: readonly GraphEdge[];
  coverageGaps: readonly CoverageGap[];
}

type JsonObject = Record<string, unknown>;

export class ContractError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ContractError';
  }
}

function object(value: unknown, path: string): JsonObject {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new ContractError(`${path} must be an object`);
  }
  return value as JsonObject;
}

function array(value: unknown, path: string): unknown[] {
  if (!Array.isArray(value)) throw new ContractError(`${path} must be an array`);
  return value;
}

function text(value: unknown, path: string): string {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new ContractError(`${path} must be a non-empty string`);
  }
  return value;
}

function integer(value: unknown, path: string): number {
  if (!Number.isSafeInteger(value) || Number(value) < 1) {
    throw new ContractError(`${path} must be a positive integer`);
  }
  return Number(value);
}

function stringMap(value: unknown, path: string): Readonly<Record<string, string>> {
  const input = object(value, path);
  const result: Record<string, string> = {};
  for (const [key, item] of Object.entries(input)) {
    if (typeof item !== 'string') throw new ContractError(`${path}.${key} must be a string`);
    result[key] = item;
  }
  return Object.freeze(result);
}

function unique(values: readonly string[], path: string): void {
  if (new Set(values).size !== values.length) throw new ContractError(`${path} contains duplicate IDs`);
}

export function parseDeveloperGraph(input: unknown): GraphModel {
  const root = object(input, 'graph document');
  if (root.schema !== DEVELOPER_GRAPH_SCHEMA) throw new ContractError('unsupported developer graph schema');
  const graph = object(root.graph, 'graph');
  const origins = array(root.sourceOrigins, 'sourceOrigins').map((raw, index) => {
    const origin = object(raw, `sourceOrigins[${index}]`);
    const kind = text(origin.kind, `sourceOrigins[${index}].kind`);
    if (!['GIT', 'MAVEN_SOURCE', 'GENERATED', 'LOCAL'].includes(kind)) throw new ContractError(`sourceOrigins[${index}].kind is invalid`);
    if (kind === 'GIT' && origin.revision === undefined || kind !== 'GIT' && origin.revision !== undefined) throw new ContractError(`sourceOrigins[${index}].revision is invalid`);
    return { id: text(origin.id, `sourceOrigins[${index}].id`) };
  });
  if (origins.length === 0) throw new ContractError('sourceOrigins must contain at least one origin');
  unique(origins.map((origin) => origin.id), 'sourceOrigins');
  const originIds = new Set(origins.map((origin) => origin.id));
  for (const [index, raw] of array(root.sourceFiles, 'sourceFiles').entries()) {
    const file = object(raw, `sourceFiles[${index}]`);
    if (!originIds.has(text(file.originId, `sourceFiles[${index}].originId`))) throw new ContractError(`sourceFiles[${index}] references an unknown origin`);
    text(file.path, `sourceFiles[${index}].path`);
    if (!/^[0-9a-f]{64}$/.test(text(file.sha256, `sourceFiles[${index}].sha256`))) throw new ContractError(`sourceFiles[${index}].sha256 is invalid`);
  }
  const nodes = array(graph.nodes, 'graph.nodes').map((raw, index): GraphNode => {
    const node = object(raw, `graph.nodes[${index}]`);
    if (node.source !== undefined) {
      const source = object(node.source, `graph.nodes[${index}].source`);
      if (!originIds.has(text(source.originId, `graph.nodes[${index}].source.originId`))) throw new ContractError(`graph.nodes[${index}].source references an unknown origin`);
    }
    const kind = text(node.kind, `graph.nodes[${index}].kind`);
    if (!NODE_KINDS.includes(kind as NodeKind)) throw new ContractError(`unsupported node kind: ${kind}`);
    return Object.freeze({
      id: text(node.id, `graph.nodes[${index}].id`),
      kind: kind as NodeKind,
      label: text(node.label, `graph.nodes[${index}].label`),
      attributes: stringMap(node.attributes ?? {}, `graph.nodes[${index}].attributes`)
    });
  });
  const edges = array(graph.edges, 'graph.edges').map((raw, index): GraphEdge => {
    const edge = object(raw, `graph.edges[${index}]`);
    return Object.freeze({
      id: text(edge.id, `graph.edges[${index}].id`),
      from: text(edge.from, `graph.edges[${index}].from`),
      to: text(edge.to, `graph.edges[${index}].to`),
      outcome: typeof edge.outcome === 'string' ? edge.outcome : (() => { throw new ContractError(`graph.edges[${index}].outcome must be a string`); })()
    });
  });
  const coverageGaps = array(graph.coverageGaps ?? [], 'graph.coverageGaps').map((raw, index): CoverageGap => {
    const gap = object(raw, `graph.coverageGaps[${index}]`);
    return Object.freeze({
      nodeId: text(gap.nodeId, `graph.coverageGaps[${index}].nodeId`),
      description: text(gap.description, `graph.coverageGaps[${index}].description`)
    });
  });
  unique(nodes.map((node) => node.id), 'graph.nodes');
  unique(edges.map((edge) => edge.id), 'graph.edges');
  const nodeIds = new Set(nodes.map((node) => node.id));
  const entryNodeId = text(graph.entryNodeId, 'graph.entryNodeId');
  if (!nodeIds.has(entryNodeId)) throw new ContractError('graph.entryNodeId references an unknown node');
  for (const edge of edges) {
    if (!nodeIds.has(edge.from) || !nodeIds.has(edge.to)) throw new ContractError(`edge ${edge.id} references an unknown node`);
  }
  for (const gap of coverageGaps) {
    if (!nodeIds.has(gap.nodeId)) throw new ContractError(`coverage gap references an unknown node: ${gap.nodeId}`);
  }
  const completeness = text(graph.completeness, 'graph.completeness');
  if (completeness !== 'COMPLETE' && completeness !== 'INCOMPLETE') throw new ContractError('unsupported graph completeness');
  return Object.freeze({
    schema: DEVELOPER_GRAPH_SCHEMA,
    id: text(graph.id, 'graph.id'),
    version: integer(graph.version, 'graph.version'),
    label: text(graph.label, 'graph.label'),
    entryNodeId,
    entryNodeIds: Object.freeze([entryNodeId]),
    completeness,
    nodes: Object.freeze(nodes),
    edges: Object.freeze(edges),
    coverageGaps: Object.freeze(coverageGaps)
  });
}

const BUSINESS_NODE_KIND: Readonly<Record<string, NodeKind>> = Object.freeze({
  RULE: 'PREDICATE',
  ACTION: 'COMPUTATION',
  RESULT: 'OUTCOME',
  GAP: 'COVERAGE_GAP'
});

export function parseBusinessGraph(input: unknown): GraphModel {
  const root = object(input, 'graph document');
  if (root.schema !== BUSINESS_GRAPH_SCHEMA) throw new ContractError('unsupported business graph schema');
  const nodes = array(root.nodes, 'nodes').map((raw, index): GraphNode => {
    const node = object(raw, `nodes[${index}]`);
    const sourceKind = text(node.kind, `nodes[${index}].kind`);
    const kind = BUSINESS_NODE_KIND[sourceKind];
    if (!kind) throw new ContractError(`unsupported business node kind: ${sourceKind}`);
    return Object.freeze({
      id: text(node.id, `nodes[${index}].id`),
      kind,
      label: text(node.label, `nodes[${index}].label`),
      attributes: Object.freeze({})
    });
  });
  const edges = array(root.edges, 'edges').map((raw, index): GraphEdge => {
    const edge = object(raw, `edges[${index}]`);
    if (typeof edge.outcome !== 'string') throw new ContractError(`edges[${index}].outcome must be a string`);
    return Object.freeze({
      id: text(edge.id, `edges[${index}].id`),
      from: text(edge.from, `edges[${index}].from`),
      to: text(edge.to, `edges[${index}].to`),
      outcome: edge.outcome
    });
  });
  unique(nodes.map((node) => node.id), 'nodes');
  unique(edges.map((edge) => edge.id), 'edges');
  const nodeIds = new Set(nodes.map((node) => node.id));
  const entryNodeIds = array(root.entryNodeIds, 'entryNodeIds').map((value, index) => text(value, `entryNodeIds[${index}]`));
  if (entryNodeIds.length === 0) throw new ContractError('entryNodeIds must contain at least one node');
  unique(entryNodeIds, 'entryNodeIds');
  for (const entryNodeId of entryNodeIds) {
    if (!nodeIds.has(entryNodeId)) throw new ContractError(`entryNodeIds references an unknown node: ${entryNodeId}`);
  }
  for (const edge of edges) {
    if (!nodeIds.has(edge.from) || !nodeIds.has(edge.to)) throw new ContractError(`edge ${edge.id} references an unknown node`);
  }
  const completeness = text(root.completeness, 'completeness');
  if (completeness !== 'COMPLETE' && completeness !== 'INCOMPLETE') throw new ContractError('unsupported graph completeness');
  const coverageGaps = nodes
    .filter((node) => node.kind === 'COVERAGE_GAP')
    .map((node): CoverageGap => Object.freeze({ nodeId: node.id, description: node.label }));
  return Object.freeze({
    schema: BUSINESS_GRAPH_SCHEMA,
    id: text(root.graphId, 'graphId'),
    version: integer(root.version, 'version'),
    label: text(root.decision, 'decision'),
    entryNodeId: entryNodeIds[0],
    entryNodeIds: Object.freeze(entryNodeIds),
    completeness,
    nodes: Object.freeze(nodes),
    edges: Object.freeze(edges),
    coverageGaps: Object.freeze(coverageGaps)
  });
}

export function parseGraphDocument(input: unknown): GraphModel {
  const root = object(input, 'graph document');
  if (root.schema === DEVELOPER_GRAPH_SCHEMA) return parseDeveloperGraph(root);
  if (root.schema === BUSINESS_GRAPH_SCHEMA) return parseBusinessGraph(root);
  throw new ContractError('unsupported graph schema');
}

export function parseDeveloperGraphJson(json: string | Uint8Array): GraphModel {
  try {
    const textValue = typeof json === 'string' ? json : new TextDecoder().decode(json);
    return parseDeveloperGraph(JSON.parse(textValue));
  } catch (error) {
    if (error instanceof ContractError) throw error;
    throw new ContractError('developer graph is not valid JSON');
  }
}

export function parseGraphDocumentJson(json: string | Uint8Array): GraphModel {
  try {
    const textValue = typeof json === 'string' ? json : new TextDecoder().decode(json);
    return parseGraphDocument(JSON.parse(textValue));
  } catch (error) {
    if (error instanceof ContractError) throw error;
    throw new ContractError('graph is not valid JSON');
  }
}
