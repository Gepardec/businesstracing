<script lang="ts">
  import { cn } from '$lib/utils';
  let { class: className, children }: { class?: string; children?: import('svelte').Snippet } = $props();
</script>

<section class={cn('card', className)}>{@render children?.()}</section>
