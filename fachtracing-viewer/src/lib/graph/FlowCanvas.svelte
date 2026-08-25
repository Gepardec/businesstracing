<script lang="ts">
  import { Background, BackgroundVariant, Controls, MarkerType, MiniMap, SvelteFlow, ViewportPortal, type EdgeTypes, type NodeTypes, type Viewport } from '@xyflow/svelte';
  import Search from '@lucide/svelte/icons/search';
  import BookOpen from '@lucide/svelte/icons/book-open';
  import MapIcon from '@lucide/svelte/icons/map';
  import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
  import { onMount } from 'svelte';
  import type { GraphModel } from '$contracts/graph-contract';
  import type { RunHighlight } from '$runs/run-highlight';
  import { layoutGraph } from './layout-client';
  import { accessibleEdgeLabel } from './edge-label';
  import BusinessNode from './BusinessNode.svelte';
  import BusinessEdge from './BusinessEdge.svelte';
  import FitGraph from './FitGraph.svelte';
  import FocusCurrent from './FocusCurrent.svelte';
  import GraphJunctions from './GraphJunctions.svelte';
  import GraphLayoutStatus from './GraphLayoutStatus.svelte';
  import { createGraphPresentation, type GraphDetailMode, type GraphPresentation } from './graph-presentation';
  import { directNeighborhood, focusedNodeBounds, openingNeighborhood, safeCanvasRect, type CanvasRect } from './graph-viewport';
  import type { BusinessFlowEdge, BusinessFlowNode } from './flow-types';
  import type { LayoutResult } from './layout-definition';

  let { graph, highlight, fullPath = true, onNodeSelect }: {
    graph: GraphModel; highlight: RunHighlight | null; fullPath?: boolean; onNodeSelect?: (nodeId: string) => void
  } = $props();
  let layoutNodes = $state<BusinessFlowNode[]>([]);
  let layoutEdges = $state<BusinessFlowEdge[]>([]);
  let layoutDecorations = $state<Pick<LayoutResult, 'junctions' | 'sharedSegments' | 'crossings' | 'regions' | 'width' | 'height'>>({
    junctions: [], sharedSegments: [], crossings: [], regions: [], width: 0, height: 0
  });
  let layoutError = $state('');
  let layoutStatus = $state<'idle' | 'arranging' | 'ready' | 'failed'>('idle');
  let layoutResult = $state<LayoutResult | null>(null);
  let globalLayoutResult: LayoutResult | null = null;
  let search = $state('');
  let viewport = $state<Viewport>({ x: 0, y: 0, zoom: 1 });
  let fitRevision = $state(0);
  let selectedNodeId = $state<string | null>(null);
  let focusNodeId = $state<string | null>(null);
  let viewMode = $state<'explore' | 'overview'>('explore');
  let detailMode = $state<GraphDetailMode>('readable');
  let safeRect = $state<CanvasRect>({ x: 16, y: 64, width: 800, height: 500 });
  let focusRequest = $state(0);
  let previousActiveNodeId: string | null = null;
  let previousGraphIdentity = '';
  let searchMessage = $state('');
  let inspectedEdgeId = $state<string | null>(null);
  let hoveredNodeId = $state<string | null>(null);
  let flowPanel: HTMLElement;
  let layoutRequest = 0;
  let localLayoutRequest = 0;
  const nodeTypes: NodeTypes = { business: BusinessNode };
  const edgeTypes: EdgeTypes = { business: BusinessEdge };
  let readablePresentation = $derived(createGraphPresentation(graph, 'readable'));
  let presentation = $derived(detailMode === 'full' ? createGraphPresentation(graph, 'full') : readablePresentation);
  let displayGraph = $derived(presentation.graph);

  function presentationNodeId(originalNodeId: string | null): string | null {
    return originalNodeId ? presentation.presentationNodeIdByOriginalNodeId.get(originalNodeId) ?? null : null;
  }

  function presentationEdgeId(originalEdgeId: string | null): string | null {
    return originalEdgeId ? presentation.presentationEdgeIdByOriginalEdgeId.get(originalEdgeId) ?? null : null;
  }

  function isOverviewMode(): boolean {
    return viewMode === 'overview';
  }

  function decorateNode(id: string) {
    const memberNodeIds = presentation.nodes.get(id)?.memberNodeIds ?? [id];
    const current = Boolean(highlight?.activeNodeId && memberNodeIds.includes(highlight.activeNodeId));
    const onPath = fullPath && memberNodeIds.some((nodeId) => highlight?.pathNodeIds.has(nodeId) ?? false);
    return { current, onPath, dimmed: fullPath && Boolean(highlight) && !onPath, stepNumber: current ? highlight?.activeStepNumber ?? null : null };
  }

  let nodes = $derived(layoutNodes.map((item) => ({
    ...item,
    selected: selectedNodeId === item.id,
    data: {
      ...item.data,
      ...decorateNode(item.id)
    }
  })));
  let edges = $derived(layoutEdges.map((item) => ({
    ...item,
    zIndex: item.data!.memberEdgeIds.includes(highlight?.activeEdgeId ?? '') ? 3 : fullPath && item.data!.memberEdgeIds.some((edgeId) => highlight?.pathEdgeIds.has(edgeId) ?? false) ? 2 : inspectedEdgeId === item.id ? 1 : 0,
    markerEnd: item.data!.sharedSegmentIds.length > 0 ? undefined : {
      type: MarkerType.ArrowClosed,
      width: 11,
      height: 11,
      color: item.data!.memberEdgeIds.includes(highlight?.activeEdgeId ?? '') ? 'var(--run-current)' : fullPath && item.data!.memberEdgeIds.some((edgeId) => highlight?.pathEdgeIds.has(edgeId) ?? false) ? 'var(--run-path)' : 'var(--graph-edge)'
    },
    data: {
      ...item.data,
      onPath: fullPath && item.data!.memberEdgeIds.some((edgeId) => highlight?.pathEdgeIds.has(edgeId) ?? false),
      current: item.data!.memberEdgeIds.includes(highlight?.activeEdgeId ?? ''),
      inspected: inspectedEdgeId === item.id
    }
  })));

  function applyLayout(input: GraphModel, layout: LayoutResult, currentPresentation: GraphPresentation): void {
    const positions = new Map(layout.nodes.map((node) => [node.id, node]));
    const routes = new Map(layout.edges.map((edge) => [edge.id, edge]));
    layoutNodes = input.nodes.map((node) => {
      const positioned = positions.get(node.id)!;
      const nodePresentation = currentPresentation.nodes.get(node.id)!;
      return {
        id: node.id, type: 'business', position: { x: positioned.x, y: positioned.y },
        data: {
          node,
          memberNodeIds: nodePresentation.memberNodeIds,
          memberLabels: nodePresentation.memberLabels,
          onPath: false, current: false, dimmed: false, stepNumber: null,
          ports: positioned.ports,
          layoutPosition: { x: positioned.x, y: positioned.y },
          occurrence: positioned.occurrence,
          incomingCount: positioned.incomingCount,
          outgoingCount: positioned.outgoingCount
        },
        draggable: false, selectable: true, focusable: true,
        ariaLabel: `${nodePresentation.memberNodeIds.length > 1 ? `${node.kind === 'PREDICATE' ? 'Rule' : 'Action'} sequence with ${nodePresentation.memberNodeIds.length} items: ${nodePresentation.memberLabels.join(', ')}` : `${node.kind}: ${node.label}`}${positioned.occurrence ? `. Occurrence ${positioned.occurrence.index} of ${positioned.occurrence.total}` : ''}.`
      } satisfies BusinessFlowNode;
    });
    layoutEdges = input.edges.map((edge) => {
      const route = routes.get(edge.id)!;
      const edgePresentation = currentPresentation.edges.get(edge.id)!;
      const sourceNode = input.nodes.find((node) => node.id === edge.from)!;
      const targetNode = input.nodes.find((node) => node.id === edge.to)!;
      return {
        id: edge.id, type: 'business', source: edge.from, target: edge.to, label: route.displayLabel || undefined,
        sourceHandle: route.sourcePort.id,
        targetHandle: route.targetPort.id,
        interactionWidth: 14,
        domAttributes: { 'data-source-node': edge.from, 'data-target-node': edge.to },
        data: {
          memberEdgeIds: edgePresentation.memberEdgeIds,
          route: route.points,
          labelPosition: route.labelPosition,
          labelAnchor: route.labelAnchor,
          onPath: false,
          current: false,
          inspected: false,
          rawOutcome: edge.outcome,
          long: route.long,
          secondary: route.secondary,
          sharedSegmentIds: route.sharedSegmentIds
        },
        focusable: true,
        ariaLabel: `${accessibleEdgeLabel(edge.outcome, route.displayLabel)} from ${sourceNode.label} to ${targetNode.label}${edgePresentation.memberEdgeIds.length > 1 ? `. Combined from ${edgePresentation.memberEdgeIds.length} source connections` : ''}`
      } satisfies BusinessFlowEdge;
    });
    layoutDecorations = {
      junctions: layout.junctions,
      sharedSegments: layout.sharedSegments,
      crossings: layout.crossings,
      regions: layout.regions,
      width: layout.width,
      height: layout.height
    };
    layoutResult = layout;
    layoutStatus = 'ready';
    focusRequest += 1;
    fitRevision += 1;
    requestAnimationFrame(measureSafeArea);
  }

  function createLocalGraph(input: GraphModel, focusId: string, showOpening: boolean): GraphModel {
    const neighborhood = showOpening ? openingNeighborhood(input, focusId) : directNeighborhood(input, focusId);
    return {
      ...input,
      entryNodeId: focusId,
      entryNodeIds: [focusId],
      nodes: input.nodes.filter((node) => neighborhood.nodeIds.has(node.id)),
      edges: input.edges.filter((edge) => neighborhood.edgeIds.has(edge.id)),
      coverageGaps: input.coverageGaps.filter((gap) => neighborhood.nodeIds.has(gap.nodeId))
    };
  }

  async function createLocalLayout(currentPresentation: GraphPresentation, focusId: string, parentRequest = layoutRequest, showOpening = false): Promise<void> {
    const request = ++localLayoutRequest;
    const input = createLocalGraph(currentPresentation.graph, focusId, showOpening);
    try {
      let layout: LayoutResult;
      try {
        layout = await layoutGraph(input, 'local');
      } catch {
        layout = await layoutGraph(input);
      }
      if (request !== localLayoutRequest || parentRequest !== layoutRequest || viewMode !== 'explore') return;
      applyLayout(input, layout, currentPresentation);
    } catch {
      if (request !== localLayoutRequest || parentRequest !== layoutRequest || !globalLayoutResult) return;
      viewMode = 'overview';
      applyLayout(currentPresentation.graph, globalLayoutResult, currentPresentation);
    }
  }

  async function createLayout(currentPresentation: GraphPresentation) {
    const input = currentPresentation.graph;
    const request = ++layoutRequest;
    localLayoutRequest += 1;
    layoutError = '';
    layoutStatus = 'arranging';
    layoutResult = null;
    globalLayoutResult = null;
    layoutNodes = [];
    layoutEdges = [];
    selectedNodeId = null;
    focusNodeId = null;
    hoveredNodeId = null;
    viewMode = 'explore';
    searchMessage = '';
    try {
      const layout = await layoutGraph(input);
      if (request !== layoutRequest) return;
      globalLayoutResult = layout;
      focusNodeId = highlight?.activeNodeId
        ? currentPresentation.presentationNodeIdByOriginalNodeId.get(highlight.activeNodeId) ?? input.entryNodeIds[0] ?? null
        : input.entryNodeIds[0] ?? input.nodes[0]?.id ?? null;
      if (isOverviewMode()) applyLayout(input, layout, currentPresentation);
      else if (focusNodeId) await createLocalLayout(currentPresentation, focusNodeId, request, !highlight?.activeNodeId);
      else applyLayout(input, layout, currentPresentation);
    } catch {
      if (request !== layoutRequest) return;
      layoutError = 'The graph layout could not be created. Check the graph structure and try again.';
      layoutStatus = 'failed';
    }
  }

  function edgeIdFromTarget(target: EventTarget | null): string | null {
    return target instanceof Element ? target.closest<SVGGElement>('.svelte-flow__edge[data-id]')?.dataset.id ?? null : null;
  }

  function inspectFocusedEdge(event: FocusEvent): void {
    inspectedEdgeId = edgeIdFromTarget(event.target);
  }

  function clearFocusedEdge(event: FocusEvent): void {
    const nextEdgeId = edgeIdFromTarget(event.relatedTarget);
    inspectedEdgeId = nextEdgeId;
  }

  $effect(() => {
    const identity = `${graph.id}@${graph.version}`;
    if (identity !== previousGraphIdentity) {
      previousGraphIdentity = identity;
      detailMode = 'readable';
    }
  });
  $effect(() => { createLayout(presentation); });
  $effect(() => {
    const activeNodeId = highlight?.activeNodeId ?? null;
    if (activeNodeId !== previousActiveNodeId) {
      previousActiveNodeId = activeNodeId;
      selectedNodeId = null;
      focusNodeId = presentationNodeId(activeNodeId) ?? displayGraph.entryNodeIds[0] ?? null;
      viewMode = 'explore';
      if (focusNodeId && globalLayoutResult) void createLocalLayout(presentation, focusNodeId);
    }
  });

  onMount(() => {
    let frame = 0;
    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(measureSafeArea);
    });
    observer.observe(flowPanel);
    return () => { cancelAnimationFrame(frame); observer.disconnect(); };
  });

  function measureSafeArea(): void {
    if (!flowPanel) return;
    const panel = flowPanel.getBoundingClientRect();
    const overlays = [...flowPanel.querySelectorAll<HTMLElement>('.canvas-toolbar, .svelte-flow__controls, .business-minimap, .large-graph-guide, .svelte-flow__attribution')]
      .filter((element) => element.offsetWidth > 0 && element.offsetHeight > 0)
      .map((element) => {
        const rect = element.getBoundingClientRect();
        return { x: rect.left - panel.left, y: rect.top - panel.top, width: rect.width, height: rect.height };
      });
    safeRect = safeCanvasRect(panel.width, panel.height, overlays);
    fitRevision += 1;
  }

  let readingBounds = $derived(layoutResult && focusNodeId
    ? { x: 0, y: 0, width: Math.max(1, layoutDecorations.width), height: Math.max(1, layoutDecorations.height) }
    : null);
  let readingFocusBounds = $derived(layoutResult && focusNodeId
    ? focusedNodeBounds(layoutResult.nodes, focusNodeId)
    : null);
  let overviewBounds = $derived<CanvasRect>({ x: 0, y: 0, width: Math.max(1, layoutDecorations.width), height: Math.max(1, layoutDecorations.height) });
  let hoveredPresentationNode = $derived(hoveredNodeId ? presentation.nodes.get(hoveredNodeId) ?? null : null);
  let zoomedNode = $derived((viewport.zoom < 0.72 || (hoveredPresentationNode?.memberNodeIds.length ?? 0) > 1)
    ? displayGraph.nodes.find((node) => node.id === hoveredNodeId) ?? null
    : null);

  function selectNode(nodeId: string): void {
    selectedNodeId = nodeId;
    focusNodeId = nodeId;
    viewMode = 'explore';
    if (globalLayoutResult) void createLocalLayout(presentation, nodeId);
    const members = presentation.nodes.get(nodeId)?.memberNodeIds ?? [nodeId];
    const selectedOriginal = highlight?.activeNodeId && members.includes(highlight.activeNodeId)
      ? highlight.activeNodeId
      : members.find((member) => highlight?.pathNodeIds.has(member)) ?? members[0];
    onNodeSelect?.(selectedOriginal);
  }

  function selectOriginalNode(nodeId: string): void {
    const displayedNodeId = presentation.presentationNodeIdByOriginalNodeId.get(nodeId);
    if (displayedNodeId) selectNode(displayedNodeId);
  }

  function changeViewMode(mode: 'explore' | 'overview'): void {
    if (viewMode === mode) return;
    viewMode = mode;
    if (mode === 'explore') {
      focusNodeId = selectedNodeId ?? presentationNodeId(highlight?.activeNodeId ?? null) ?? displayGraph.entryNodeIds[0] ?? displayGraph.nodes[0]?.id ?? null;
      if (focusNodeId && globalLayoutResult) void createLocalLayout(presentation, focusNodeId, layoutRequest, !selectedNodeId && !highlight?.activeNodeId);
    } else if (globalLayoutResult) {
      localLayoutRequest += 1;
      applyLayout(displayGraph, globalLayoutResult, presentation);
    }
  }

  function changeDetailMode(mode: GraphDetailMode): void {
    if (detailMode === mode) return;
    detailMode = mode;
    viewMode = 'explore';
  }

  let activePresentationEdgeId = $derived(presentationEdgeId(highlight?.activeEdgeId ?? null));
  let pathPresentationEdgeIds = $derived(new Set([...highlight?.pathEdgeIds ?? []]
    .map((edgeId) => presentation.presentationEdgeIdByOriginalEdgeId.get(edgeId))
    .filter((edgeId): edgeId is string => Boolean(edgeId))));

  function selectSearchResult() {
    const normalized = search.trim().toLowerCase();
    if (!normalized) {
      searchMessage = 'Enter a node name or ID.';
      return;
    }
    const exactMatch = graph.nodes.find((node) => node.id.toLowerCase() === normalized);
    const labelMatches = [...graph.nodes]
      .filter((node) => node.label.toLowerCase().includes(normalized))
      .sort((first, second) => first.id.localeCompare(second.id));
    const match = exactMatch ?? labelMatches[0];
    if (!match) {
      searchMessage = normalized ? 'No matching node was found.' : 'Enter a node name or ID.';
      return;
    }
    selectOriginalNode(match.id);
    const occurrence = exactMatch ? '' : labelMatches.length > 1 ? ` Match 1 of ${labelMatches.length}.` : '';
    searchMessage = `Selected ${match.label}.${occurrence}`;
  }
