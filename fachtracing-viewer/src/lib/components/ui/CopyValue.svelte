<script lang="ts">
  import Check from '@lucide/svelte/icons/check';
  import Copy from '@lucide/svelte/icons/copy';
  import { Button } from './button';

  let { value, mono = false, label }: { value: string; mono?: boolean; label: string } = $props();
  let copied = $state(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(value);
      copied = true;
      setTimeout(() => copied = false, 1_500);
    } catch {
      copied = false;
    }
  }
</script>

<span class="copy-value" class:mono title={value}>
  <span class="copy-value__text">{value}</span>
  <Button class="copy-value__button" variant="ghost" size="icon-sm" onclick={copy} aria-label={copied ? `${label} copied` : `Copy ${label}`}>
    {#if copied}<Check />{:else}<Copy />{/if}
  </Button>
</span>

<style>
  .copy-value { width: 100%; max-width: 100%; min-width: 0; display: flex; align-items: center; gap: 4px; overflow: hidden; }
  .copy-value__text { width: 100%; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  :global(.copy-value__button) { flex: 0 0 auto; opacity: .28; }
  .copy-value:hover :global(.copy-value__button), .copy-value:focus-within :global(.copy-value__button) { opacity: 1; }
  @media (pointer: coarse) { :global(.copy-value__button) { opacity: .72; } }
</style>
