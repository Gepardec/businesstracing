import type { GraphEdge, GraphModel, GraphNode } from '$contracts/graph-contract';
import { analyzeTopology } from './topology-analysis';

export type GraphDetailMode = 'readable' | 'full';

export interface PresentationNodeInfo {
  readonly id: string;
  readonly memberNodeIds: readonly string[];
  readonly memberLabels: readonly string[];
}

export interface PresentationEdgeInfo {
  readonly id: string;
  readonly memberEdgeIds: readonly string[];
}

export interface GraphNarrative {
  readonly sentences: readonly string[];
  readonly ruleCount: number;
  readonly actionCount: number;
  readonly outcomeCount: number;
  readonly branchCount: number;
  readonly cycleCount: number;
}

export interface GraphPresentation {
  readonly graph: GraphModel;
  readonly mode: GraphDetailMode;
  readonly reduced: boolean;
  readonly nodes: ReadonlyMap<string, PresentationNodeInfo>;
  readonly edges: ReadonlyMap<string, PresentationEdgeInfo>;
  readonly presentationNodeIdByOriginalNodeId: ReadonlyMap<string, string>;
  readonly presentationEdgeIdByOriginalEdgeId: ReadonlyMap<string, string | null>;
  readonly narrative: GraphNarrative;
}

interface EdgeGroup {
  readonly from: string;
  readonly to: string;
  readonly edges: GraphEdge[];
}

function unique<T>(values: readonly T[]): T[] {
  return [...new Set(values)];
}

function edgeGroups(graph: GraphModel): EdgeGroup[] {
  const groups = new Map<string, EdgeGroup>();
  for (const edge of graph.edges) {
    const key = `${edge.from}\u0000${edge.to}`;
    const group = groups.get(key) ?? { from: edge.from, to: edge.to, edges: [] };
    group.edges.push(edge);
    groups.set(key, group);
  }
  return [...groups.values()].sort((first, second) =>
    first.from.localeCompare(second.from) || first.to.localeCompare(second.to)
  );
}

function sequenceCategory(node: GraphNode): 'action' | 'rule' | null {
  if (node.kind === 'COMPUTATION') return 'action';
  if (node.kind === 'PREDICATE') return 'rule';
  return null;
}

function unconditionalConnection(group: EdgeGroup): boolean {
  if (group.edges.every((edge) => edge.outcome.trim() === '')) return true;
  const outcomes = new Set(group.edges.map((edge) => edge.outcome.trim().toLowerCase()).filter(Boolean));
  return outcomes.size === 2 && outcomes.has('yes') && outcomes.has('no');
}

function guardSequences(graph: GraphModel): string[][] {
  const nodeById = new Map(graph.nodes.map((node) => [node.id, node]));
  const entries = new Set(graph.entryNodeIds);
  const groups = edgeGroups(graph);
  const incoming = new Map(graph.nodes.map((node) => [node.id, [] as EdgeGroup[]]));
  const outgoing = new Map(graph.nodes.map((node) => [node.id, [] as EdgeGroup[]]));
  for (const group of groups) {
    incoming.get(group.to)!.push(group);
    outgoing.get(group.from)!.push(group);
  }

  const candidates: string[][] = [];
  for (const start of graph.nodes.filter((node) => node.kind === 'PREDICATE' && !entries.has(node.id))) {
    for (const exitTarget of unique(outgoing.get(start.id)!.map((group) => group.to))) {
      const sequence: string[] = [];
      const visited = new Set<string>();
      let current = start.id;
      while (!visited.has(current)) {
        visited.add(current);
        const currentNode = nodeById.get(current)!;
        const currentGroups = outgoing.get(current)!;
        const exits = currentGroups.filter((group) => group.to === exitTarget);
        const continuations = currentGroups.filter((group) => group.to !== exitTarget);
        if (currentNode.kind !== 'PREDICATE' || exits.length !== 1 || continuations.length !== 1) break;
        sequence.push(current);
        const next = continuations[0].to;
        const nextNode = nodeById.get(next)!;
        if (next === exitTarget || nextNode.kind !== 'PREDICATE' || entries.has(next) ||
            incoming.get(next)!.length !== 1 || incoming.get(next)![0].from !== current) break;
        current = next;
      }
      if (sequence.length > 1) candidates.push(sequence);
    }
  }

  const assigned = new Set<string>();
  const selected: string[][] = [];
  for (const sequence of candidates.sort((first, second) =>
    second.length - first.length || first[0].localeCompare(second[0]) || first.join('\u0000').localeCompare(second.join('\u0000'))
  )) {
    if (sequence.some((nodeId) => assigned.has(nodeId))) continue;
    selected.push(sequence);
    for (const nodeId of sequence) assigned.add(nodeId);
  }
  return selected;
}

