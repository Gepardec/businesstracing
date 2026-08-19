<script lang="ts">
  import { useSvelteFlow } from '@xyflow/svelte';
  import { NODE_HEIGHT, NODE_WIDTH } from './layout-engine';
  let { nodeId }: { nodeId: string | null } = $props();
  const { getNode, getZoom, setCenter } = useSvelteFlow();
  let previous = '';
  $effect(() => {
    if (!nodeId || nodeId === previous) return;
    previous = nodeId;
    const node = getNode(nodeId);
    if (node) void setCenter(node.position.x + NODE_WIDTH / 2, node.position.y + NODE_HEIGHT / 2, { zoom: getZoom(), duration: 0 });
  });
</script>
