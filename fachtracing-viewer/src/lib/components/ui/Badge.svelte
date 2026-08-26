<script lang="ts">
  import { cn } from '$lib/utils';
  let { tone = 'neutral', class: className, children }: {
    tone?: 'neutral' | 'success' | 'danger' | 'warning'; class?: string; children?: import('svelte').Snippet
  } = $props();
</script>

<span class={cn('badge', `badge--${tone}`, className)}>{@render children?.()}</span>
