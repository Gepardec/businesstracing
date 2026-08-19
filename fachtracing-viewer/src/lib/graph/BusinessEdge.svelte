<script lang="ts">
  import { BaseEdge, EdgeLabel, getSmoothStepPath, type EdgeProps } from '@xyflow/svelte';
  import type { BusinessFlowEdge } from './flow-types';
  import { conciseEdgeLabel } from './edge-label';
  let { id, interactionWidth, label, markerEnd, sourcePosition, sourceX, sourceY, targetPosition, targetX, targetY, data }: EdgeProps<BusinessFlowEdge> = $props();
  let [path, labelX, labelY] = $derived(getSmoothStepPath({ sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, borderRadius: 8, offset: 22 }));
  let edgeStyle = $derived(`stroke: ${data?.current ? 'var(--run-current)' : data?.onPath ? 'var(--run-path)' : 'var(--graph-edge)'}; stroke-width: ${data?.current ? 3 : data?.onPath ? 2.3 : 1.2};`);
  let fullLabel = $derived(label ?? '');
  let visibleLabel = $derived(fullLabel && (data?.current || data?.onPath || data?.showLabel) ? conciseEdgeLabel(fullLabel) : '');
</script>

<BaseEdge {id} {path} {markerEnd} {interactionWidth} style={edgeStyle} />
{#if visibleLabel}
  <EdgeLabel x={labelX} y={labelY} class="business-edge-label" title={fullLabel} transparent>
    {visibleLabel}
  </EdgeLabel>
{/if}

<style>
  :global(.business-edge-label) { max-width: 168px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 2px 7px; border: 1px solid color-mix(in oklch, var(--border), transparent 18%); border-radius: 999px; background: color-mix(in oklch, var(--card), transparent 6%); color: var(--foreground); box-shadow: 0 1px 4px color-mix(in oklch, var(--foreground), transparent 92%); font-size: 10px; font-weight: 650; line-height: 1.35; pointer-events: auto; }
</style>
