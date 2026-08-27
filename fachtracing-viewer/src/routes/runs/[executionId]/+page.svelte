<script lang="ts">
  import ArrowLeft from '@lucide/svelte/icons/arrow-left';
  import PanelRightOpen from '@lucide/svelte/icons/panel-right-open';
  import type { PageData } from './$types';
  import { Badge } from '$components/ui/badge';
  import { Button } from '$components/ui/button';
  import CopyValue from '$components/ui/CopyValue.svelte';
  import * as Sheet from '$components/ui/sheet';
  import FlowCanvas from '$graph/FlowCanvas.svelte';
  import InspectorResizer from '$runs/InspectorResizer.svelte';
  import RunInspector from '$runs/RunInspector.svelte';
  import { deriveRunHighlight } from '$runs/run-highlight';
  let { data }: { data: PageData } = $props();
  let activeIndex = $state(0);
  let fullPath = $state(true);
  let inspectorOpen = $state(false);
  let inspectorWidth = $state(380);
  let viewportWidth = $state(1_440);
  let highlight = $derived(deriveRunHighlight(data.graph, data.run, activeIndex));
  const statusVariant = $derived<'success' | 'destructive' | 'warning'>(data.run.status === 'SUCCEEDED' ? 'success' : data.run.status === 'FAILED' ? 'destructive' : 'warning');
  const result = $derived(data.run.finalDecision?.displayValue ?? data.run.failure?.displayValue ?? 'No final result');
  function selectNode(nodeId: string) {
    const index = data.run.observations.findIndex((item, itemIndex) => item.nodeId === nodeId && itemIndex >= activeIndex);
    const fallback = data.run.observations.findIndex((item) => item.nodeId === nodeId);
    if (index >= 0 || fallback >= 0) {
      activeIndex = index >= 0 ? index : fallback;
      if (viewportWidth < 1_024) inspectorOpen = true;
    }
  }

  $effect(() => { if (viewportWidth >= 1_024) inspectorOpen = false; });
</script>

<svelte:window bind:innerWidth={viewportWidth} />
<svelte:head><title>{data.graph.label} · Fachtracing</title></svelte:head>
<div class="decision-page">
  <header class="decision-header">
    <div class="header-topline">
      <a href="/runs" class="back-link"><ArrowLeft size={16} /> Decisions</a>
      <div class="header-actions">
        <Badge variant={statusVariant}>{data.run.status.toLowerCase()}</Badge>
        <div class="mobile-explanation">
          <Sheet.Root bind:open={inspectorOpen}>
            <Sheet.Trigger>
              {#snippet child({ props })}<Button {...props} variant="outline" size="sm" aria-label="Open run explanation"><PanelRightOpen data-icon="inline-start" /> Explanation <span class="step-count">{data.run.observations.length}</span></Button>{/snippet}
            </Sheet.Trigger>
            <Sheet.Content class="run-sheet w-[min(430px,94vw)] gap-0 p-0 sm:max-w-[430px]">
              <Sheet.Header class="sr-only"><Sheet.Title>Run explanation</Sheet.Title><Sheet.Description>Recorded steps and evidence for this decision</Sheet.Description></Sheet.Header>
              <RunInspector graph={data.graph} run={data.run} {activeIndex} {fullPath}
                onSelect={(index) => activeIndex = index} onFullPath={(enabled) => fullPath = enabled} />
            </Sheet.Content>
          </Sheet.Root>
        </div>
      </div>
    </div>
    <div class="decision-title"><span class="eyebrow">Decision</span><h1>{data.graph.label}</h1></div>
    <dl>
      <div><dt>Final result</dt><dd><CopyValue value={result} label="final result" /></dd></div>
      <div><dt>Completed</dt><dd>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(data.run.completedAt))}</dd></div>
      <div><dt>Execution</dt><dd><CopyValue value={data.run.executionId} mono label="execution ID" /></dd></div>
      <div><dt>Graph</dt><dd><CopyValue value={`${data.run.graphId}@${data.run.graphVersion}`} mono label="graph ID and version" /></dd></div>
    </dl>
  </header>
  <div class="decision-workspace" style={`--inspector-width: ${inspectorWidth}px`}>
    <div class="canvas"><FlowCanvas graph={data.graph} {highlight} {fullPath} onNodeSelect={selectNode} /></div>
    <InspectorResizer value={inspectorWidth} onResize={(value) => inspectorWidth = value} />
    <div class="desktop-inspector"><RunInspector graph={data.graph} run={data.run} {activeIndex} {fullPath}
      onSelect={(index) => activeIndex = index} onFullPath={(enabled) => fullPath = enabled} /></div>
  </div>
</div>

<style>
  .decision-page { height: calc(100dvh - 58px); display: grid; grid-template-rows: auto minmax(0, 1fr); overflow: hidden; }
  .decision-header { padding: 11px 20px 12px; border-bottom: 1px solid var(--border); background: var(--card); }
  .header-topline { min-height: 32px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
  .header-actions { display: flex; align-items: center; gap: 9px; }
  .back-link { display: inline-flex; align-items: center; gap: 5px; color: var(--muted-foreground); text-decoration: none; font-size: .76rem; font-weight: 650; }
  .decision-title { min-width: 0; display: flex; align-items: baseline; gap: 10px; margin-top: 3px; }
  h1 { margin: 1px 0 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: clamp(1.18rem, 2vw, 1.48rem); letter-spacing: -.035em; }
  dl { display: grid; grid-template-columns: minmax(180px, 1.35fr) .65fr 1fr 1fr; gap: clamp(12px, 2vw, 26px); margin: 9px 0 0; }
  dt { color: var(--muted-foreground); font-size: .67rem; text-transform: uppercase; letter-spacing: .08em; font-weight: 700; }
  dl > div, dd { min-width: 0; overflow: hidden; }
  dd { margin: 2px 0 0; font-size: .78rem; font-weight: 620; }
  .decision-workspace { display: grid; grid-template-columns: minmax(0, 1fr) 8px var(--inspector-width); min-height: 0; }
  .canvas, .desktop-inspector { min-height: 0; position: relative; }
  .mobile-explanation { display: none; }
  .step-count { min-width: 19px; height: 19px; display: inline-grid; place-items: center; padding: 0 5px; border-radius: 999px; background: var(--muted); color: var(--muted-foreground); font-size: .68rem; }
  :global(.run-sheet .run-inspector) { border-left: 0; }
  :global(.run-sheet .inspector-header) { padding-right: 58px; }
  @media (max-width: 1023px) {
    .decision-workspace { display: block; }
    .canvas { height: 100%; min-height: 0; }
    .desktop-inspector, :global(.inspector-resizer) { display: none; }
    .mobile-explanation { display: block; }
    dl { grid-template-columns: 1.25fr .75fr 1fr; }
    dl > div:last-child { display: none; }
  }
  @media (max-width: 620px) {
    .decision-header { padding-inline: 14px; }
    .decision-title { display: block; }
    h1 { white-space: normal; display: -webkit-box; line-clamp: 2; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
    dl { grid-template-columns: 1fr 1fr; gap: 7px 14px; }
    dl > div:nth-child(3) { grid-column: 1 / -1; }
  }
</style>
