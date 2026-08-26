<script lang="ts">
  import { BaseEdge, EdgeLabel, type EdgeProps } from '@xyflow/svelte';
  import type { BusinessFlowEdge } from './flow-types';
  import { roundedOrthogonalPath } from './edge-route';
  import { isQuietReference, shouldShowEdgeLabel } from './edge-presentation';
  let { id, interactionWidth, label, markerEnd, data }: EdgeProps<BusinessFlowEdge> = $props();
  let edgeData = $derived(data!);
  let path = $derived(roundedOrthogonalPath(edgeData.route));
  let quietReference = $derived(isQuietReference(edgeData));
  let edgeStyle = $derived(`stroke: ${edgeData.current ? 'var(--run-current)' : edgeData.onPath ? 'var(--run-path)' : edgeData.inspected ? 'var(--primary)' : 'var(--graph-edge)'}; stroke-width: ${edgeData.current ? 2.75 : edgeData.onPath || edgeData.inspected ? 2.1 : quietReference ? 1 : 1.65}; opacity: ${quietReference ? 0.22 : 1}; stroke-dasharray: ${quietReference ? '4 5' : 'none'};`);
  let visibleLabel = $derived(label ?? '');
</script>

<BaseEdge {id} {path} markerEnd={edgeData.sharedSegmentIds.length > 0 ? undefined : markerEnd} {interactionWidth} style={edgeStyle} data-route-edge={id} data-secondary={edgeData.secondary ? 'true' : 'false'} data-branch={edgeData.branch ? 'true' : 'false'} class={[edgeData.long && 'long-route', quietReference && 'secondary-route'].filter(Boolean).join(' ') || undefined} />
{#if shouldShowEdgeLabel(visibleLabel, edgeData)}
  <EdgeLabel x={edgeData.labelPosition.x} y={edgeData.labelPosition.y} class="business-edge-label" title={edgeData.rawOutcome} data-edge-label={id} transparent
    style="max-width: 148px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 2px 7px; border: 1px solid color-mix(in oklch, var(--border), transparent 18%); border-radius: 999px; background: color-mix(in oklch, var(--card), transparent 3%); color: var(--foreground); box-shadow: 0 1px 4px color-mix(in oklch, var(--foreground), transparent 92%); font-size: 10px; font-weight: 700; line-height: 1.35; pointer-events: auto;">
    {visibleLabel}
  </EdgeLabel>
{/if}
