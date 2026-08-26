<script lang="ts">
  import Search from '@lucide/svelte/icons/search';
  import SlidersHorizontal from '@lucide/svelte/icons/sliders-horizontal';
  import Button from '$components/ui/Button.svelte';
  import Card from '$components/ui/Card.svelte';
  import type { RunSearch } from '$contracts/run-search';
  let { correlationNames, busy, onSearch }: { correlationNames: string[]; busy: boolean; onSearch: (query: RunSearch) => void } = $props();
  let correlationName = $state(''); let correlationValue = $state(''); let executionId = $state('');
  let graphId = $state(''); let status = $state(''); let completedFrom = $state(''); let completedTo = $state('');
  let advanced = $state(false);
  function submit(event: SubmitEvent) {
    event.preventDefault();
    const query: RunSearch = {};
    if (correlationName.trim() && correlationValue.trim()) query.correlation = { name: correlationName.trim(), value: correlationValue.trim() };
    if (executionId.trim()) query.executionId = executionId.trim();
    if (graphId.trim()) query.graphId = graphId.trim();
    if (status) query.status = status as RunSearch['status'];
    if (completedFrom) query.completedFrom = new Date(completedFrom).toISOString();
    if (completedTo) query.completedTo = new Date(completedTo).toISOString();
    onSearch(query);
  }
</script>

<Card class="filter-card">
  <form onsubmit={submit}>
    <div class="filter-title"><div><span class="eyebrow">Exact lookup</span><h2>Find recorded decisions</h2></div><Button type="button" variant="ghost" size="sm" onclick={() => advanced = !advanced}><SlidersHorizontal size={15} /> Filters</Button></div>
    <div class="primary-fields">
      <label><span>Correlation name</span><input class="input" list="correlation-names" bind:value={correlationName} placeholder="routeId, personRef, address…" maxlength="200" /><datalist id="correlation-names">{#each correlationNames as name}<option value={name}></option>{/each}</datalist></label>
      <label><span>Exact stored value</span><input class="input" bind:value={correlationValue} placeholder="Enter the canonical value" maxlength="500" /></label>
      <Button type="submit" disabled={busy || Boolean(correlationName.trim()) !== Boolean(correlationValue.trim())}><Search size={16} />{busy ? 'Searching…' : 'Search'}</Button>
    </div>
    {#if advanced}
      <div class="advanced-fields">
        <label><span>Execution ID</span><input class="input" bind:value={executionId} maxlength="200" /></label>
        <label><span>Graph ID</span><input class="input" bind:value={graphId} maxlength="200" /></label>
        <label><span>Status</span><select class="select" bind:value={status}><option value="">Any status</option><option>SUCCEEDED</option><option>FAILED</option><option>INCOMPLETE</option></select></label>
        <label><span>Completed from</span><input class="input" type="datetime-local" bind:value={completedFrom} /></label>
        <label><span>Completed to</span><input class="input" type="datetime-local" bind:value={completedTo} /></label>
      </div>
    {/if}
  </form>
</Card>

<style>
  :global(.filter-card) { padding: 18px; }
  .filter-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
  h2 { margin: 3px 0 0; font-size: 1.08rem; }
  .primary-fields { display: grid; grid-template-columns: minmax(170px, .75fr) minmax(240px, 1.3fr) auto; align-items: end; gap: 11px; }
  .advanced-fields { display: grid; grid-template-columns: repeat(5, minmax(140px, 1fr)); gap: 11px; padding-top: 13px; margin-top: 13px; border-top: 1px solid var(--border); }
  label { display: grid; gap: 6px; font-size: .76rem; font-weight: 650; color: var(--muted-foreground); }
  @media (max-width: 920px) { .primary-fields, .advanced-fields { grid-template-columns: 1fr; } }
</style>
