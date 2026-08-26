<script lang="ts">
  import { useSvelteFlow } from '@xyflow/svelte';
  let { revision }: { revision: number } = $props();
  const { fitView } = useSvelteFlow();

  $effect(() => {
    if (revision < 1) return;
    const frame = requestAnimationFrame(() => void fitView({ padding: 0.18, maxZoom: 0.9, duration: 0 }));
    return () => cancelAnimationFrame(frame);
  });
</script>
