<script lang="ts">
  import { BaseEdge, EdgeLabel, type EdgeProps } from '@xyflow/svelte';
  import type { BusinessFlowEdge } from './flow-types';
  import { conciseEdgeLabel } from './edge-label';
  import { roundedOrthogonalPath } from './edge-route';
  let { id, interactionWidth, label, markerEnd, data }: EdgeProps<BusinessFlowEdge> = $props();
  let edgeData = $derived(data!);
  let path = $derived(roundedOrthogonalPath(edgeData.route));
  let edgeStyle = $derived(`stroke: ${edgeData.current ? 'var(--run-current)' : edgeData.onPath ? 'var(--run-path)' : 'var(--graph-edge)'}; stroke-width: ${edgeData.current ? 2.75 : edgeData.onPath ? 2.1 : 1.25};`);
  let fullLabel = $derived(label ?? '');
  let visibleLabel = $derived(fullLabel && (edgeData.current || edgeData.onPath || edgeData.showLabel) ? conciseEdgeLabel(fullLabel) : '');
</script>

<BaseEdge {id} {path} {markerEnd} {interactionWidth} style={edgeStyle} data-route-edge={id} />
{#if visibleLabel}
  <EdgeLabel x={edgeData.labelPosition.x} y={edgeData.labelPosition.y} class="business-edge-label" title={fullLabel} data-edge-label={id} transparent
    style="max-width: 148px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 2px 7px; border: 1px solid color-mix(in oklch, var(--border), transparent 18%); border-radius: 999px; background: color-mix(in oklch, var(--card), transparent 3%); color: var(--foreground); box-shadow: 0 1px 4px color-mix(in oklch, var(--foreground), transparent 92%); font-size: 10px; font-weight: 700; line-height: 1.35; pointer-events: auto;">
    {visibleLabel}
  </EdgeLabel>
{/if}
