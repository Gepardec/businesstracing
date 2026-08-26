<script lang="ts">
  import type { HTMLButtonAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';
  let { variant = 'default', size = 'default', class: className, children, ...props }: HTMLButtonAttributes & {
    variant?: 'default' | 'outline' | 'ghost' | 'danger'; size?: 'default' | 'sm' | 'icon'; children?: import('svelte').Snippet
  } = $props();
</script>

<button class={cn('button', `button--${variant}`, `button--${size}`, className)} {...props}>{@render children?.()}</button>
