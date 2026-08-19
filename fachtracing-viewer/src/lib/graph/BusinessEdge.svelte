<script lang="ts">
  import { BaseEdge, getSmoothStepPath, type EdgeProps } from '@xyflow/svelte';
  import type { BusinessFlowEdge } from './flow-types';
  let { id, interactionWidth, label, markerEnd, sourcePosition, sourceX, sourceY, targetPosition, targetX, targetY, data }: EdgeProps<BusinessFlowEdge> = $props();
  let [path, labelX, labelY] = $derived(getSmoothStepPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, borderRadius: 8, offset: 22 }));
  let edgeStyle = $derived(`stroke: ${data?.current ? 'var(--run-current)' : data?.onPath ? 'var(--run-path)' : 'var(--graph-edge)'}; stroke-width: ${data?.current ? 3 : data?.onPath ? 2.3 : 1.2};`);
</script>

<BaseEdge {id} {path} {labelX} {labelY} {label} {markerEnd} {interactionWidth} style={edgeStyle} />
