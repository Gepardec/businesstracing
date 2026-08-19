<script lang="ts">
  import ChevronLeft from '@lucide/svelte/icons/chevron-left';
  import ChevronRight from '@lucide/svelte/icons/chevron-right';
  import Route from '@lucide/svelte/icons/route';
  import type { GraphModel } from '$contracts/graph-contract';
  import type { RunModel } from '$contracts/run-contract';
  import Button from '$components/ui/Button.svelte';
  import Badge from '$components/ui/Badge.svelte';
  import { explainObservation } from './run-highlight';
  let { graph, run, activeIndex, fullPath, onSelect, onFullPath }: {
    graph: GraphModel; run: RunModel; activeIndex: number; fullPath: boolean;
    onSelect: (index: number) => void; onFullPath: (enabled: boolean) => void
  } = $props();
</script>

<aside class="run-inspector" aria-label="Run explanation">
  <div class="inspector-header">
    <div><span class="eyebrow">Recorded path</span><h2>{run.observations.length} steps</h2></div>
    <label class="path-toggle"><input type="checkbox" checked={fullPath} onchange={(event) => onFullPath(event.currentTarget.checked)} /><Route size={15} /> Full path</label>
  </div>
  <div class="step-nav" aria-label="Step navigation">
    <Button variant="outline" size="sm" disabled={activeIndex <= 0} onclick={() => onSelect(activeIndex - 1)}><ChevronLeft size={15} /> Previous</Button>
    <span>{Math.min(activeIndex + 1, run.observations.length)} / {run.observations.length}</span>
    <Button variant="outline" size="sm" disabled={activeIndex >= run.observations.length - 1} onclick={() => onSelect(activeIndex + 1)}>Next <ChevronRight size={15} /></Button>
  </div>
  <ol class="steps">
    {#each run.observations as observation, index}
      {@const node = graph.nodes.find((item) => item.id === observation.nodeId)}
      <li class:active={index === activeIndex}>
        <button onclick={() => onSelect(index)} aria-current={index === activeIndex ? 'step' : undefined}>
          <span class="step-number">{observation.sequence}</span>
          <span class="step-content">
            <span class="step-meta"><Badge>{node?.kind.replace('_', ' ') ?? 'UNKNOWN'}</Badge>{#if observation.selectedEdgeId}<span>Branch recorded</span>{/if}</span>
            <strong>{node?.label ?? observation.nodeId}</strong>
            <span>{explainObservation(graph, observation)}</span>
            {#if observation.selectedEdgeId}<code>{observation.selectedEdgeId}</code>{/if}
          </span>
        </button>
      </li>
    {/each}
  </ol>
</aside>

<style>
  .run-inspector { height: 100%; background: var(--card); border-left: 1px solid var(--border); display: flex; flex-direction: column; min-width: 0; }
  .inspector-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 18px 13px; border-bottom: 1px solid var(--border); }
  h2 { margin: 3px 0 0; font-size: 1.15rem; }
  .path-toggle { display: flex; gap: 7px; align-items: center; padding: 7px 9px; background: var(--muted); border-radius: 8px; font-size: .78rem; font-weight: 650; }
  .path-toggle input { accent-color: var(--primary); }
  .step-nav { display: flex; align-items: center; justify-content: space-between; padding: 11px 14px; border-bottom: 1px solid var(--border); color: var(--muted-foreground); font-size: .75rem; }
  .steps { list-style: none; margin: 0; padding: 12px 10px 40px; overflow: auto; }
  li { position: relative; }
  li:not(:last-child)::after { content: ''; position: absolute; left: 20px; top: 47px; bottom: -5px; width: 1px; background: var(--border); }
  li button { width: 100%; display: grid; grid-template-columns: 34px 1fr; gap: 9px; text-align: left; border: 1px solid transparent; background: transparent; color: var(--foreground); border-radius: 9px; padding: 10px 8px; cursor: pointer; }
  li button:hover { background: var(--muted); }
  li.active button { border-color: color-mix(in oklch, var(--run-current), transparent 35%); background: color-mix(in oklch, var(--run-current), transparent 92%); }
  .step-number { width: 27px; height: 27px; display: grid; place-items: center; border-radius: 999px; background: var(--muted); border: 1px solid var(--border); font-weight: 800; font-size: .75rem; z-index: 1; }
  li.active .step-number { background: var(--run-current); color: white; border-color: var(--run-current); }
  .step-content { display: flex; flex-direction: column; gap: 6px; min-width: 0; font-size: .8rem; line-height: 1.35; }
  .step-content > span:not(.step-meta) { color: var(--muted-foreground); }
  .step-meta { display: flex; align-items: center; gap: 7px; color: var(--muted-foreground); font-size: .68rem; }
  code { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: .68rem; color: var(--muted-foreground); }
</style>
