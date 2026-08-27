<script lang="ts">
  import ArrowRight from '@lucide/svelte/icons/arrow-right';
  import Inbox from '@lucide/svelte/icons/inbox';
  import { Badge } from '$components/ui/badge';
  import { Button } from '$components/ui/button';
  import * as Card from '$components/ui/card';
  import * as Table from '$components/ui/table';
  import type { RunSummary } from '$contracts/run-search';
  let { items, nextCursor, busy, onNext }: { items: RunSummary[]; nextCursor: string | null; busy: boolean; onNext: () => void } = $props();
  const statusVariant = (status: RunSummary['status']): 'success' | 'destructive' | 'warning' => status === 'SUCCEEDED' ? 'success' : status === 'FAILED' ? 'destructive' : 'warning';
</script>

{#if items.length === 0}
  <Card.Root class="empty-state"><Card.Content class="empty-state-content"><span class="empty-icon"><Inbox /></span><h2>No decisions found</h2><p>Change the filters or wait until an application records a decision.</p></Card.Content></Card.Root>
{:else}
  <div class="table-wrap">
    <Table.Root class="decision-table">
      <Table.Header><Table.Row><Table.Head>Completed</Table.Head><Table.Head>Decision</Table.Head><Table.Head>Status</Table.Head><Table.Head>Final result</Table.Head><Table.Head><span class="sr-only">Open</span></Table.Head></Table.Row></Table.Header>
      <Table.Body>{#each items as item}
        <Table.Row>
          <Table.Cell><time datetime={item.completedAt}>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.completedAt))}</time></Table.Cell>
          <Table.Cell class="decision-cell"><strong>{item.decisionLabel}</strong><code>{item.executionId}</code></Table.Cell>
          <Table.Cell><Badge variant={statusVariant(item.status)}>{item.status.toLowerCase()}</Badge></Table.Cell>
          <Table.Cell>{item.finalResult ?? 'No final result'}</Table.Cell>
          <Table.Cell class="action-cell"><Button href={`/runs/${encodeURIComponent(item.executionId)}`} variant="ghost" size="sm">Explain <ArrowRight data-icon="inline-end" /></Button></Table.Cell>
        </Table.Row>
      {/each}</Table.Body>
    </Table.Root>
  </div>
  {#if nextCursor}<div class="pagination"><Button variant="outline" disabled={busy} onclick={onNext}>Load older decisions</Button></div>{/if}
{/if}

<style>
  .table-wrap { border: 1px solid var(--border); border-radius: var(--radius-xl); background: var(--card); overflow: hidden; box-shadow: var(--shadow); }
  :global(.decision-table) { min-width: 760px; }
  :global(.decision-table [data-slot='table-head']) { height: 42px; padding-inline: 15px; color: var(--muted-foreground); font-size: .7rem; text-transform: uppercase; letter-spacing: .08em; }
  :global(.decision-table [data-slot='table-cell']) { padding: 13px 15px; font-size: .84rem; }
  :global(.decision-cell strong), :global(.decision-cell code) { display: block; }
  :global(.decision-cell code) { margin-top: 4px; color: var(--muted-foreground); font-size: .68rem; }
  :global(.action-cell) { text-align: right; }
  :global(.action-cell [data-slot='button']) { color: var(--primary); }
  .pagination { display: flex; justify-content: center; padding: 18px; }
  :global(.empty-state) { min-height: 290px; border-style: dashed; box-shadow: none; }
  :global(.empty-state-content) { display: flex; flex: 1; flex-direction: column; align-items: center; justify-content: center; color: var(--muted-foreground); text-align: center; }
  :global(.empty-state-content h2) { color: var(--foreground); font-size: 1rem; margin: 12px 0 2px; }
  :global(.empty-state-content p) { margin: 0; font-size: .82rem; }
  .empty-icon { width: 46px; height: 46px; display: grid; place-items: center; color: var(--primary); background: color-mix(in oklch, var(--primary), transparent 90%); border-radius: 14px; }
  .empty-icon :global(svg) { width: 22px; height: 22px; }
  .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
</style>