function linearSequences(graph: GraphModel, blocked: ReadonlySet<string>): string[][] {
  const nodeById = new Map(graph.nodes.map((node) => [node.id, node]));
  const entries = new Set(graph.entryNodeIds);
  const groups = edgeGroups(graph);
  const incoming = new Map(graph.nodes.map((node) => [node.id, [] as EdgeGroup[]]));
  const outgoing = new Map(graph.nodes.map((node) => [node.id, [] as EdgeGroup[]]));
  for (const group of groups) {
    incoming.get(group.to)!.push(group);
    outgoing.get(group.from)!.push(group);
  }

  function joins(group: EdgeGroup): boolean {
    const source = nodeById.get(group.from)!;
    const target = nodeById.get(group.to)!;
    const category = sequenceCategory(source);
    return category !== null && sequenceCategory(target) === category &&
      !blocked.has(source.id) && !blocked.has(target.id) &&
      !entries.has(source.id) && !entries.has(target.id) &&
      unconditionalConnection(group) &&
      incoming.get(source.id)!.length === 1 && outgoing.get(source.id)!.length === 1 &&
      incoming.get(target.id)!.length === 1 && outgoing.get(target.id)!.length === 1;
  }

  const assigned = new Set<string>(blocked);
  const sequences: string[][] = [];
  const starts = [...graph.nodes]
    .filter((node) => sequenceCategory(node) !== null && !entries.has(node.id))
    .filter((node) => !incoming.get(node.id)!.some(joins))
    .sort((first, second) => first.id.localeCompare(second.id));

  for (const start of starts) {
    if (assigned.has(start.id)) continue;
    const sequence = [start.id];
    assigned.add(start.id);
    let current = start.id;
    while (true) {
      const nextGroup = outgoing.get(current)!.find(joins);
      if (!nextGroup || assigned.has(nextGroup.to)) break;
      sequence.push(nextGroup.to);
      assigned.add(nextGroup.to);
      current = nextGroup.to;
    }
    if (sequence.length > 1) sequences.push(sequence);
  }
  return sequences;
}

function sequenceLabel(members: readonly GraphNode[]): string {
  if (members.length === 2) return `${members[0].label} → ${members[1].label}`;
  return `${members[0].label} → ${members.at(-1)!.label}`;
}

function combinedOutcome(edges: readonly GraphEdge[]): string {
  const outcomes = unique(edges.map((edge) => edge.outcome.trim()).filter(Boolean));
  const normalized = new Set(outcomes.map((outcome) => outcome.toLowerCase()));
  if (normalized.size === 2 && normalized.has('yes') && normalized.has('no')) return 'Any result';
  if (outcomes.length === 0) return '';
  if (outcomes.length <= 3) return outcomes.join(' / ');
  return `${outcomes.length} alternatives`;
}

function quotedList(labels: readonly string[], maximum = 3): string {
  const visible = unique(labels).slice(0, maximum).map((label) => `“${label}”`);
  const remaining = unique(labels).length - visible.length;
  if (remaining > 0) visible.push(`${remaining} more`);
  if (visible.length < 2) return visible[0] ?? '';
  return `${visible.slice(0, -1).join(', ')} or ${visible.at(-1)}`;
}

