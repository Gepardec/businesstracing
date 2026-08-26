<script lang="ts">
  import { useSvelteFlow } from '@xyflow/svelte';
  import { NODE_HEIGHT, NODE_WIDTH } from './layout-definition';
  let { nodeId, requestToken = 0, minimumZoom = 0 }: { nodeId: string | null; requestToken?: number; minimumZoom?: number } = $props();
  const { getNode, getZoom, setCenter } = useSvelteFlow();
  let previous = '';
  $effect(() => {
    const request = `${nodeId ?? ''}:${requestToken}`;
    if (!nodeId || request === previous) return;
    previous = request;
    const node = getNode(nodeId);
    if (node) void setCenter(node.position.x + NODE_WIDTH / 2, node.position.y + NODE_HEIGHT / 2, { zoom: Math.max(getZoom(), minimumZoom), duration: 0 });
  });
</script>
