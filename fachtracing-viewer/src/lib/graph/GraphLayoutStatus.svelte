<script lang="ts">
  import LoaderCircle from '@lucide/svelte/icons/loader-circle';
  let { nodeCount, edgeCount }: { nodeCount: number; edgeCount: number } = $props();
</script>

<div class="layout-status" role="status" aria-live="polite">
  <LoaderCircle class="spinner" size={22} aria-hidden="true" />
  <div>
    <strong>Arranging {nodeCount} nodes</strong>
    <p>{edgeCount} connections · Keeping the interface responsive</p>
  </div>
</div>

<style>
  .layout-status { position: absolute; inset: 0; z-index: 4; display: flex; align-items: center; justify-content: center; gap: 12px; background: color-mix(in oklch, var(--graph-canvas), transparent 5%); color: var(--foreground); }
  :global(.spinner) { color: var(--primary); animation: spin 900ms linear infinite; }
  strong { font-size: 14px; }
  p { margin: 3px 0 0; color: var(--muted-foreground); font-size: 12px; }
  @keyframes spin { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { :global(.spinner) { animation: none; } }
</style>