function firstMaterialBranch(graph: GraphModel): string[] {
  if (graph.entryNodeIds.length > 1) {
    const labels = graph.entryNodeIds.map((id) => graph.nodes.find((node) => node.id === id)!.label);
    return unique(labels);
  }
  const outgoing = new Map(graph.nodes.map((node) => [node.id, [] as string[]]));
  for (const edge of graph.edges) outgoing.get(edge.from)!.push(edge.to);
  let current = graph.entryNodeIds[0];
  const visited = new Set<string>();
  for (let depth = 0; depth < 10 && current && !visited.has(current); depth += 1) {
    visited.add(current);
    const targets = unique(outgoing.get(current) ?? []);
    if (targets.length > 1) {
      return unique(targets.map((id) => graph.nodes.find((node) => node.id === id)!.label));
    }
    if (targets.length !== 1) return [];
    current = targets[0];
  }
  return [];
}

export function explainGraph(graph: GraphModel): GraphNarrative {
  const topology = analyzeTopology(graph);
  const entryLabels = unique(graph.entryNodeIds.map((id) => graph.nodes.find((node) => node.id === id)!.label));
  const branchLabels = firstMaterialBranch(graph);
  const outcomeLabels = unique(graph.nodes.filter((node) => node.kind === 'OUTCOME').map((node) => node.label));
  const ruleCount = graph.nodes.filter((node) => node.kind === 'PREDICATE' || node.kind === 'CHOICE').length;
  const actionCount = graph.nodes.filter((node) => node.kind === 'COMPUTATION' || node.kind === 'DISPATCH').length;
  const branchCount = [...new Set(graph.edges.map((edge) => edge.from))].filter((nodeId) =>
    unique(graph.edges.filter((edge) => edge.from === nodeId).map((edge) => edge.to)).length > 1
  ).length;
  const cycleCount = topology.stronglyConnectedComponents.filter((component) => component.cyclic).length;
  const start = graph.entryNodeIds.length === 1
    ? `This decision starts with ${quotedList(entryLabels)}`
    : `This decision has ${graph.entryNodeIds.length} entry points: ${quotedList(entryLabels)}`;
  const opening = `${start}.`;
  const alternatives = branchLabels.length > 1
    ? `The first alternatives continue to ${quotedList(branchLabels)}.`
    : branchLabels.length === 1 ? `The flow first continues to ${quotedList(branchLabels)}.` : '';
  const repeat = cycleCount > 0 ? ', and some paths can return to an earlier check' : '';
  const result = outcomeLabels.length > 0
    ? `Possible results are ${quotedList(outcomeLabels)}${repeat}.`
    : `No explicit terminal result is declared${graph.completeness === 'INCOMPLETE' ? ' because the graph is incomplete' : ''}${repeat}.`;
  return {
    sentences: [opening, alternatives, result].filter(Boolean),
    ruleCount,
    actionCount,
    outcomeCount: outcomeLabels.length,
    branchCount,
    cycleCount
  };
}

function fullPresentation(graph: GraphModel): GraphPresentation {
  const nodes = new Map(graph.nodes.map((node) => [node.id, {
    id: node.id,
    memberNodeIds: [node.id],
    memberLabels: [node.label]
  }]));
  const edges = new Map(graph.edges.map((edge) => [edge.id, { id: edge.id, memberEdgeIds: [edge.id] }]));
  return {
    graph,
    mode: 'full',
    reduced: false,
    nodes,
    edges,
    presentationNodeIdByOriginalNodeId: new Map(graph.nodes.map((node) => [node.id, node.id])),
    presentationEdgeIdByOriginalEdgeId: new Map(graph.edges.map((edge) => [edge.id, edge.id])),
    narrative: explainGraph(graph)
  };
}

