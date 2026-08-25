import type { GraphEdge, GraphModel } from '$contracts/graph-contract';

export interface BranchRegion {
  sourceNodeId: string;
  edgeIds: readonly string[];
  memberNodeIds: readonly string[];
  convergenceNodeId: string | null;
}

export interface ConvergenceGroup {
  targetNodeId: string;
  incomingEdgeIds: readonly string[];
}

export interface StronglyConnectedComponent {
  id: string;
  nodeIds: readonly string[];
  cyclic: boolean;
}

export interface DuplicateOccurrence {
  index: number;
  total: number;
}

export interface TopologyAnalysis {
  componentByNodeId: ReadonlyMap<string, string>;
  rankByNodeId: ReadonlyMap<string, number>;
  spineNodeIds: readonly string[];
  branchRegions: readonly BranchRegion[];
  convergenceGroups: readonly ConvergenceGroup[];
  stronglyConnectedComponents: readonly StronglyConnectedComponent[];
  duplicateByNodeId: ReadonlyMap<string, DuplicateOccurrence>;
  longEdgeIds: ReadonlySet<string>;
  incomingByNodeId: ReadonlyMap<string, readonly GraphEdge[]>;
  outgoingByNodeId: ReadonlyMap<string, readonly GraphEdge[]>;
}

function sortedEdges(edges: readonly GraphEdge[]): GraphEdge[] {
  return [...edges].sort((first, second) => first.id.localeCompare(second.id));
}

function adjacency(graph: GraphModel): {
  incoming: Map<string, GraphEdge[]>;
  outgoing: Map<string, GraphEdge[]>;
} {
  const incoming = new Map(graph.nodes.map((node) => [node.id, [] as GraphEdge[]]));
  const outgoing = new Map(graph.nodes.map((node) => [node.id, [] as GraphEdge[]]));
  for (const edge of sortedEdges(graph.edges)) {
    outgoing.get(edge.from)!.push(edge);
    incoming.get(edge.to)!.push(edge);
  }
  return { incoming, outgoing };
}

function weakComponents(graph: GraphModel, incoming: ReadonlyMap<string, readonly GraphEdge[]>, outgoing: ReadonlyMap<string, readonly GraphEdge[]>): Map<string, string> {
  const componentByNodeId = new Map<string, string>();
  const entryOrder = [...graph.entryNodeIds, ...graph.nodes.map((node) => node.id)].filter((id, index, values) => values.indexOf(id) === index);
  let componentIndex = 0;
  for (const start of entryOrder) {
    if (componentByNodeId.has(start)) continue;
    const componentId = `component-${String(componentIndex).padStart(3, '0')}`;
    componentIndex += 1;
    const pending = [start];
    componentByNodeId.set(start, componentId);
    while (pending.length > 0) {
      const nodeId = pending.shift()!;
      const neighbours = [
        ...(incoming.get(nodeId) ?? []).map((edge) => edge.from),
        ...(outgoing.get(nodeId) ?? []).map((edge) => edge.to)
      ].sort();
      for (const neighbour of neighbours) {
        if (componentByNodeId.has(neighbour)) continue;
        componentByNodeId.set(neighbour, componentId);
        pending.push(neighbour);
      }
    }
  }
  return componentByNodeId;
}

function stronglyConnected(graph: GraphModel, outgoing: ReadonlyMap<string, readonly GraphEdge[]>): StronglyConnectedComponent[] {
  let nextIndex = 0;
  const indices = new Map<string, number>();
  const lowLinks = new Map<string, number>();
  const stack: string[] = [];
  const onStack = new Set<string>();
  const components: string[][] = [];

  function visit(nodeId: string): void {
    indices.set(nodeId, nextIndex);
    lowLinks.set(nodeId, nextIndex);
    nextIndex += 1;
    stack.push(nodeId);
    onStack.add(nodeId);

    const targets = (outgoing.get(nodeId) ?? []).map((edge) => edge.to).sort();
    for (const target of targets) {
      if (!indices.has(target)) {
        visit(target);
        lowLinks.set(nodeId, Math.min(lowLinks.get(nodeId)!, lowLinks.get(target)!));
      } else if (onStack.has(target)) {
        lowLinks.set(nodeId, Math.min(lowLinks.get(nodeId)!, indices.get(target)!));
      }
    }

    if (lowLinks.get(nodeId) !== indices.get(nodeId)) return;
    const memberIds: string[] = [];
    let member: string;
    do {
      member = stack.pop()!;
      onStack.delete(member);
      memberIds.push(member);
    } while (member !== nodeId);
    components.push(memberIds.sort());
  }

  for (const node of [...graph.nodes].sort((first, second) => first.id.localeCompare(second.id))) {
    if (!indices.has(node.id)) visit(node.id);
  }

  return components
    .sort((first, second) => first[0].localeCompare(second[0]))
    .map((nodeIds, index) => ({
      id: `scc-${String(index).padStart(3, '0')}`,
      nodeIds,
      cyclic: nodeIds.length > 1 || (outgoing.get(nodeIds[0]) ?? []).some((edge) => edge.to === nodeIds[0])
    }));
}

