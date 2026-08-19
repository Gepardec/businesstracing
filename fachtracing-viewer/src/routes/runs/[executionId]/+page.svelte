<script lang="ts">
  import ArrowLeft from '@lucide/svelte/icons/arrow-left';
  import PanelRightOpen from '@lucide/svelte/icons/panel-right-open';
  import type { PageData } from './$types';
  import Badge from '$components/ui/Badge.svelte';
  import Button from '$components/ui/Button.svelte';
  import FlowCanvas from '$graph/FlowCanvas.svelte';
  import RunInspector from '$runs/RunInspector.svelte';
  import { deriveRunHighlight } from '$runs/run-highlight';
  let { data }: { data: PageData } = $props();
  let activeIndex = $state(0);
  let fullPath = $state(true);
  let inspectorOpen = $state(false);
  let highlight = $derived(deriveRunHighlight(data.graph, data.run, activeIndex));
  const tone = $derived(data.run.status === 'SUCCEEDED' ? 'success' : data.run.status === 'FAILED' ? 'danger' : 'warning');
  function selectNode(nodeId: string) {
    const index = data.run.observations.findIndex((item, itemIndex) => item.nodeId === nodeId && itemIndex >= activeIndex);
    const fallback = data.run.observations.findIndex((item) => item.nodeId === nodeId);
    if (index >= 0 || fallback >= 0) activeIndex = index >= 0 ? index : fallback;
  }
</script>

<svelte:head><title>{data.graph.label} · Fachtracing</title></svelte:head>
<div class="decision-page">
  <header class="decision-header">
    <a href="/runs" class="back-link"><ArrowLeft size={16} /> Decisions</a>
    <div class="decision-title"><div><span class="eyebrow">Decision explanation</span><h1>{data.graph.label}</h1></div><Badge tone={tone}>{data.run.status}</Badge></div>
    <dl>
      <div><dt>Final result</dt><dd>{data.run.finalDecision?.displayValue ?? data.run.failure?.displayValue ?? 'No final result'}</dd></div>
      <div><dt>Completed</dt><dd>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(data.run.completedAt))}</dd></div>
      <div><dt>Execution</dt><dd class="mono">{data.run.executionId}</dd></div>
      <div><dt>Graph</dt><dd class="mono">{data.run.graphId}@{data.run.graphVersion}</dd></div>
    </dl>
    <Button class="mobile-inspector" variant="outline" onclick={() => inspectorOpen = !inspectorOpen}><PanelRightOpen size={16} /> Explanation</Button>
  </header>
  <div class="decision-workspace">
    <div class="canvas"><FlowCanvas graph={data.graph} {highlight} {fullPath} onNodeSelect={selectNode} /></div>
    <div class:open={inspectorOpen} class="inspector"><RunInspector graph={data.graph} run={data.run} {activeIndex} {fullPath}
      onSelect={(index) => { activeIndex = index; inspectorOpen = false; }} onFullPath={(enabled) => fullPath = enabled} /></div>
  </div>
</div>

<style>
  .decision-page { height: calc(100vh - 58px); display: grid; grid-template-rows: auto minmax(0, 1fr); }
  .decision-header { padding: 13px 20px 14px; border-bottom: 1px solid var(--border); background: var(--card); position: relative; }
  .back-link { display: inline-flex; align-items: center; gap: 5px; color: var(--muted-foreground); text-decoration: none; font-size: .76rem; font-weight: 650; }
  .decision-title { display: flex; align-items: center; gap: 12px; margin-top: 8px; }
  h1 { margin: 2px 0 0; font-size: 1.45rem; letter-spacing: -.035em; }
  dl { display: grid; grid-template-columns: minmax(180px, 1.2fr) repeat(3, minmax(130px, 1fr)); gap: 20px; margin: 13px 0 0; }
  dt { color: var(--muted-foreground); font-size: .67rem; text-transform: uppercase; letter-spacing: .08em; font-weight: 700; }
  dd { margin: 3px 0 0; font-size: .78rem; font-weight: 620; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .decision-workspace { display: grid; grid-template-columns: minmax(0, 1fr) 380px; min-height: 0; }
  .canvas, .inspector { min-height: 0; position: relative; }
  :global(.mobile-inspector) { display: none; }
  @media (max-width: 1199px) {
    .decision-page { height: auto; min-height: calc(100vh - 58px); }
    .decision-workspace { display: block; min-height: 650px; }
    .canvas { height: calc(100vh - 230px); min-height: 590px; }
    .inspector { position: fixed; z-index: 45; top: 58px; right: 0; bottom: 0; width: min(420px, 92vw); transform: translateX(105%); transition: transform 160ms; box-shadow: var(--shadow); }
    .inspector.open { transform: translateX(0); }
    :global(.mobile-inspector) { display: inline-flex; position: absolute; right: 18px; top: 17px; }
    dl { grid-template-columns: repeat(2, minmax(0, 1fr)); padding-right: 0; }
  }
  @media (max-width: 620px) { dl { grid-template-columns: 1fr; gap: 8px; } .canvas { height: 620px; } }
</style>