</script>

<section bind:this={flowPanel} class="flow-panel" class:dense-graph={layoutEdges.length / Math.max(1, layoutNodes.length) > 2.5} aria-label={`Business graph: ${graph.label}`}
  aria-busy={layoutStatus === 'arranging'} onfocusin={inspectFocusedEdge} onfocusout={clearFocusedEdge}>
  <div class="canvas-toolbar">
    <label><span class="sr-only">Find a node</span><Search size={15} /><input bind:value={search} onkeydown={(event) => event.key === 'Enter' && selectSearchResult()} placeholder="Find a node" /></label>
    <div class="view-modes" aria-label="Graph view">
      <button type="button" class:active={viewMode === 'explore'} aria-pressed={viewMode === 'explore'} onclick={() => changeViewMode('explore')}><BookOpen size={14} />Explore</button>
      <button type="button" class:active={viewMode === 'overview'} aria-pressed={viewMode === 'overview'} onclick={() => changeViewMode('overview')}><MapIcon size={14} />Overview</button>
    </div>
    {#if readablePresentation.reduced}
      <div class="view-modes detail-modes" aria-label="Graph detail">
        <button type="button" class:active={detailMode === 'readable'} aria-pressed={detailMode === 'readable'} onclick={() => changeDetailMode('readable')}>Readable</button>
        <button type="button" class:active={detailMode === 'full'} aria-pressed={detailMode === 'full'} onclick={() => changeDetailMode('full')}>Full detail</button>
      </div>
      <span class="reduction-count">
        {#if viewMode === 'explore'}
          {layoutNodes.length} nearby nodes · Select a node to continue
        {:else}
          {displayGraph.nodes.length} of {graph.nodes.length} nodes
        {/if}
      </span>
    {/if}
  </div>
  {#if layoutError}
    <div class="layout-error" role="alert"><TriangleAlert size={20} /><div><strong>Graph layout unavailable</strong><p>{layoutError}</p></div></div>
  {:else}
    <SvelteFlow {nodes} {edges} {nodeTypes} {edgeTypes} bind:viewport minZoom={0.01} maxZoom={1.6}
      deleteKey={null} nodesDraggable={false} nodesConnectable={false}
      onnodeclick={({ node }) => selectNode(node.id)}
      onnodepointerenter={({ node }) => hoveredNodeId = node.id}
      onnodepointerleave={({ node }) => { if (hoveredNodeId === node.id) hoveredNodeId = null; }}
      onedgepointerenter={({ edge }) => inspectedEdgeId = edge.id}
      onedgepointerleave={({ edge }) => { if (inspectedEdgeId === edge.id) inspectedEdgeId = null; }}>
      <Background variant={BackgroundVariant.Dots} patternColor="var(--graph-grid)" gap={22} size={1.2} />
      <ViewportPortal target="front">
        <GraphJunctions
          junctions={layoutDecorations.junctions}
          sharedSegments={layoutDecorations.sharedSegments}
          crossings={layoutDecorations.crossings}
          regions={layoutDecorations.regions}
          width={layoutDecorations.width}
          height={layoutDecorations.height}
          activeEdgeId={activePresentationEdgeId}
          pathEdgeIds={fullPath ? pathPresentationEdgeIds : new Set<string>()}
          {inspectedEdgeId}
        />
      </ViewportPortal>
      <Controls showLock={false} showFitView={false} />
      {#if displayGraph.nodes.length > 8 && displayGraph.nodes.length <= 100}
        <MiniMap class="business-minimap" width={152} height={112} pannable zoomable nodeColor="var(--muted-foreground)" maskColor="color-mix(in oklch, var(--graph-canvas), transparent 55%)" maskStrokeColor="var(--primary)" maskStrokeWidth={1} />
      {:else if displayGraph.nodes.length > 100}
        <div class="large-graph-guide" aria-label={`${displayGraph.nodes.length}-node graph navigation`}>
          <strong>{displayGraph.nodes.length} nodes</strong>
          <span>Search to jump · Overview shows all</span>
        </div>
      {/if}
      {#if layoutStatus === 'ready' && viewMode === 'overview'}
        <FitGraph revision={fitRevision} bounds={overviewBounds} {safeRect} />
      {:else if layoutStatus === 'ready'}
        <FocusCurrent bounds={readingBounds} focusBounds={readingFocusBounds} {focusNodeId} {safeRect} requestToken={focusRequest + fitRevision} />
      {/if}
    </SvelteFlow>
    {#if zoomedNode}
      <div class="zoomed-node-readout" role="status" aria-label="Zoomed node label">
        <span>{(hoveredPresentationNode?.memberNodeIds.length ?? 0) > 1 ? `${hoveredPresentationNode!.memberNodeIds.length} ${zoomedNode.kind === 'PREDICATE' ? 'rule' : 'action'} sequence` : zoomedNode.kind.replace('_', ' ')}</span>
        <strong>{zoomedNode.label}</strong>
        {#if (hoveredPresentationNode?.memberLabels.length ?? 0) > 1}
          <ol>{#each hoveredPresentationNode!.memberLabels as label}<li>{label}</li>{/each}</ol>
        {/if}
      </div>
    {/if}
    {#if layoutStatus === 'arranging'}<GraphLayoutStatus nodeCount={displayGraph.nodes.length} edgeCount={displayGraph.edges.length} />{/if}
  {/if}
</section>
<span class="sr-only" role="status">{searchMessage}</span>

<details class="semantic-node-list">
  <summary>Accessible graph list ({graph.nodes.length} nodes, {graph.edges.length} edges)</summary>
  <h2>Nodes</h2>
  <ol>{#each graph.nodes as node}<li><button onclick={() => selectOriginalNode(node.id)}><span>{node.kind.replace('_', ' ')}</span> {node.label} <small>{node.id}</small></button></li>{/each}</ol>
  <h2>Edges</h2>
  <ol>{#each graph.edges as edge}<li>{edge.outcome || 'Continuation'} from {graph.nodes.find((node) => node.id === edge.from)?.label} to {graph.nodes.find((node) => node.id === edge.to)?.label}</li>{/each}</ol>
</details>
{#each layoutDecorations.junctions as junction (junction.id)}
  <span class="sr-only">{junction.incomingEdgeIds.length} routes converge before {displayGraph.nodes.find((node) => node.id === junction.targetNodeId)?.label}.</span>
{/each}

<style>
  .flow-panel { height: 100%; min-height: 360px; position: relative; background: var(--graph-canvas); overflow: hidden; }
  :global(.svelte-flow) { background: var(--graph-canvas); }
  .dense-graph :global(.svelte-flow__background) { opacity: .6; }
  .canvas-toolbar { position: absolute; top: 14px; left: 14px; right: 180px; z-index: 5; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
  label { height: 36px; width: 220px; display: flex; align-items: center; gap: 7px; background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 0 10px; box-shadow: var(--shadow); }
  input { width: 100%; border: 0; outline: 0; color: var(--foreground); background: transparent; font-size: 13px; }
  .view-modes { height: 36px; display: flex; align-items: center; padding: 3px; border: 1px solid var(--border); border-radius: 8px; background: var(--card); box-shadow: var(--shadow); }
  .view-modes button { height: 28px; display: flex; align-items: center; gap: 5px; padding: 0 9px; border: 0; border-radius: 6px; background: transparent; color: var(--muted-foreground); font-size: 11px; font-weight: 700; cursor: pointer; }
  .view-modes button.active { background: var(--secondary); color: var(--foreground); }
  .view-modes button:focus-visible { outline: 2px solid var(--ring); outline-offset: 1px; }
  .detail-modes button { min-width: 70px; justify-content: center; }
  .reduction-count { color: var(--muted-foreground); font-size: 10px; font-weight: 700; white-space: nowrap; }
  .layout-error { height: 100%; display: flex; justify-content: center; align-items: center; gap: 12px; color: var(--status-failure); }
  .layout-error p { color: var(--muted-foreground); margin: 4px 0 0; }
  .semantic-node-list { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  .semantic-node-list:focus-within { position: fixed; width: min(540px, 90vw); height: auto; clip: auto; overflow: auto; z-index: 100; left: 20px; bottom: 20px; padding: 16px; background: var(--card); border: 1px solid var(--border); border-radius: 10px; }
  .semantic-node-list button { border: 0; background: none; color: inherit; text-align: left; padding: 4px; }
  .semantic-node-list span { font-weight: 700; }
  .semantic-node-list small { color: var(--muted-foreground); }
  .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  :global(.svelte-flow__controls) { box-shadow: var(--shadow); overflow: hidden; border: 1px solid var(--border); border-radius: 9px; }
  :global(.svelte-flow__controls-button) { width: 32px; height: 32px; border-bottom-color: var(--border); background: color-mix(in oklch, var(--card), transparent 3%); color: var(--foreground); }
  :global(.business-minimap) { overflow: hidden; border: 1px solid var(--border); border-radius: 9px; background: color-mix(in oklch, var(--card), transparent 3%); box-shadow: var(--shadow); }
  .large-graph-guide { position: absolute; right: 14px; bottom: 14px; z-index: 5; display: grid; gap: 2px; min-width: 174px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 9px; background: color-mix(in oklch, var(--card), transparent 3%); box-shadow: var(--shadow); }
  .large-graph-guide strong { font-size: 12px; }
  .large-graph-guide span { color: var(--muted-foreground); font-size: 10px; }
  .zoomed-node-readout { position: absolute; top: 14px; right: 14px; z-index: 6; pointer-events: none; width: min(280px, calc(100% - 28px)); display: grid; gap: 3px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 9px; background: color-mix(in oklch, var(--card), transparent 3%); box-shadow: var(--shadow); }
  .zoomed-node-readout span { color: var(--muted-foreground); font-size: 9px; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; }
  .zoomed-node-readout strong { font-size: 13px; line-height: 1.3; }
  .zoomed-node-readout ol { max-height: 180px; margin: 5px 0 0; padding: 7px 0 0 18px; overflow: auto; border-top: 1px solid var(--border); color: var(--muted-foreground); font-size: 11px; line-height: 1.45; }
  .zoomed-node-readout li + li { margin-top: 3px; }
  @media (max-width: 640px) {
    .canvas-toolbar { right: 14px; align-items: stretch; flex-direction: column; flex-wrap: nowrap; }
    label { width: auto; }
    .view-modes { align-self: flex-start; }
  }
</style>
