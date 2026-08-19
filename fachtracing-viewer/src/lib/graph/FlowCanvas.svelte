<script lang="ts">
  import { Background, BackgroundVariant, Controls, MarkerType, MiniMap, SvelteFlow, type EdgeTypes, type NodeTypes, type Viewport } from '@xyflow/svelte';
  import Search from '@lucide/svelte/icons/search';
  import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
  import { onMount } from 'svelte';
  import type { GraphModel } from '$contracts/graph-contract';
  import type { RunHighlight } from '$runs/run-highlight';
  import { layoutGraph } from './layout-client';
  import BusinessNode from './BusinessNode.svelte';
  import BusinessEdge from './BusinessEdge.svelte';
  import FitGraph from './FitGraph.svelte';
  import FocusCurrent from './FocusCurrent.svelte';
  import type { BusinessFlowEdge, BusinessFlowNode } from './flow-types';

  let { graph, highlight, fullPath = true, onNodeSelect }: {
    graph: GraphModel; highlight: RunHighlight | null; fullPath?: boolean; onNodeSelect?: (nodeId: string) => void
  } = $props();
  let layoutNodes = $state<BusinessFlowNode[]>([]);
  let layoutEdges = $state<BusinessFlowEdge[]>([]);
  let layoutError = $state('');
  let search = $state('');
  let viewport = $state<Viewport>({ x: 0, y: 0, zoom: 1 });
  let fitRevision = $state(0);
  let searchFocusId = $state<string | null>(null);
  let focusRequest = $state(0);
  let previousActiveNodeId: string | null = null;
  let searchMessage = $state('');
  let flowPanel: HTMLElement;
  let layoutRequest = 0;
  const nodeTypes: NodeTypes = { business: BusinessNode };
  const edgeTypes: EdgeTypes = { business: BusinessEdge };

  function decorateNode(id: string) {
    const current = highlight?.activeNodeId === id;
    const onPath = fullPath && (highlight?.pathNodeIds.has(id) ?? false);
    return { current, onPath, dimmed: fullPath && Boolean(highlight) && !onPath, stepNumber: current ? highlight?.activeStepNumber ?? null : null };
  }

  let nodes = $derived(layoutNodes.map((item) => ({
    ...item,
    data: { ...item.data, ...decorateNode(item.id) }
  })));
  let edges = $derived(layoutEdges.map((item) => ({
    ...item,
    data: {
      onPath: fullPath && (highlight?.pathEdgeIds.has(item.id) ?? false),
      current: highlight?.activeEdgeId === item.id,
      showLabel: viewport.zoom >= 0.92
    }
  })));

  async function createLayout(input: GraphModel) {
    const request = ++layoutRequest;
    layoutError = '';
    try {
      const layout = await layoutGraph(input);
      if (request !== layoutRequest) return;
      const positions = new Map(layout.nodes.map((node) => [node.id, node]));
      layoutNodes = input.nodes.map((node) => ({
        id: node.id, type: 'business', position: positions.get(node.id) ?? { x: 0, y: 0 },
        data: { node, onPath: false, current: false, dimmed: false, stepNumber: null }, draggable: false, selectable: true, focusable: true,
        ariaLabel: `${node.kind}: ${node.label}`
      }));
      layoutEdges = input.edges.map((edge) => ({
        id: edge.id, type: 'business', source: edge.from, target: edge.to, label: edge.outcome || undefined,
        markerEnd: { type: MarkerType.ArrowClosed },
        data: { onPath: false, current: false, showLabel: false },
        focusable: true, ariaLabel: `${edge.outcome || 'next'} from ${edge.from} to ${edge.to}`
      }));
      fitRevision += 1;
    } catch (error) {
      if (request !== layoutRequest) return;
      layoutError = error instanceof Error ? error.message : 'The graph layout could not be created.';
    }
  }

  $effect(() => { createLayout(graph); });
  $effect(() => {
    const activeNodeId = highlight?.activeNodeId ?? null;
    if (activeNodeId !== previousActiveNodeId) {
      previousActiveNodeId = activeNodeId;
      searchFocusId = null;
      focusRequest += 1;
    }
  });

  onMount(() => {
    let frame = 0;
    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => fitRevision += 1);
    });
    observer.observe(flowPanel);
    return () => { cancelAnimationFrame(frame); observer.disconnect(); };
  });

  function selectSearchResult() {
    const normalized = search.trim().toLowerCase();
    const match = graph.nodes.find((node) => node.id.toLowerCase() === normalized || node.label.toLowerCase().includes(normalized));
    if (!match) {
      searchMessage = normalized ? 'No matching node was found.' : 'Enter a node name or ID.';
      return;
    }
    searchFocusId = match.id;
    focusRequest += 1;
    searchMessage = `Focused ${match.label}.`;
    onNodeSelect?.(match.id);
  }
