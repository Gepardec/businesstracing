<script lang="ts">
  import { BaseEdge, EdgeLabel, type EdgeProps } from '@xyflow/svelte';
  import type { BusinessFlowEdge } from './flow-types';
  import { roundedOrthogonalPath } from './edge-route';
  let { id, interactionWidth, label, markerEnd, data }: EdgeProps<BusinessFlowEdge> = $props();
  let edgeData = $derived(data!);
  let path = $derived(roundedOrthogonalPath(edgeData.route));
  let leaderLength = $derived(Math.hypot(edgeData.labelPosition.x - edgeData.labelAnchor.x, edgeData.labelPosition.y - edgeData.labelAnchor.y));
  let leaderPath = $derived(`M ${edgeData.labelAnchor.x} ${edgeData.labelAnchor.y} L ${edgeData.labelPosition.x} ${edgeData.labelPosition.y}`);
  let edgeStyle = $derived(`stroke: ${edgeData.current ? 'var(--run-current)' : edgeData.onPath ? 'var(--run-path)' : edgeData.inspected ? 'var(--primary)' : 'var(--graph-edge)'}; stroke-width: ${edgeData.current ? 2.75 : edgeData.onPath || edgeData.inspected ? 2.1 : 1.5}; opacity: ${edgeData.contextDimmed ? 0.1 : edgeData.long && !edgeData.current && !edgeData.onPath && !edgeData.inspected ? 0.72 : 1};`);
  let visibleLabel = $derived(label && !edgeData.contextDimmed && (edgeData.alwaysShowLabel || edgeData.current || edgeData.onPath || edgeData.showLabel) ? label : '');
</script>

<BaseEdge {id} {path} markerEnd={edgeData.sharedSegmentIds.length > 0 ? undefined : markerEnd} {interactionWidth} style={edgeStyle} data-route-edge={id} class={edgeData.long ? 'long-route' : undefined} />
{#if visibleLabel}
  {#if leaderLength > 32}<path class="business-edge-label-leader" d={leaderPath} aria-hidden="true" />{/if}
  <EdgeLabel x={edgeData.labelPosition.x} y={edgeData.labelPosition.y} class="business-edge-label" title={edgeData.rawOutcome} data-edge-label={id} transparent
    style="max-width: 148px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 2px 7px; border: 1px solid color-mix(in oklch, var(--border), transparent 18%); border-radius: 999px; background: color-mix(in oklch, var(--card), transparent 3%); color: var(--foreground); box-shadow: 0 1px 4px color-mix(in oklch, var(--foreground), transparent 92%); font-size: 10px; font-weight: 700; line-height: 1.35; pointer-events: auto;">
    {visibleLabel}
  </EdgeLabel>
{/if}

<style>
  .business-edge-label-leader { fill: none; stroke: color-mix(in oklch, var(--graph-edge), transparent 25%); stroke-width: 1; stroke-dasharray: 3 3; pointer-events: none; }
</style>
