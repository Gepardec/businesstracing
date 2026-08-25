<script lang="ts">
  import Calculator from '@lucide/svelte/icons/calculator';
  import CircleCheck from '@lucide/svelte/icons/circle-check';
  import GitBranch from '@lucide/svelte/icons/git-branch';
  import LogIn from '@lucide/svelte/icons/log-in';
  import Send from '@lucide/svelte/icons/send';
  import Split from '@lucide/svelte/icons/split';
  import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
  import { Handle, Position, type NodeProps } from '@xyflow/svelte';
  import type { PortSide } from './route-planner';
  import type { BusinessFlowNode } from './flow-types';
  let { data }: NodeProps<BusinessFlowNode> = $props();

  function handlePosition(side: PortSide): Position {
    if (side === 'north') return Position.Top;
    if (side === 'south') return Position.Bottom;
    if (side === 'west') return Position.Left;
    return Position.Right;
  }

  function handleStyle(side: PortSide, point: { x: number; y: number }): string {
    if (side === 'north' || side === 'south') return `left: ${point.x - data.layoutPosition.x}px`;
    return `top: ${point.y - data.layoutPosition.y}px`;
  }
</script>

{#each data.ports as port (port.id)}
  <Handle id={port.id} type={port.role} position={handlePosition(port.side)} class="business-handle" style={handleStyle(port.side, port.point)} role="presentation" aria-hidden="true" />
{/each}
<article class:node-path={data.onPath} class:node-current={data.current} class:node-dimmed={data.dimmed} class:node-context-dimmed={data.contextDimmed} class:node-overview={!data.showDetails}
  class="business-node business-node--{data.node.kind.toLowerCase()}" data-run-state={data.current ? 'current' : data.onPath ? 'path' : data.dimmed ? 'dimmed' : 'default'}
  aria-label={`${data.node.kind}: ${data.node.label}. Node ${data.node.id}. ${data.incomingCount} incoming and ${data.outgoingCount} outgoing edges${data.occurrence ? `. Occurrence ${data.occurrence.index} of ${data.occurrence.total}` : ''}.`}>
  <header>
    <span class="node-icon" aria-hidden="true">
      {#if data.node.kind === 'ENTRY'}<LogIn size={15} />
      {:else if data.node.kind === 'PREDICATE'}<GitBranch size={15} />
      {:else if data.node.kind === 'CHOICE'}<Split size={15} />
      {:else if data.node.kind === 'COMPUTATION'}<Calculator size={15} />
      {:else if data.node.kind === 'DISPATCH'}<Send size={15} />
      {:else if data.node.kind === 'OUTCOME'}<CircleCheck size={15} />
      {:else}<TriangleAlert size={15} />{/if}
    </span>
    <span class="node-kind">{data.node.kind.replace('_', ' ')}</span>
    {#if data.occurrence}<span class="node-occurrence" title={`Node ${data.node.id}`}>{data.occurrence.index} of {data.occurrence.total}</span>{/if}
    {#if data.stepNumber !== null}<span class="node-step" aria-label={`Step ${data.stepNumber}`}>{data.stepNumber}</span>{/if}
  </header>
  {#if data.showDetails}<p title={data.node.label}>{data.node.label}</p>{/if}
</article>

<style>
  .business-node { box-sizing: border-box; position: relative; width: 232px; height: 92px; border: 1px solid var(--border); border-radius: 12px; background: linear-gradient(90deg, color-mix(in oklch, var(--node-color), transparent 18%) 0 4px, var(--card) 4px); box-shadow: 0 4px 16px color-mix(in oklch, var(--foreground), transparent 92%); overflow: hidden; transition: opacity 120ms, border-color 120ms, box-shadow 120ms; }
  .business-node header { display: flex; align-items: center; gap: 7px; padding: 11px 13px 4px; color: color-mix(in oklch, var(--node-color), var(--foreground) 30%); }
  .node-icon { display: grid; place-items: center; }
  .node-kind { font-size: 10px; font-weight: 800; letter-spacing: .09em; }
  .node-occurrence { margin-left: auto; color: var(--muted-foreground); font-size: 9px; font-weight: 750; letter-spacing: .02em; }
  .node-occurrence + .node-step { margin-left: 0; }
  .node-step { margin-left: auto; width: 21px; height: 21px; display: grid; place-items: center; border-radius: 999px; background: var(--run-current); color: white; font-size: 10px; font-weight: 800; box-shadow: 0 0 0 2px color-mix(in oklch, var(--card), transparent 8%); }
  p { margin: 0; padding: 4px 14px 12px; font-size: 14px; line-height: 1.3; font-weight: 650; display: -webkit-box; line-clamp: 3; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
  .business-node--entry { --node-color: var(--node-entry); border-color: color-mix(in oklch, var(--node-color), var(--border) 24%); border-radius: 999px; }
  .business-node--predicate { --node-color: var(--node-predicate); }
  .business-node--choice { --node-color: var(--node-choice); clip-path: polygon(8% 0, 92% 0, 100% 50%, 92% 100%, 8% 100%, 0 50%); padding-inline: 10px; }
  .business-node--computation { --node-color: var(--node-computation); }
  .business-node--dispatch { --node-color: var(--node-dispatch); clip-path: polygon(0 0, calc(100% - 13px) 0, 100% 13px, 100% 100%, 0 100%); }
  .business-node--outcome { --node-color: var(--node-outcome); border-color: color-mix(in oklch, var(--node-color), var(--border) 24%); border-radius: 16px; }
  .business-node--coverage_gap { --node-color: var(--node-gap); border-color: var(--node-color); border-style: dashed; clip-path: polygon(8% 0, 92% 0, 100% 20%, 100% 80%, 92% 100%, 8% 100%, 0 80%, 0 20%); }
  .node-path:not(.node-current) { border: 2px solid var(--run-path); box-shadow: 0 5px 20px color-mix(in oklch, var(--run-path), transparent 84%); }
  .node-current { border: 3px solid var(--run-current); box-shadow: 0 0 0 4px color-mix(in oklch, var(--run-current), transparent 88%), 0 8px 24px color-mix(in oklch, var(--run-current), transparent 78%); }
  .node-dimmed { opacity: .68; }
  .node-context-dimmed { opacity: .12; box-shadow: none; }
  .node-overview header { visibility: hidden; }
  .node-overview { box-shadow: none; }
  :global(.svelte-flow__node:focus-visible .business-node) { outline: 2px solid var(--ring); outline-offset: 3px; }
  :global(.svelte-flow__node.selected .business-node:not(.node-current):not(.node-path)) { border: 3px solid var(--primary); box-shadow: 0 0 0 4px color-mix(in oklch, var(--primary), transparent 82%), 0 8px 22px color-mix(in oklch, var(--foreground), transparent 84%); }
  :global(.business-handle) { width: 1px; height: 1px; opacity: 0; border: 0; background: transparent; pointer-events: none; }
</style>
