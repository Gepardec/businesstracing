<script lang="ts">
  import Route from '@lucide/svelte/icons/route';
  import type { GraphNarrative } from './graph-presentation';

  let { narrative, sourceNodeCount, sourceEdgeCount, visibleNodeCount, visibleEdgeCount }: {
    narrative: GraphNarrative;
    sourceNodeCount: number;
    sourceEdgeCount: number;
    visibleNodeCount: number;
    visibleEdgeCount: number;
  } = $props();

  let reduced = $derived(visibleNodeCount < sourceNodeCount || visibleEdgeCount < sourceEdgeCount);
</script>

<section class="graph-narrative" aria-labelledby="graph-narrative-title">
  <span class="narrative-icon" aria-hidden="true"><Route size={17} /></span>
  <div class="narrative-copy">
    <span class="eyebrow" id="graph-narrative-title">Business logic at a glance</span>
    <p>{narrative.sentences.join(' ')}</p>
  </div>
  {#if reduced}
    <p class="reduction"><strong>Readable map</strong><span>{visibleNodeCount} of {sourceNodeCount} nodes · {visibleEdgeCount} of {sourceEdgeCount} connections</span></p>
  {/if}
</section>

<style>
  .graph-narrative { grid-column: 1 / -1; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 11px; align-items: start; padding-top: 13px; border-top: 1px solid var(--border); }
  .narrative-icon { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 9px; color: var(--primary); background: color-mix(in oklch, var(--primary), transparent 90%); }
  .narrative-copy { min-width: 0; }
  .narrative-copy p { margin: 3px 0 0; max-width: 1040px; color: var(--foreground); font-size: .83rem; line-height: 1.5; }
  .reduction { align-self: center; display: grid; gap: 1px; margin: 0; text-align: right; white-space: nowrap; }
  .reduction strong { font-size: .72rem; }
  .reduction span { color: var(--muted-foreground); font-size: .68rem; }
  @media (max-width: 760px) {
    .graph-narrative { grid-template-columns: auto minmax(0, 1fr); }
    .reduction { grid-column: 2; text-align: left; white-space: normal; }
  }
</style>
