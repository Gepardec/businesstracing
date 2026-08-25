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
  import { directNeighborhood, focusedNodeBounds, neighborhoodBounds, OVERVIEW_DETAIL_ZOOM, safeCanvasRect, type CanvasRect } from './graph-viewport';
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
  let search = $state('');
  let viewport = $state<Viewport>({ x: 0, y: 0, zoom: 1 });
  let fitRevision = $state(0);
  let selectedNodeId = $state<string | null>(null);
  let focusNodeId = $state<string | null>(null);
  let viewMode = $state<'reading' | 'overview'>('reading');
  let safeRect = $state<CanvasRect>({ x: 16, y: 64, width: 800, height: 500 });
  let focusRequest = $state(0);
  let previousActiveNodeId: string | null = null;
  let searchMessage = $state('');
  let inspectedEdgeId = $state<string | null>(null);
  let flowPanel: HTMLElement;
  let layoutRequest = 0;
  const nodeTypes: NodeTypes = { business: BusinessNode };
  const edgeTypes: EdgeTypes = { business: BusinessEdge };

  function decorateNode(id: string) {
    const current = highlight?.activeNodeId === id;
    const onPath = fullPath && (highlight?.pathNodeIds.has(id) ?? false);
    return { current, onPath, dimmed: fullPath && Boolean(highlight) && !onPath, stepNumber: current ? highlight?.activeStepNumber ?? null : null };
  }

  let readingContext = $derived(viewMode === 'reading' && focusNodeId && !highlight ? directNeighborhood(graph, focusNodeId) : null);

  let nodes = $derived(layoutNodes.map((item) => ({
    ...item,
    selected: selectedNodeId === item.id,
    data: {
      ...item.data,
      ...decorateNode(item.id),
      showDetails: viewMode === 'reading' || viewport.zoom >= OVERVIEW_DETAIL_ZOOM,
      contextDimmed: Boolean(readingContext && !readingContext.nodeIds.has(item.id))
    }
  })));
  let edges = $derived(layoutEdges.map((item) => ({
    ...item,
    zIndex: highlight?.activeEdgeId === item.id ? 3 : fullPath && (highlight?.pathEdgeIds.has(item.id) ?? false) ? 2 : inspectedEdgeId === item.id ? 1 : 0,
    markerEnd: item.data!.sharedSegmentIds.length > 0 ? undefined : {
      type: MarkerType.ArrowClosed,
      width: 11,
      height: 11,
      color: highlight?.activeEdgeId === item.id ? 'var(--run-current)' : fullPath && (highlight?.pathEdgeIds.has(item.id) ?? false) ? 'var(--run-path)' : 'var(--graph-edge)'
    },
    data: {
      ...item.data,
      onPath: fullPath && (highlight?.pathEdgeIds.has(item.id) ?? false),
      current: highlight?.activeEdgeId === item.id,
      inspected: inspectedEdgeId === item.id,
      showLabel: viewport.zoom >= 0.72,
      contextDimmed: Boolean(readingContext && !readingContext.edgeIds.has(item.id))
    }
  })));

  async function createLayout(input: GraphModel) {
    const request = ++layoutRequest;
    layoutError = '';
    layoutStatus = 'arranging';
    layoutResult = null;
    layoutNodes = [];
    layoutEdges = [];
    selectedNodeId = null;
    focusNodeId = null;
    viewMode = 'reading';
    searchMessage = '';
    try {
      const layout = await layoutGraph(input);
      if (request !== layoutRequest) return;
      const positions = new Map(layout.nodes.map((node) => [node.id, node]));
      const routes = new Map(layout.edges.map((edge) => [edge.id, edge]));
      layoutNodes = input.nodes.map((node) => {
        const positioned = positions.get(node.id)!;
        return {
          id: node.id, type: 'business', position: { x: positioned.x, y: positioned.y },
          data: {
            node, onPath: false, current: false, dimmed: false, stepNumber: null,
            ports: positioned.ports,
            layoutPosition: { x: positioned.x, y: positioned.y },
            occurrence: positioned.occurrence,
            incomingCount: positioned.incomingCount,
            outgoingCount: positioned.outgoingCount,
            showDetails: true,
            contextDimmed: false
          },
          draggable: false, selectable: true, focusable: true,
          ariaLabel: `${node.kind}: ${node.label}${positioned.occurrence ? `. Occurrence ${positioned.occurrence.index} of ${positioned.occurrence.total}` : ''}. Node ${node.id}`
        } satisfies BusinessFlowNode;
      });
      layoutEdges = input.edges.map((edge) => {
        const route = routes.get(edge.id)!;
        const sourceNode = input.nodes.find((node) => node.id === edge.from)!;
        const targetNode = input.nodes.find((node) => node.id === edge.to)!;
        return {
        id: edge.id, type: 'business', source: edge.from, target: edge.to, label: route.displayLabel || undefined,
        sourceHandle: route.sourcePort.id,
        targetHandle: route.targetPort.id,
        interactionWidth: 14,
        domAttributes: { 'data-source-node': edge.from, 'data-target-node': edge.to },
        data: {
          route: route.points,
          labelPosition: route.labelPosition,
          labelAnchor: route.labelAnchor,
          onPath: false,
          current: false,
          inspected: false,
          showLabel: false,
          alwaysShowLabel: positionedOutgoingCount(input, edge.from) > 1,
          rawOutcome: edge.outcome,
          long: route.long,
          sharedSegmentIds: route.sharedSegmentIds,
          contextDimmed: false
        },
        focusable: true,
        ariaLabel: `${accessibleEdgeLabel(edge.outcome, route.displayLabel)} from ${sourceNode.label} to ${targetNode.label}`
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
      focusNodeId = input.entryNodeIds[0] ?? input.nodes[0]?.id ?? null;
      focusRequest += 1;
      fitRevision += 1;
      requestAnimationFrame(measureSafeArea);
    } catch {
      if (request !== layoutRequest) return;
      layoutError = 'The graph layout could not be created. Check the graph structure and try again.';
      layoutStatus = 'failed';
    }
  }

  function positionedOutgoingCount(input: GraphModel, nodeId: string): number {
    return input.edges.filter((edge) => edge.from === nodeId).length;
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

  $effect(() => { createLayout(graph); });
  $effect(() => {
    const activeNodeId = highlight?.activeNodeId ?? null;
    if (activeNodeId !== previousActiveNodeId) {
      previousActiveNodeId = activeNodeId;
      selectedNodeId = null;
      focusNodeId = activeNodeId ?? graph.entryNodeIds[0] ?? null;
      viewMode = 'reading';
      focusRequest += 1;
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
    ? neighborhoodBounds(graph, layoutResult.nodes, layoutResult.edges, focusNodeId)
    : null);
  let readingFocusBounds = $derived(layoutResult && focusNodeId
    ? focusedNodeBounds(layoutResult.nodes, focusNodeId)
    : null);
  let overviewBounds = $derived<CanvasRect>({ x: 0, y: 0, width: Math.max(1, layoutDecorations.width), height: Math.max(1, layoutDecorations.height) });

  function selectNode(nodeId: string): void {
    selectedNodeId = nodeId;
    focusNodeId = nodeId;
    viewMode = 'reading';
    focusRequest += 1;
    onNodeSelect?.(nodeId);
  }

  function changeViewMode(mode: 'reading' | 'overview'): void {
    viewMode = mode;
    if (mode === 'reading') {
      focusNodeId = selectedNodeId ?? highlight?.activeNodeId ?? graph.entryNodeIds[0] ?? graph.nodes[0]?.id ?? null;
      focusRequest += 1;
    } else {
      fitRevision += 1;
    }
  }

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
    selectNode(match.id);
    const occurrence = exactMatch ? '' : labelMatches.length > 1 ? ` Match 1 of ${labelMatches.length}.` : '';
    searchMessage = `Selected ${match.label}.${occurrence}`;
  }
</script>

<section bind:this={flowPanel} class="flow-panel" class:dense-graph={graph.edges.length / Math.max(1, graph.nodes.length) > 2.5} aria-label={`Business graph: ${graph.label}`}
  aria-busy={layoutStatus === 'arranging'} onfocusin={inspectFocusedEdge} onfocusout={clearFocusedEdge}>
  <div class="canvas-toolbar">
    <label><span class="sr-only">Find a node</span><Search size={15} /><input bind:value={search} onkeydown={(event) => event.key === 'Enter' && selectSearchResult()} placeholder="Find a node" /></label>
    <div class="view-modes" aria-label="Graph view">
      <button type="button" class:active={viewMode === 'reading'} aria-pressed={viewMode === 'reading'} onclick={() => changeViewMode('reading')}><BookOpen size={14} />Reading</button>
      <button type="button" class:active={viewMode === 'overview'} aria-pressed={viewMode === 'overview'} onclick={() => changeViewMode('overview')}><MapIcon size={14} />Overview</button>
    </div>
  </div>
  {#if layoutError}
    <div class="layout-error" role="alert"><TriangleAlert size={20} /><div><strong>Graph layout unavailable</strong><p>{layoutError}</p></div></div>
  {:else}
    <SvelteFlow {nodes} {edges} {nodeTypes} {edgeTypes} bind:viewport minZoom={0.01} maxZoom={1.6}
      deleteKey={null} nodesDraggable={false} nodesConnectable={false}
      onnodeclick={({ node }) => selectNode(node.id)}
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
          activeEdgeId={highlight?.activeEdgeId ?? null}
          pathEdgeIds={fullPath ? highlight?.pathEdgeIds ?? new Set<string>() : new Set<string>()}
          {inspectedEdgeId}
          contextEdgeIds={readingContext?.edgeIds ?? null}
        />
      </ViewportPortal>
      <Controls showLock={false} showFitView={false} />
      {#if graph.nodes.length > 8 && graph.nodes.length <= 100}
        <MiniMap class="business-minimap" width={152} height={112} pannable zoomable nodeColor="var(--muted-foreground)" maskColor="color-mix(in oklch, var(--graph-canvas), transparent 55%)" maskStrokeColor="var(--primary)" maskStrokeWidth={1} />
      {:else if graph.nodes.length > 100}
        <div class="large-graph-guide" aria-label={`${graph.nodes.length}-node graph navigation`}>
          <strong>{graph.nodes.length} nodes</strong>
          <span>Search to jump · Overview shows all</span>
        </div>
      {/if}
      {#if layoutStatus === 'ready' && viewMode === 'overview'}
        <FitGraph revision={fitRevision} bounds={overviewBounds} {safeRect} />
      {:else if layoutStatus === 'ready'}
        <FocusCurrent bounds={readingBounds} focusBounds={readingFocusBounds} {focusNodeId} {safeRect} requestToken={focusRequest + fitRevision} />
      {/if}
    </SvelteFlow>
    {#if layoutStatus === 'arranging'}<GraphLayoutStatus nodeCount={graph.nodes.length} edgeCount={graph.edges.length} />{/if}
  {/if}
</section>
<span class="sr-only" role="status">{searchMessage}</span>

<details class="semantic-node-list">
  <summary>Accessible graph list ({graph.nodes.length} nodes, {graph.edges.length} edges)</summary>
  <h2>Nodes</h2>
  <ol>{#each graph.nodes as node}<li><button onclick={() => selectNode(node.id)}><span>{node.kind.replace('_', ' ')}</span> {node.label} <small>{node.id}</small></button></li>{/each}</ol>
  <h2>Edges</h2>
  <ol>{#each graph.edges as edge}<li>{edge.outcome || 'Continuation'} from {graph.nodes.find((node) => node.id === edge.from)?.label} to {graph.nodes.find((node) => node.id === edge.to)?.label}</li>{/each}</ol>
</details>
{#each layoutDecorations.junctions as junction (junction.id)}
  <span class="sr-only">{junction.incomingEdgeIds.length} routes converge before {graph.nodes.find((node) => node.id === junction.targetNodeId)?.label}.</span>
{/each}

<style>
  .flow-panel { height: 100%; min-height: 360px; position: relative; background: var(--graph-canvas); overflow: hidden; }
  :global(.svelte-flow) { background: var(--graph-canvas); }
  .dense-graph :global(.svelte-flow__background) { opacity: .6; }
  .canvas-toolbar { position: absolute; top: 14px; left: 14px; z-index: 5; display: flex; align-items: center; gap: 8px; }
  label { height: 36px; width: 220px; display: flex; align-items: center; gap: 7px; background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 0 10px; box-shadow: var(--shadow); }
  input { width: 100%; border: 0; outline: 0; color: var(--foreground); background: transparent; font-size: 13px; }
  .view-modes { height: 36px; display: flex; align-items: center; padding: 3px; border: 1px solid var(--border); border-radius: 8px; background: var(--card); box-shadow: var(--shadow); }
  .view-modes button { height: 28px; display: flex; align-items: center; gap: 5px; padding: 0 9px; border: 0; border-radius: 6px; background: transparent; color: var(--muted-foreground); font-size: 11px; font-weight: 700; cursor: pointer; }
  .view-modes button.active { background: var(--secondary); color: var(--foreground); }
  .view-modes button:focus-visible { outline: 2px solid var(--ring); outline-offset: 1px; }
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
  @media (max-width: 640px) {
    .canvas-toolbar { right: 14px; align-items: stretch; flex-direction: column; }
    label { width: auto; }
    .view-modes { align-self: flex-start; }
  }
</style>
