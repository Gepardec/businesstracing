<script lang="ts">
  import X from '@lucide/svelte/icons/x';
  import { Dialog } from 'bits-ui';

  let {
    open = $bindable(false),
    title,
    description,
    triggerLabel,
    trigger,
    children
  }: {
    open?: boolean;
    title: string;
    description: string;
    triggerLabel: string;
    trigger?: import('svelte').Snippet;
    children?: import('svelte').Snippet;
  } = $props();
</script>

<Dialog.Root bind:open>
  <Dialog.Trigger class="button button--outline button--sm sheet-trigger" aria-label={triggerLabel}>
    {@render trigger?.()}
  </Dialog.Trigger>
  <Dialog.Portal>
    <Dialog.Overlay class="sheet-overlay" />
    <Dialog.Content class="sheet-content">
      <Dialog.Title class="sr-only">{title}</Dialog.Title>
      <Dialog.Description class="sr-only">{description}</Dialog.Description>
      <Dialog.Close class="sheet-close" aria-label="Close explanation"><X size={18} /></Dialog.Close>
      <div class="sheet-body">{@render children?.()}</div>
    </Dialog.Content>
  </Dialog.Portal>
</Dialog.Root>

<style>
  :global(.sheet-overlay) { position: fixed; inset: 0; z-index: 60; background: oklch(0.1 0.01 255 / .48); backdrop-filter: blur(2px); animation: sheet-fade-in 150ms ease-out; }
  :global(.sheet-content) { position: fixed; z-index: 61; top: 0; right: 0; bottom: 0; width: min(430px, 94vw); padding: 0; border: 0; border-left: 1px solid var(--border); background: var(--card); color: var(--foreground); box-shadow: -18px 0 50px oklch(0.1 0.01 255 / .2); animation: sheet-slide-in 180ms ease-out; }
  :global(.sheet-close) { position: absolute; z-index: 2; top: 13px; right: 13px; width: 34px; height: 34px; display: grid; place-items: center; border: 1px solid var(--border); border-radius: 8px; background: var(--card); color: var(--muted-foreground); cursor: pointer; }
  :global(.sheet-close:hover) { color: var(--foreground); background: var(--muted); }
  :global(.sheet-body) { height: 100%; min-height: 0; }
  :global(.sheet-body .run-inspector) { border-left: 0; }
  :global(.sheet-body .inspector-header) { padding-right: 72px; }
  :global(.sr-only) { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
  @keyframes sheet-fade-in { from { opacity: 0; } }
  @keyframes sheet-slide-in { from { transform: translateX(100%); } }
  @media (prefers-reduced-motion: reduce) { :global(.sheet-overlay), :global(.sheet-content) { animation: none; } }
</style>
