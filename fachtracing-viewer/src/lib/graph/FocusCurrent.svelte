<script lang="ts">
  import { useSvelteFlow } from '@xyflow/svelte';
  import { readingViewport, READING_MINIMUM_ZOOM, type CanvasRect } from './graph-viewport';
  let { bounds, focusBounds, focusNodeId, safeRect, requestToken = 0, minimumZoom = READING_MINIMUM_ZOOM }: {
    bounds: CanvasRect | null; focusBounds: CanvasRect | null; focusNodeId: string | null; safeRect: CanvasRect; requestToken?: number; minimumZoom?: number
  } = $props();
  const { setViewport } = useSvelteFlow();
  let previous = '';
  $effect(() => {
    const request = [
      focusNodeId,
      bounds?.x, bounds?.y, bounds?.width, bounds?.height,
      focusBounds?.x, focusBounds?.y, focusBounds?.width, focusBounds?.height,
      safeRect.x, safeRect.y, safeRect.width, safeRect.height,
      requestToken
    ].join(':');
    if (!bounds || !focusBounds || request === previous) return;
    previous = request;
    const frame = requestAnimationFrame(() => void setViewport(readingViewport(bounds, focusBounds, safeRect, minimumZoom, 1.2), { duration: 0 }));
    return () => cancelAnimationFrame(frame);
  });
</script>
