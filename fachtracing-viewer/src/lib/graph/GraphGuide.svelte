<script lang="ts">
  import ArrowLeft from '@lucide/svelte/icons/arrow-left';
  import ArrowRight from '@lucide/svelte/icons/arrow-right';
  import CircleDot from '@lucide/svelte/icons/circle-dot';
  import ListTree from '@lucide/svelte/icons/list-tree';
  import MapIcon from '@lucide/svelte/icons/map';
  import type { GraphNarrative } from './graph-presentation';
  import type { GraphGuideContext } from './graph-guide';

  let { narrative, context, selected = false, overview = false, onSelect }: {
    narrative: GraphNarrative;
    context: GraphGuideContext | null;
    selected?: boolean;
    overview?: boolean;
    onSelect: (nodeId: string) => void;
  } = $props();

  function kindLabel(): string {
    if (!context) return 'Step';
    if (context.memberLabels.length > 1) return context.node.kind === 'PREDICATE' ? 'Rule sequence' : 'Action sequence';
    return context.node.kind.replace('_', ' ').toLowerCase();
  }
</script>

<aside class="graph-guide" class:has-selection={selected} aria-label="Graph explanation">
  <section class="decision-summary">
    <span class="guide-eyebrow"><ListTree size={14} /> Decision summary</span>
    {#each narrative.sentences as sentence}<p>{sentence}</p>{/each}
  </section>

  {#if overview}
    <section class="overview-note">
      <span class="guide-eyebrow"><MapIcon size={14} /> Topology map</span>
      <strong>The complete structure is shown at map scale.</strong>
      <p>Select a node to open it at reading size and inspect its immediate logic.</p>
    </section>
    {#if context}
      <section class="overview-inspection" aria-label="Zoomed node label">
        <span class="guide-eyebrow"><CircleDot size={14} /> Node under pointer</span>
        <span class="kind">{kindLabel()}</span>
        <h2>{context.node.label}</h2>
        {#if context.memberLabels.length > 1}
          <div class="section-heading"><strong>Steps in this sequence</strong><span>{context.memberLabels.length}</span></div>
          <ol>{#each context.memberLabels as label}<li>{label}</li>{/each}</ol>
        {/if}
      </section>
    {/if}
  {:else if context}
    <section class="current-step">
      <span class="guide-eyebrow"><CircleDot size={14} /> {selected ? 'Selected step' : 'Start here'}</span>
      <span class="kind">{kindLabel()}</span>
      <h2>{context.node.label}</h2>
    </section>

    {#if context.memberLabels.length > 1}
      <section class="sequence">
        <div class="section-heading"><strong>Steps in this sequence</strong><span>{context.memberLabels.length}</span></div>
        <ol>{#each context.memberLabels as label}<li>{label}</li>{/each}</ol>
      </section>
    {/if}

    {#if context.outgoing.length > 0}
      <section class="connections next-connections">
        <div class="section-heading"><strong>Continue to</strong><span>{context.outgoing.length}</span></div>
        <div class="connection-list">
          {#each context.outgoing as item}
            <button type="button" onclick={() => onSelect(item.nodeId)}>
              <span class="connection-copy">{#if item.outcome}<small>{item.outcome}</small>{/if}<strong>{item.nodeLabel}</strong></span>
              <ArrowRight size={16} />
            </button>
          {/each}
        </div>
      </section>
    {:else}
      <p class="terminal-note">This step has no declared continuation.</p>
    {/if}

    {#if context.incoming.length > 0}
      <details class="connections previous-connections">
        <summary><ArrowLeft size={14} /> Arrives from {context.incoming.length} {context.incoming.length === 1 ? 'step' : 'steps'}</summary>
        <div class="connection-list">
          {#each context.incoming as item}
            <button type="button" onclick={() => onSelect(item.nodeId)}>
              <span class="connection-copy">{#if item.outcome}<small>{item.outcome}</small>{/if}<strong>{item.nodeLabel}</strong></span>
              <ArrowLeft size={15} />
            </button>
          {/each}
        </div>
      </details>
    {/if}
  {/if}
</aside>

<style>
  .graph-guide { position: absolute; z-index: 7; top: 14px; right: 14px; bottom: 14px; width: 326px; box-sizing: border-box; display: flex; flex-direction: column; gap: 15px; overflow: auto; padding: 18px; border: 1px solid var(--border); border-radius: 12px; background: color-mix(in oklch, var(--card), transparent 2%); box-shadow: 0 18px 50px color-mix(in oklch, var(--foreground), transparent 88%); }
  section { display: grid; gap: 8px; }
  section + section, details { padding-top: 15px; border-top: 1px solid var(--border); }
  .guide-eyebrow { display: flex; align-items: center; gap: 7px; color: var(--primary); font-size: 10px; font-weight: 850; letter-spacing: .11em; text-transform: uppercase; }
  .decision-summary p, .overview-note p { margin: 0; color: var(--muted-foreground); font-size: 12px; line-height: 1.45; }
  .decision-summary p:first-of-type, .overview-note strong { color: var(--foreground); }
  .current-step { gap: 4px; }
  .kind { width: max-content; margin-top: 5px; padding: 3px 7px; border-radius: 999px; color: var(--muted-foreground); background: var(--secondary); font-size: 9px; font-weight: 800; letter-spacing: .06em; text-transform: uppercase; }
  h2 { margin: 3px 0 0; font-size: 18px; line-height: 1.25; letter-spacing: -.02em; }
  .section-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; font-size: 12px; }
  .section-heading span { min-width: 21px; height: 21px; display: grid; place-items: center; border-radius: 999px; color: var(--muted-foreground); background: var(--secondary); font-size: 10px; font-weight: 800; }
  ol { max-height: 190px; overflow: auto; margin: 0; padding: 0 0 0 24px; color: var(--muted-foreground); font-size: 11px; line-height: 1.4; }
  li { padding: 4px 3px; }
  li::marker { color: var(--primary); font-weight: 800; }
  .connection-list { display: grid; gap: 7px; }
  button { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 11px; border: 1px solid var(--border); border-radius: 9px; color: var(--foreground); background: var(--background); text-align: left; cursor: pointer; transition: border-color 120ms, background 120ms, transform 120ms; }
  button:hover { border-color: color-mix(in oklch, var(--primary), var(--border) 45%); background: color-mix(in oklch, var(--primary), var(--background) 96%); transform: translateX(2px); }
  button:focus-visible { outline: 2px solid var(--ring); outline-offset: 2px; }
  .connection-copy { min-width: 0; display: grid; gap: 2px; }
  .connection-copy small { color: var(--primary); font-size: 9px; font-weight: 850; letter-spacing: .08em; text-transform: uppercase; }
  .connection-copy strong { font-size: 12px; line-height: 1.3; }
  .terminal-note { margin: 0; padding: 10px 11px; border-radius: 9px; color: var(--muted-foreground); background: var(--secondary); font-size: 11px; }
  details { margin-top: auto; }
  summary { display: flex; align-items: center; gap: 6px; color: var(--muted-foreground); font-size: 11px; font-weight: 750; cursor: pointer; list-style: none; }
  details[open] summary { margin-bottom: 9px; }
  @media (max-width: 900px) {
    .graph-guide { top: auto; left: 14px; width: auto; max-height: 34%; }
    .graph-guide.has-selection .decision-summary { display: none; }
  }
</style>
