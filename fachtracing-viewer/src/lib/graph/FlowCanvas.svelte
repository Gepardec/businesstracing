<script lang="ts">
  import { Background, BackgroundVariant, Controls, MarkerType, MiniMap, SvelteFlow, type EdgeTypes, type NodeTypes } from '@xyflow/svelte';
  import Search from '@lucide/svelte/icons/search';
  import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
  import type { GraphModel } from '$contracts/graph-contract';
  import type { RunHighlight } from '$runs/run-highlight';
  import { layoutGraph } from './layout-client';
  import BusinessNode from './BusinessNode.svelte';
  import BusinessEdge from './BusinessEdge.svelte';
  import FocusCurrent from './FocusCurrent.svelte';
  import type { BusinessFlowEdge, BusinessFlowNode } from './flow-types';

  let { graph, highlight, fullPath = true, onNodeSelect }: {
    graph: GraphModel; highlight: RunHighlight | null; fullPath?: boolean; onNodeSelect?: (nodeId: string) => void
  } = $props();
  let layoutNodes = $state<BusinessFlowNode[]>([]);
  let layoutEdges = $state<BusinessFlowEdge[]>([]);
  let layoutError = $state('');
  let search = $state('');
  let layoutRequest = 0;
  const nodeTypes: NodeTypes = { business: BusinessNode };
  const edgeTypes: EdgeTypes = { business: BusinessEdge };

  function decorateNode(id: string) {
    const current = highlight?.activeNodeId === id;
    const onPath = fullPath && (highlight?.pathNodeIds.has(id) ?? false);
    return { current, onPath, dimmed: fullPath && Boolean(highlight) && !onPath, sequence: current ? highlight?.activeSequence ?? null : null };
  }

  let nodes = $derived(layoutNodes.map((item) => ({
    ...item,
    data: { ...item.data, ...decorateNode(item.id) }
  })));
  let edges = $derived(layoutEdges.map((item) => ({
    ...item,
    data: {
      onPath: fullPath && (highlight?.pathEdgeIds.has(item.id) ?? false),
      current: highlight?.activeEdgeId === item.id
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
        data: { node, onPath: false, current: false, dimmed: false, sequence: null }, draggable: false, selectable: true, focusable: true,
        ariaLabel: `${node.kind}: ${node.label}`
      }));
      layoutEdges = input.edges.map((edge) => ({
        id: edge.id, type: 'business', source: edge.from, target: edge.to, label: edge.outcome || undefined,
        markerEnd: { type: MarkerType.ArrowClosed },
        data: { onPath: false, current: false },
        focusable: true, ariaLabel: `${edge.outcome || 'next'} from ${edge.from} to ${edge.to}`
      }));
    } catch (error) {
      if (request !== layoutRequest) return;
      layoutError = error instanceof Error ? error.message : 'The graph layout could not be created.';
    }
  }

  $effect(() => { createLayout(graph); });

  function selectSearchResult() {
    const normalized = search.trim().toLowerCase();
    const match = graph.nodes.find((node) => node.id.toLowerCase() === normalized || node.label.toLowerCase().includes(normalized));
    if (match) onNodeSelect?.(match.id);
  }
</script>

<section class="flow-panel" aria-label={`Business graph: ${graph.label}`}>
  <div class="canvas-toolbar">
    <label><span class="sr-only">Find a node</span><Search size={15} /><input bind:value={search} onkeydown={(event) => event.key === 'Enter' && selectSearchResult()} placeholder="Find a node" /></label>
  </div>
  {#if layoutError}
    <div class="layout-error" role="alert"><TriangleAlert size={20} /><div><strong>Graph layout unavailable</strong><p>{layoutError}</p></div></div>
  {:else}
    <SvelteFlow {nodes} {edges} {nodeTypes} {edgeTypes} fitView fitViewOptions={{ padding: 0.16 }} minZoom={0.12} maxZoom={1.8}
      deleteKey={null} nodesDraggable={false} nodesConnectable={false}
      onnodeclick={({ node }) => onNodeSelect?.(node.id)}>
      <Background variant={BackgroundVariant.Dots} patternColor="var(--graph-grid)" gap={22} size={1.2} />
      <Controls showLock={false} />
      <MiniMap pannable zoomable nodeColor="var(--node-computation)" maskColor="color-mix(in oklch, var(--graph-canvas), transparent 20%)" />
      <FocusCurrent nodeId={highlight?.activeNodeId ?? null} />
    </SvelteFlow>
  {/if}
</section>

<details class="semantic-node-list">
  <summary>Accessible node list ({graph.nodes.length})</summary>
  <ol>{#each graph.nodes as node}<li><button onclick={() => onNodeSelect?.(node.id)}><span>{node.kind.replace('_', ' ')}</span> {node.label}</button></li>{/each}</ol>
</details>

<style>
  .flow-panel { height: 100%; min-height: 560px; position: relative; background: var(--graph-canvas); overflow: hidden; }
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
</style>