</script>

<section bind:this={flowPanel} class="flow-panel" aria-label={`Business graph: ${graph.label}`}>
  <div class="canvas-toolbar">
    <label><span class="sr-only">Find a node</span><Search size={15} /><input bind:value={search} onkeydown={(event) => event.key === 'Enter' && selectSearchResult()} placeholder="Find a node" /></label>
  </div>
  {#if layoutError}
    <div class="layout-error" role="alert"><TriangleAlert size={20} /><div><strong>Graph layout unavailable</strong><p>{layoutError}</p></div></div>
  {:else}
    <SvelteFlow {nodes} {edges} {nodeTypes} {edgeTypes} bind:viewport fitView fitViewOptions={{ padding: 0.18, maxZoom: 0.9 }} minZoom={0.01} maxZoom={1.6}
      deleteKey={null} nodesDraggable={false} nodesConnectable={false}
      onnodeclick={({ node }) => onNodeSelect?.(node.id)}>
      <Background variant={BackgroundVariant.Dots} patternColor="var(--graph-grid)" gap={22} size={1.2} />
      <Controls showLock={false} fitViewOptions={{ padding: 0.18, maxZoom: 0.9 }} />
      {#if graph.nodes.length > 8 && graph.nodes.length <= 100}
        <MiniMap class="business-minimap" width={152} height={112} pannable zoomable nodeColor="var(--muted-foreground)" maskColor="color-mix(in oklch, var(--graph-canvas), transparent 55%)" maskStrokeColor="var(--primary)" maskStrokeWidth={1} />
      {:else if graph.nodes.length > 100}
        <div class="large-graph-guide" aria-label={`${graph.nodes.length}-node graph navigation`}>
          <strong>{graph.nodes.length} nodes</strong>
          <span>Search to jump · Fit to overview</span>
        </div>
      {/if}
      <FitGraph revision={fitRevision} />
      <FocusCurrent nodeId={searchFocusId ?? highlight?.activeNodeId ?? null} requestToken={focusRequest + fitRevision} minimumZoom={searchFocusId ? 0.75 : 0} />
    </SvelteFlow>
  {/if}
</section>
<span class="sr-only" role="status">{searchMessage}</span>

<details class="semantic-node-list">
  <summary>Accessible node list ({graph.nodes.length})</summary>
  <ol>{#each graph.nodes as node}<li><button onclick={() => onNodeSelect?.(node.id)}><span>{node.kind.replace('_', ' ')}</span> {node.label}</button></li>{/each}</ol>
</details>

<style>
  .flow-panel { height: 100%; min-height: 360px; position: relative; background: var(--graph-canvas); overflow: hidden; }
  :global(.svelte-flow) { background: var(--graph-canvas); }
  .canvas-toolbar { position: absolute; top: 14px; left: 14px; z-index: 5; }
  label { height: 36px; width: 220px; display: flex; align-items: center; gap: 7px; background: var(--card); border: 1px solid var(--border); border-radius: 8px; padding: 0 10px; box-shadow: var(--shadow); }
  input { width: 100%; border: 0; outline: 0; color: var(--foreground); background: transparent; font-size: 13px; }
  .layout-error { height: 100%; display: flex; justify-content: center; align-items: center; gap: 12px; color: var(--status-failure); }
  .layout-error p { color: var(--muted-foreground); margin: 4px 0 0; }
  .semantic-node-list { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  .semantic-node-list:focus-within { position: fixed; width: min(540px, 90vw); height: auto; clip: auto; overflow: auto; z-index: 100; left: 20px; bottom: 20px; padding: 16px; background: var(--card); border: 1px solid var(--border); border-radius: 10px; }
  .semantic-node-list button { border: 0; background: none; color: inherit; text-align: left; padding: 4px; }
  .semantic-node-list span { font-weight: 700; }
  .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
  :global(.svelte-flow__controls) { box-shadow: var(--shadow); overflow: hidden; border: 1px solid var(--border); border-radius: 9px; }
  :global(.svelte-flow__controls-button) { width: 32px; height: 32px; border-bottom-color: var(--border); background: color-mix(in oklch, var(--card), transparent 3%); color: var(--foreground); }
  :global(.business-minimap) { overflow: hidden; border: 1px solid var(--border); border-radius: 9px; background: color-mix(in oklch, var(--card), transparent 3%); box-shadow: var(--shadow); }
  .large-graph-guide { position: absolute; right: 14px; bottom: 14px; z-index: 5; display: grid; gap: 2px; min-width: 174px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 9px; background: color-mix(in oklch, var(--card), transparent 3%); box-shadow: var(--shadow); }
  .large-graph-guide strong { font-size: 12px; }
  .large-graph-guide span { color: var(--muted-foreground); font-size: 10px; }
</style>
