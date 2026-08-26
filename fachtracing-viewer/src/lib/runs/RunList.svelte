<script lang="ts">
  import ArrowRight from '@lucide/svelte/icons/arrow-right';
  import Inbox from '@lucide/svelte/icons/inbox';
  import Badge from '$components/ui/Badge.svelte';
  import Button from '$components/ui/Button.svelte';
  import type { RunSummary } from '$contracts/run-search';
  let { items, nextCursor, busy, onNext }: { items: RunSummary[]; nextCursor: string | null; busy: boolean; onNext: () => void } = $props();
  const statusTone = (status: RunSummary['status']) => status === 'SUCCEEDED' ? 'success' : status === 'FAILED' ? 'danger' : 'warning';
</script>

{#if items.length === 0}
  <div class="empty-state"><Inbox size={28} /><h2>No decisions found</h2><p>Change the filters or wait until an application records a decision.</p></div>
{:else}
  <div class="table-wrap">
    <table>
      <thead><tr><th>Completed</th><th>Decision</th><th>Status</th><th>Final result</th><th><span class="sr-only">Open</span></th></tr></thead>
      <tbody>{#each items as item}
        <tr>
          <td><time datetime={item.completedAt}>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.completedAt))}</time></td>
          <td><strong>{item.decisionLabel}</strong><code>{item.executionId}</code></td>
          <td><Badge tone={statusTone(item.status)}>{item.status}</Badge></td>
          <td>{item.finalResult ?? 'No final result'}</td>
          <td><a class="explain-link" href={`/runs/${encodeURIComponent(item.executionId)}`}>Explain <ArrowRight size={14} /></a></td>
        </tr>
      {/each}</tbody>
    </table>
  </div>
  {#if nextCursor}<div class="pagination"><Button variant="outline" disabled={busy} onclick={onNext}>Load older decisions</Button></div>{/if}
{/if}

<style>
  .table-wrap { border: 1px solid var(--border); border-radius: 12px; background: var(--card); overflow: auto; box-shadow: var(--shadow); }
  table { width: 100%; border-collapse: collapse; min-width: 760px; }
  th { padding: 11px 15px; text-align: left; color: var(--muted-foreground); font-size: .7rem; text-transform: uppercase; letter-spacing: .08em; border-bottom: 1px solid var(--border); }
  td { padding: 14px 15px; border-bottom: 1px solid var(--border); font-size: .84rem; vertical-align: middle; }
  tbody tr:last-child td { border-bottom: 0; }
  tbody tr:hover { background: color-mix(in oklch, var(--muted), transparent 30%); }
  td strong, td code { display: block; }
  td code { margin-top: 4px; color: var(--muted-foreground); font-size: .68rem; }
  .explain-link { display: inline-flex; gap: 5px; align-items: center; color: var(--primary); font-weight: 700; text-decoration: none; }
  .pagination { display: flex; justify-content: center; padding: 18px; }
  .empty-state { min-height: 290px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--muted-foreground); border: 1px dashed var(--border); border-radius: 12px; }
  .empty-state h2 { color: var(--foreground); font-size: 1rem; margin: 12px 0 2px; }
  .empty-state p { margin: 0; font-size: .82rem; }
  .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
</style>
