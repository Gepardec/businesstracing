<script lang="ts">
  import Calculator from '@lucide/svelte/icons/calculator';
  import CircleCheck from '@lucide/svelte/icons/circle-check';
  import GitBranch from '@lucide/svelte/icons/git-branch';
  import LogIn from '@lucide/svelte/icons/log-in';
  import Send from '@lucide/svelte/icons/send';
  import Split from '@lucide/svelte/icons/split';
  import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
  import { Handle, Position, type NodeProps } from '@xyflow/svelte';
  import type { BusinessFlowNode } from './flow-types';
  let { data }: NodeProps<BusinessFlowNode> = $props();
</script>

<Handle type="target" position={Position.Top} class="business-handle" />
<article class:node-path={data.onPath} class:node-current={data.current} class:node-dimmed={data.dimmed}
  class="business-node business-node--{data.node.kind.toLowerCase()}" data-run-state={data.current ? 'current' : data.onPath ? 'path' : data.dimmed ? 'dimmed' : 'default'} aria-label={`${data.node.kind}: ${data.node.label}`}>
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
    {#if data.stepNumber !== null}<span class="node-step" aria-label={`Step ${data.stepNumber}`}>{data.stepNumber}</span>{/if}
  </header>
  <p title={data.node.label}>{data.node.label}</p>
</article>
<Handle type="source" position={Position.Bottom} class="business-handle" />

<style>
  .business-node { box-sizing: border-box; position: relative; width: 232px; height: 92px; border: 1px solid var(--border); border-radius: 12px; background: linear-gradient(90deg, color-mix(in oklch, var(--node-color), transparent 18%) 0 4px, var(--card) 4px); box-shadow: 0 4px 16px color-mix(in oklch, var(--foreground), transparent 92%); overflow: hidden; transition: opacity 120ms, border-color 120ms, box-shadow 120ms; }
  .business-node header { display: flex; align-items: center; gap: 7px; padding: 11px 13px 4px; color: color-mix(in oklch, var(--node-color), var(--foreground) 30%); }
  .node-icon { display: grid; place-items: center; }
  .node-kind { font-size: 10px; font-weight: 800; letter-spacing: .09em; }
  .node-step { margin-left: auto; width: 21px; height: 21px; display: grid; place-items: center; border-radius: 999px; background: var(--run-current); color: white; font-size: 10px; font-weight: 800; box-shadow: 0 0 0 2px color-mix(in oklch, var(--card), transparent 8%); }
  p { margin: 0; padding: 4px 14px 12px; font-size: 14px; line-height: 1.3; font-weight: 650; display: -webkit-box; line-clamp: 3; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
  .business-node--entry { --node-color: var(--node-entry); border-color: color-mix(in oklch, var(--node-color), var(--border) 24%); border-radius: 999px; }
  .business-node--predicate { --node-color: var(--node-predicate); }
  .business-node--predicate::before { content: ''; position: absolute; left: 12px; top: 51px; width: 8px; height: 8px; background: var(--node-color); transform: rotate(45deg); }
  .business-node--predicate p { padding-left: 29px; }
  .business-node--choice { --node-color: var(--node-choice); clip-path: polygon(8% 0, 92% 0, 100% 50%, 92% 100%, 8% 100%, 0 50%); padding-inline: 10px; }
  .business-node--computation { --node-color: var(--node-computation); }
  .business-node--dispatch { --node-color: var(--node-dispatch); clip-path: polygon(0 0, calc(100% - 13px) 0, 100% 13px, 100% 100%, 0 100%); }
  .business-node--outcome { --node-color: var(--node-outcome); border-color: color-mix(in oklch, var(--node-color), var(--border) 24%); border-radius: 16px; }
  .business-node--coverage_gap { --node-color: var(--node-gap); border-color: var(--node-color); border-style: dashed; clip-path: polygon(8% 0, 92% 0, 100% 20%, 100% 80%, 92% 100%, 8% 100%, 0 80%, 0 20%); }
  .node-path:not(.node-current) { border: 2px solid var(--run-path); box-shadow: 0 5px 20px color-mix(in oklch, var(--run-path), transparent 84%); }
  .node-current { border: 3px solid var(--run-current); box-shadow: 0 0 0 4px color-mix(in oklch, var(--run-current), transparent 88%), 0 8px 24px color-mix(in oklch, var(--run-current), transparent 78%); }
  .node-dimmed { opacity: .68; }
  :global(.svelte-flow__node:focus-visible .business-node) { outline: 2px solid var(--ring); outline-offset: 3px; }
  :global(.svelte-flow__node.selected .business-node:not(.node-current):not(.node-path)) { border-color: color-mix(in oklch, var(--foreground), transparent 22%); box-shadow: 0 6px 20px color-mix(in oklch, var(--foreground), transparent 87%); }
  :global(.business-handle) { width: 1px; height: 1px; opacity: 0; border: 0; background: transparent; pointer-events: none; }
</style>
