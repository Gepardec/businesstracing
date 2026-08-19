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
  class="business-node business-node--{data.node.kind.toLowerCase()}" aria-label={`${data.node.kind}: ${data.node.label}`}>
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
    {#if data.sequence !== null}<span class="node-step" aria-label={`Step ${data.sequence}`}>{data.sequence}</span>{/if}
  </header>
  <p title={data.node.label}>{data.node.label}</p>
</article>
<Handle type="source" position={Position.Bottom} class="business-handle" />

<style>
  .business-node { width: 232px; min-height: 92px; border: 1px solid color-mix(in oklch, var(--node-color), black 18%); border-left: 5px solid var(--node-color); border-radius: 12px; background: var(--card); box-shadow: 0 4px 18px color-mix(in oklch, var(--foreground), transparent 91%); overflow: hidden; transition: opacity 120ms, box-shadow 120ms; }
  .business-node header { display: flex; align-items: center; gap: 7px; padding: 9px 11px 5px; color: color-mix(in oklch, var(--node-color), var(--foreground) 30%); }
  .node-icon { display: grid; place-items: center; }
  .node-kind { font-size: 10px; font-weight: 800; letter-spacing: .09em; }
  .node-step { margin-left: auto; width: 23px; height: 23px; display: grid; place-items: center; border-radius: 999px; background: var(--run-path); color: oklch(0.17 0.02 150); font-size: 11px; font-weight: 800; }
  p { margin: 0; padding: 3px 12px 12px; font-size: 14px; line-height: 1.3; font-weight: 650; display: -webkit-box; line-clamp: 3; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
  .business-node--entry { --node-color: var(--node-entry); border-radius: 999px; }
  .business-node--predicate { --node-color: var(--node-predicate); }
  .business-node--predicate::before { content: ''; float: left; margin: 37px 0 0 9px; width: 8px; height: 8px; background: var(--node-color); transform: rotate(45deg); }
  .business-node--choice { --node-color: var(--node-choice); clip-path: polygon(8% 0, 92% 0, 100% 50%, 92% 100%, 8% 100%, 0 50%); padding-inline: 10px; }
  .business-node--computation { --node-color: var(--node-computation); }
  .business-node--dispatch { --node-color: var(--node-dispatch); clip-path: polygon(0 0, calc(100% - 13px) 0, 100% 13px, 100% 100%, 0 100%); }
  .business-node--outcome { --node-color: var(--node-outcome); box-shadow: inset 0 0 0 3px var(--card), inset 0 0 0 4px var(--node-color), 0 4px 18px color-mix(in oklch, var(--foreground), transparent 91%); }
  .business-node--coverage_gap { --node-color: var(--node-gap); border-style: dashed; clip-path: polygon(8% 0, 92% 0, 100% 20%, 100% 80%, 92% 100%, 8% 100%, 0 80%, 0 20%); }
  .node-path { box-shadow: inset 0 0 0 2px var(--run-path), 0 4px 18px color-mix(in oklch, var(--run-path), transparent 78%); }
  .node-current { outline: 3px solid var(--run-current); outline-offset: 3px; }
  .node-dimmed { opacity: .48; }
  :global(.business-handle) { width: 8px; height: 8px; background: var(--card); border: 2px solid var(--graph-edge); }
</style>