function ranks(
  graph: GraphModel,
  incoming: ReadonlyMap<string, readonly GraphEdge[]>,
  outgoing: ReadonlyMap<string, readonly GraphEdge[]>,
  components: readonly StronglyConnectedComponent[],
  weakComponentByNodeId: ReadonlyMap<string, string>
): Map<string, number> {
  const sccByNodeId = new Map(components.flatMap((component) => component.nodeIds.map((nodeId) => [nodeId, component.id] as const)));
  const componentById = new Map(components.map((component) => [component.id, component]));
  const outgoingComponents = new Map(components.map((component) => [component.id, new Set<string>()]));
  const incomingCount = new Map(components.map((component) => [component.id, 0]));
  for (const edge of graph.edges) {
    const source = sccByNodeId.get(edge.from)!;
    const target = sccByNodeId.get(edge.to)!;
    if (source === target || outgoingComponents.get(source)!.has(target)) continue;
    outgoingComponents.get(source)!.add(target);
    incomingCount.set(target, incomingCount.get(target)! + 1);
  }

  const rankByComponent = new Map(components.map((component) => [component.id, 0]));
  const pending = components.filter((component) => incomingCount.get(component.id) === 0).map((component) => component.id).sort();
  while (pending.length > 0) {
    const componentId = pending.shift()!;
    for (const target of [...outgoingComponents.get(componentId)!].sort()) {
      rankByComponent.set(target, Math.max(rankByComponent.get(target)!, rankByComponent.get(componentId)! + 1));
      incomingCount.set(target, incomingCount.get(target)! - 1);
      if (incomingCount.get(target) === 0) {
        pending.push(target);
        pending.sort();
      }
    }
  }

  const rankByNodeId = new Map<string, number>();
  for (const [componentId, rank] of rankByComponent) {
    for (const nodeId of componentById.get(componentId)!.nodeIds) rankByNodeId.set(nodeId, rank);
  }

  for (const entryNodeId of graph.entryNodeIds) rankByNodeId.set(entryNodeId, 0);
  for (const node of graph.nodes) {
    if (node.kind === 'OUTCOME' && (incoming.get(node.id)?.length ?? 0) > 0) {
      const componentId = weakComponentByNodeId.get(node.id);
      const maximumNonOutcome = Math.max(0, ...graph.nodes
        .filter((candidate) => candidate.kind !== 'OUTCOME' && weakComponentByNodeId.get(candidate.id) === componentId)
        .map((candidate) => rankByNodeId.get(candidate.id) ?? 0));
      rankByNodeId.set(node.id, maximumNonOutcome + 1);
    }
  }
  return rankByNodeId;
}

function reachableFrom(start: string, outgoing: ReadonlyMap<string, readonly GraphEdge[]>): Set<string> {
  const reached = new Set<string>();
  const pending = [start];
  while (pending.length > 0) {
    const nodeId = pending.shift()!;
    if (reached.has(nodeId)) continue;
    reached.add(nodeId);
    for (const edge of outgoing.get(nodeId) ?? []) pending.push(edge.to);
  }
  return reached;
}

function branchRegions(outgoing: ReadonlyMap<string, readonly GraphEdge[]>, rankByNodeId: ReadonlyMap<string, number>): BranchRegion[] {
  const regions: BranchRegion[] = [];
  for (const [sourceNodeId, sourceEdges] of [...outgoing].sort(([first], [second]) => first.localeCompare(second))) {
    if (sourceEdges.length < 2) continue;
    const reachability = sourceEdges.map((edge) => reachableFrom(edge.to, outgoing));
    const common = [...reachability[0]].filter((nodeId) => reachability.every((nodes) => nodes.has(nodeId)));
    common.sort((first, second) => (rankByNodeId.get(first)! - rankByNodeId.get(second)!) || first.localeCompare(second));
    const convergenceNodeId = common[0] ?? null;
    const members = new Set<string>();
    for (const reached of reachability) {
      for (const nodeId of reached) {
        if (nodeId !== convergenceNodeId) members.add(nodeId);
      }
    }
    regions.push({
      sourceNodeId,
      edgeIds: sourceEdges.map((edge) => edge.id),
      memberNodeIds: [...members].sort(),
      convergenceNodeId
    });
  }
  return regions;
}

