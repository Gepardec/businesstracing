<script lang="ts">
  import Search from '@lucide/svelte/icons/search';
  import SlidersHorizontal from '@lucide/svelte/icons/sliders-horizontal';
  import { Button } from '$components/ui/button';
  import * as Card from '$components/ui/card';
  import { Input } from '$components/ui/input';
  import { Label } from '$components/ui/label';
  import * as Select from '$components/ui/select';
  import type { RunSearch } from '$contracts/run-search';
  let { correlationNames, busy, onSearch }: { correlationNames: string[]; busy: boolean; onSearch: (query: RunSearch) => void } = $props();
  let correlationName = $state(''); let correlationValue = $state(''); let executionId = $state('');
  let graphId = $state(''); let status = $state('ANY'); let completedFrom = $state(''); let completedTo = $state('');
  let advanced = $state(false);
  function submit(event: SubmitEvent) {
    event.preventDefault();
    const query: RunSearch = {};
    if (correlationName.trim() && correlationValue.trim()) query.correlation = { name: correlationName.trim(), value: correlationValue.trim() };
    if (executionId.trim()) query.executionId = executionId.trim();
    if (graphId.trim()) query.graphId = graphId.trim();
    if (status !== 'ANY') query.status = status as RunSearch['status'];
    if (completedFrom) query.completedFrom = new Date(completedFrom).toISOString();
    if (completedTo) query.completedTo = new Date(completedTo).toISOString();
    onSearch(query);
  }
</script>

<Card.Root class="filter-card" size="sm">
  <form class="filter-form" onsubmit={submit}>
    <Card.Header class="filter-header border-b">
      <div><span class="eyebrow">Exact lookup</span><Card.Title>Find recorded decisions</Card.Title></div>
      <Card.Action><Button type="button" variant="ghost" size="sm" aria-expanded={advanced} onclick={() => advanced = !advanced}><SlidersHorizontal data-icon="inline-start" /> Filters</Button></Card.Action>
    </Card.Header>
    <Card.Content class="filter-content">
      <div class="primary-fields">
        <div class="field"><Label for="correlation-name">Correlation name</Label><Input id="correlation-name" list="correlation-names" bind:value={correlationName} placeholder="routeId, personRef, address…" maxlength={200} /><datalist id="correlation-names">{#each correlationNames as name}<option value={name}></option>{/each}</datalist></div>
        <div class="field"><Label for="correlation-value">Exact stored value</Label><Input id="correlation-value" bind:value={correlationValue} placeholder="Enter the canonical value" maxlength={500} /></div>
        <Button type="submit" disabled={busy || Boolean(correlationName.trim()) !== Boolean(correlationValue.trim())}><Search data-icon="inline-start" />{busy ? 'Searching…' : 'Search'}</Button>
      </div>
      {#if advanced}
        <div class="advanced-fields">
          <div class="field"><Label for="execution-id">Execution ID</Label><Input id="execution-id" bind:value={executionId} maxlength={200} /></div>
          <div class="field"><Label for="graph-id">Graph ID</Label><Input id="graph-id" bind:value={graphId} maxlength={200} /></div>
          <div class="field"><Label for="run-status">Status</Label><Select.Root type="single" bind:value={status}><Select.Trigger id="run-status" class="w-full">{status === 'ANY' ? 'Any status' : status}</Select.Trigger><Select.Content><Select.Item value="ANY">Any status</Select.Item><Select.Item value="SUCCEEDED">Succeeded</Select.Item><Select.Item value="FAILED">Failed</Select.Item><Select.Item value="INCOMPLETE">Incomplete</Select.Item></Select.Content></Select.Root></div>
          <div class="field"><Label for="completed-from">Completed from</Label><Input id="completed-from" type="datetime-local" bind:value={completedFrom} /></div>
          <div class="field"><Label for="completed-to">Completed to</Label><Input id="completed-to" type="datetime-local" bind:value={completedTo} /></div>
        </div>
      {/if}
    </Card.Content>
  </form>
</Card.Root>

<style>
  :global(.filter-card) { box-shadow: var(--shadow); }
  .filter-form { display: contents; }
  :global(.filter-header) { align-items: center; }
  :global(.filter-header [data-slot='card-title']) { margin-top: 3px; }
  :global(.filter-content) { display: grid; gap: 13px; }
  .primary-fields { display: grid; grid-template-columns: minmax(170px, .75fr) minmax(240px, 1.3fr) auto; align-items: end; gap: 11px; }
  .advanced-fields { display: grid; grid-template-columns: repeat(5, minmax(140px, 1fr)); gap: 11px; padding-top: 13px; margin-top: 13px; border-top: 1px solid var(--border); }
  .field { display: grid; gap: 6px; min-width: 0; }
  .field :global([data-slot='label']) { color: var(--muted-foreground); font-size: .76rem; }
  @media (max-width: 920px) { .primary-fields, .advanced-fields { grid-template-columns: 1fr; } }
</style>
