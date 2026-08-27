<script lang="ts">
  import type { PositionedRegion } from './layout-definition';
  import { roundedOrthogonalPath } from './edge-route';
  import type { LayoutJunction, RouteCrossing, SharedRouteSegment } from './route-planner';

  let { junctions, sharedSegments, crossings, regions, width, height, activeEdgeId, pathEdgeIds, inspectedEdgeId }: {
    junctions: readonly LayoutJunction[];
    sharedSegments: readonly SharedRouteSegment[];
    crossings: readonly RouteCrossing[];
    regions: readonly PositionedRegion[];
    width: number;
    height: number;
    activeEdgeId: string | null;
    pathEdgeIds: ReadonlySet<string>;
    inspectedEdgeId: string | null;
  } = $props();

  function state(edgeIds: readonly string[]): 'current' | 'path' | 'inspect' | 'default' {
    if (activeEdgeId && edgeIds.includes(activeEdgeId)) return 'current';
    if (edgeIds.some((edgeId) => pathEdgeIds.has(edgeId))) return 'path';
    if (inspectedEdgeId && edgeIds.includes(inspectedEdgeId)) return 'inspect';
    return 'default';
  }

  function crossingState(crossing: RouteCrossing): 'current' | 'path' | 'inspect' | 'default' {
    return state([crossing.overEdgeId]);
  }

</script>

<svg class="graph-decorations" {width} {height} viewBox={`0 0 ${width} ${height}`} aria-hidden="true">
  {#each regions as region (region.id)}
    <g class="structural-region">
      <rect x={region.x} y={region.y} width={region.width} height={region.height} rx="16" />
      <text x={region.x + 12} y={region.y + 17}>{region.label}</text>
    </g>
  {/each}
  {#each sharedSegments as segment (segment.id)}
    <g class="shared-segment shared-segment--{state(segment.incomingEdgeIds)}" data-shared-segment={segment.id} data-target-node={segment.targetNodeId} data-member-count={segment.incomingEdgeIds.length}>
      <path d={roundedOrthogonalPath(segment.lanePoints, 6)} />
      <path d={roundedOrthogonalPath(segment.points, 6)} />
      <polygon points={`${segment.points.at(-1)!.x},${segment.points.at(-1)!.y} ${segment.points.at(-1)!.x - 5},${segment.points.at(-1)!.y - 9} ${segment.points.at(-1)!.x + 5},${segment.points.at(-1)!.y - 9}`} />
    </g>
  {/each}
  {#each junctions as junction (junction.id)}
    <g class="junction junction--{state(junction.incomingEdgeIds)}" data-junction={junction.id} data-member-count={junction.incomingEdgeIds.length}>
      <circle cx={junction.point.x} cy={junction.point.y} r="5" />
      <title>{junction.incomingEdgeIds.length} routes converge before this node</title>
    </g>
  {/each}
  {#each crossings as crossing (crossing.id)}
    <g class="route-crossing route-crossing--{crossingState(crossing)}" data-route-crossing={crossing.id}>
      <circle cx={crossing.point.x} cy={crossing.point.y} r={crossing.radius + 2} />
      {#if crossing.overAxis === 'horizontal'}
        <line x1={crossing.point.x - crossing.radius - 2} x2={crossing.point.x + crossing.radius + 2} y1={crossing.point.y} y2={crossing.point.y} />
      {:else}
        <line x1={crossing.point.x} x2={crossing.point.x} y1={crossing.point.y - crossing.radius - 2} y2={crossing.point.y + crossing.radius + 2} />
      {/if}
    </g>
  {/each}
</svg>

<style>
  .graph-decorations { position: absolute; inset: 0; overflow: visible; pointer-events: none; }
  .structural-region rect { fill: color-mix(in oklch, var(--node-predicate), transparent 99%); stroke: color-mix(in oklch, var(--node-predicate), transparent 76%); stroke-width: 1; }
  .structural-region text { fill: color-mix(in oklch, var(--node-predicate), var(--foreground) 34%); font-size: 10px; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
  .shared-segment path { fill: none; stroke: var(--graph-edge); stroke-width: 1.5; }
  .shared-segment polygon { fill: var(--graph-edge); }
  .shared-segment--path path, .shared-segment--path polygon { stroke: var(--run-path); fill: var(--run-path); stroke-width: 2.1; }
  .shared-segment--current path, .shared-segment--current polygon { stroke: var(--run-current); fill: var(--run-current); stroke-width: 2.75; }
  .shared-segment--inspect path, .shared-segment--inspect polygon { stroke: var(--primary); fill: var(--primary); stroke-width: 2.1; }
  .junction circle { fill: var(--graph-edge); stroke: var(--graph-canvas); stroke-width: 3; }
  .junction--path circle { fill: var(--run-path); }
  .junction--current circle { fill: var(--run-current); }
  .junction--inspect circle { fill: var(--primary); }
  .route-crossing circle { fill: var(--graph-canvas); }
  .route-crossing line { stroke: var(--graph-edge); stroke-width: 1.5; stroke-linecap: round; }
  .route-crossing--path line { stroke: var(--run-path); stroke-width: 2.1; }
  .route-crossing--current line { stroke: var(--run-current); stroke-width: 2.75; }
  .route-crossing--inspect line { stroke: var(--primary); stroke-width: 2.1; }
</style>
