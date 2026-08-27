<script lang="ts">
  import CircleAlert from '@lucide/svelte/icons/circle-alert';
  import type { PageData } from './$types';
  import * as Alert from '$components/ui/alert';
  import type { RunPage, RunSearch } from '$contracts/run-search';
  import RunFilters from '$runs/RunFilters.svelte';
  import RunList from '$runs/RunList.svelte';
  let { data }: { data: PageData } = $props();
  let page = $state<RunPage>({ items: [], nextCursor: null });
  let currentQuery = $state<RunSearch>({});
  let busy = $state(false);
  let searchError = $state<string | null>(null);
  let initialized = $state(false);

  $effect(() => {
    if (!initialized) {
      page = data.page;
      searchError = data.availabilityError;
      initialized = true;
    }
  });

  async function request(query: RunSearch, append = false) {
    busy = true; searchError = null;
    try {
      const response = await fetch('/api/v1/runs', {
        method: 'QUERY', headers: { 'content-type': 'application/json' }, body: JSON.stringify(query), cache: 'no-store'
      });
      if (!response.ok) throw new Error('Search failed');
      const next = await response.json() as RunPage;
      page = append ? { items: [...page.items, ...next.items], nextCursor: next.nextCursor } : next;
    } catch {
      searchError = 'The decision search could not be completed. Try again.';
    } finally { busy = false; }
  }

  function search(query: RunSearch) { currentQuery = query; request(query); }
  function next() { if (page.nextCursor) request({ ...currentQuery, cursor: page.nextCursor }, true); }
</script>

<svelte:head><title>Decisions · Fachtracing</title></svelte:head>
<div class="runs-page">
  <header class="page-heading"><div><span class="eyebrow">Run explorer</span><h1>Recorded decisions</h1><p>Find a run by any stored business correlation and explain its exact path.</p></div><div class="count"><strong>{page.items.length}</strong><span>shown</span></div></header>
  <RunFilters correlationNames={data.correlationNames} {busy} onSearch={search} />
  {#if searchError}
    <Alert.Root variant="destructive">
      <CircleAlert />
      <Alert.Title>Search unavailable</Alert.Title>
      <Alert.Description>{searchError}</Alert.Description>
    </Alert.Root>
  {/if}
  <RunList items={page.items} nextCursor={page.nextCursor} {busy} onNext={next} />
</div>

<style>
  .runs-page { max-width: 1380px; margin: 0 auto; padding: 36px clamp(18px, 4vw, 54px) 60px; display: grid; gap: 20px; }
  .page-heading { display: flex; align-items: flex-end; justify-content: space-between; }
  h1 { margin: 5px 0 4px; font-size: clamp(1.75rem, 3vw, 2.35rem); letter-spacing: -.045em; }
  .page-heading p { margin: 0; color: var(--muted-foreground); font-size: .9rem; }
  .count { min-width: 76px; text-align: right; color: var(--muted-foreground); }
  .count strong, .count span { display: block; }
  .count strong { color: var(--foreground); font-size: 1.4rem; }
  .count span { font-size: .72rem; }
</style>
