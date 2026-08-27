<script lang="ts">
  import { useSvelteFlow } from '@xyflow/svelte';
  import { viewportForBounds, type CanvasRect } from './graph-viewport';
  let { revision, bounds, safeRect }: { revision: number; bounds: CanvasRect; safeRect: CanvasRect } = $props();
  const { setViewport } = useSvelteFlow();

  $effect(() => {
    if (revision < 1) return;
    const frame = requestAnimationFrame(() => void setViewport(viewportForBounds(bounds, safeRect, 0.01, 0.9), { duration: 0 }));
    return () => cancelAnimationFrame(frame);
  });
</script>