export function createGraphPresentation(graph: GraphModel, mode: GraphDetailMode = 'readable'): GraphPresentation {
  if (mode === 'full') return fullPresentation(graph);
  const nodeById = new Map(graph.nodes.map((node) => [node.id, node]));
  const sequenceByMemberId = new Map<string, readonly string[]>();
  const guards = guardSequences(graph);
  const guardMembers = new Set(guards.flat());
  for (const sequence of [...guards, ...linearSequences(graph, guardMembers)]) {
    for (const nodeId of sequence) sequenceByMemberId.set(nodeId, sequence);
  }

  const presentationNodeIdByOriginalNodeId = new Map<string, string>();
  const presentationNodes: GraphNode[] = [];
  const nodeInfo = new Map<string, PresentationNodeInfo>();
  const emittedSequences = new Set<readonly string[]>();
  for (const original of graph.nodes) {
    const sequence = sequenceByMemberId.get(original.id);
    if (!sequence) {
      presentationNodes.push(original);
      presentationNodeIdByOriginalNodeId.set(original.id, original.id);
      nodeInfo.set(original.id, { id: original.id, memberNodeIds: [original.id], memberLabels: [original.label] });
      continue;
    }
    if (emittedSequences.has(sequence)) continue;
    emittedSequences.add(sequence);
    const members = sequence.map((nodeId) => nodeById.get(nodeId)!);
    const id = `sequence-${members[0].id}`;
    const labels = members.map((node) => node.label);
    const category = sequenceCategory(members[0])!;
    presentationNodes.push(Object.freeze({
      id,
      kind: members[0].kind,
      label: sequenceLabel(members),
      attributes: Object.freeze({
        'presentation.type': `${category}-sequence`,
        'presentation.count': String(members.length),
        'presentation.labels': JSON.stringify(labels)
      })
    }));
    nodeInfo.set(id, { id, memberNodeIds: [...sequence], memberLabels: labels });
    for (const member of members) presentationNodeIdByOriginalNodeId.set(member.id, id);
  }

  const originalEdgesByPresentationPair = new Map<string, GraphEdge[]>();
  const presentationEdgeIdByOriginalEdgeId = new Map<string, string | null>();
  for (const edge of graph.edges) {
    const from = presentationNodeIdByOriginalNodeId.get(edge.from)!;
    const to = presentationNodeIdByOriginalNodeId.get(edge.to)!;
    if (from === to) {
      presentationEdgeIdByOriginalEdgeId.set(edge.id, null);
      continue;
    }
    const key = `${from}\u0000${to}`;
    originalEdgesByPresentationPair.set(key, [...(originalEdgesByPresentationPair.get(key) ?? []), edge]);
  }

  const presentationEdges: GraphEdge[] = [];
  const edgeInfo = new Map<string, PresentationEdgeInfo>();
  for (const [key, members] of [...originalEdgesByPresentationPair].sort(([first], [second]) => first.localeCompare(second))) {
    const [from, to] = key.split('\u0000');
    const id = members.length === 1 ? members[0].id : `connection-${members[0].id}`;
    presentationEdges.push(Object.freeze({ id, from, to, outcome: combinedOutcome(members) }));
    edgeInfo.set(id, { id, memberEdgeIds: members.map((edge) => edge.id) });
    for (const member of members) presentationEdgeIdByOriginalEdgeId.set(member.id, id);
  }

  const presentationEntryNodeIds = unique(graph.entryNodeIds.map((id) => presentationNodeIdByOriginalNodeId.get(id)!));
  const presentationGraph: GraphModel = Object.freeze({
    ...graph,
    entryNodeId: presentationEntryNodeIds[0],
    entryNodeIds: Object.freeze(presentationEntryNodeIds),
    nodes: Object.freeze(presentationNodes),
    edges: Object.freeze(presentationEdges),
    coverageGaps: Object.freeze(graph.coverageGaps.map((gap) => ({
      ...gap,
      nodeId: presentationNodeIdByOriginalNodeId.get(gap.nodeId)!
    })))
  });
  const reduced = presentationGraph.nodes.length < graph.nodes.length || presentationGraph.edges.length < graph.edges.length;
  if (!reduced) return fullPresentation(graph);
  return {
    graph: presentationGraph,
    mode: 'readable',
    reduced,
    nodes: nodeInfo,
    edges: edgeInfo,
    presentationNodeIdByOriginalNodeId,
    presentationEdgeIdByOriginalEdgeId,
    narrative: explainGraph(graph)
  };
}