function spine(graph: GraphModel, outgoing: ReadonlyMap<string, readonly GraphEdge[]>, rankByNodeId: ReadonlyMap<string, number>): string[] {
  const bestFromNode = new Map<string, string[]>();
  function bestFrom(nodeId: string): string[] {
    const cached = bestFromNode.get(nodeId);
    if (cached) return cached;
    const nextEdges = (outgoing.get(nodeId) ?? [])
      .filter((edge) => (rankByNodeId.get(edge.to) ?? 0) > (rankByNodeId.get(nodeId) ?? 0))
      .sort((first, second) => {
        const firstSequential = first.outcome === '' || first.outcome.toLowerCase() === 'next' ? 0 : 1;
        const secondSequential = second.outcome === '' || second.outcome.toLowerCase() === 'next' ? 0 : 1;
        return firstSequential - secondSequential || first.id.localeCompare(second.id);
      });
    const candidates = nextEdges.map((edge) => [nodeId, ...bestFrom(edge.to)]);
    candidates.sort((first, second) => second.length - first.length || first.join('\u0000').localeCompare(second.join('\u0000')));
    const best = candidates[0] ?? [nodeId];
    bestFromNode.set(nodeId, best);
    return best;
  }
  const candidates = graph.entryNodeIds.map(bestFrom);
  candidates.sort((first, second) => second.length - first.length || first.join('\u0000').localeCompare(second.join('\u0000')));
  return candidates[0] ?? [];
}

function duplicateOccurrences(graph: GraphModel, componentByNodeId: ReadonlyMap<string, string>, rankByNodeId: ReadonlyMap<string, number>): Map<string, DuplicateOccurrence> {
  const groups = new Map<string, string[]>();
  for (const node of graph.nodes) {
    const key = `${node.kind}\u0000${node.label}`;
    groups.set(key, [...(groups.get(key) ?? []), node.id]);
  }
  const result = new Map<string, DuplicateOccurrence>();
  for (const nodeIds of groups.values()) {
    if (nodeIds.length < 2) continue;
    nodeIds.sort((first, second) =>
      componentByNodeId.get(first)!.localeCompare(componentByNodeId.get(second)!) ||
      (rankByNodeId.get(first)! - rankByNodeId.get(second)!) ||
      first.localeCompare(second)
    );
    nodeIds.forEach((nodeId, index) => result.set(nodeId, { index: index + 1, total: nodeIds.length }));
  }
  return result;
}

function feedbackEdges(graph: GraphModel, outgoing: ReadonlyMap<string, readonly GraphEdge[]>): Set<string> {
  const state = new Map<string, 'visiting' | 'visited'>();
  const feedback = new Set<string>();

  function visit(nodeId: string): void {
    state.set(nodeId, 'visiting');
    for (const edge of outgoing.get(nodeId) ?? []) {
      const targetState = state.get(edge.to);
      if (targetState === 'visiting') {
        feedback.add(edge.id);
      } else if (!targetState) {
        visit(edge.to);
      }
    }
    state.set(nodeId, 'visited');
  }

  const starts = [...graph.entryNodeIds, ...graph.nodes.map((node) => node.id)]
    .filter((nodeId, index, values) => values.indexOf(nodeId) === index);
  for (const nodeId of starts) {
    if (!state.has(nodeId)) visit(nodeId);
  }
  return feedback;
}

export function analyzeTopology(graph: GraphModel): TopologyAnalysis {
  const { incoming, outgoing } = adjacency(graph);
  const componentByNodeId = weakComponents(graph, incoming, outgoing);
  const stronglyConnectedComponents = stronglyConnected(graph, outgoing);
  const rankByNodeId = ranks(graph, incoming, outgoing, stronglyConnectedComponents, componentByNodeId);
  const convergenceGroups = [...incoming]
    .filter(([, edges]) => edges.length >= 4)
    .sort(([first], [second]) => first.localeCompare(second))
    .map(([targetNodeId, edges]) => ({ targetNodeId, incomingEdgeIds: edges.map((edge) => edge.id) }));
  const feedbackEdgeIds = feedbackEdges(graph, outgoing);
  const longEdgeIds = new Set(graph.edges.filter((edge) => {
    const rankSpan = (rankByNodeId.get(edge.to) ?? 0) - (rankByNodeId.get(edge.from) ?? 0);
    return feedbackEdgeIds.has(edge.id) || rankSpan > 2 || rankSpan < 0;
  }).map((edge) => edge.id));

  return {
    componentByNodeId,
    rankByNodeId,
    spineNodeIds: spine(graph, outgoing, rankByNodeId),
    branchRegions: branchRegions(outgoing, rankByNodeId),
    convergenceGroups,
    stronglyConnectedComponents,
    duplicateByNodeId: duplicateOccurrences(graph, componentByNodeId, rankByNodeId),
    longEdgeIds,
    incomingByNodeId: incoming,
    outgoingByNodeId: outgoing
  };
}
