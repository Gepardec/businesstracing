<script lang="ts">
  import type { HTMLInputAttributes } from 'svelte/elements';
  import { cn } from '$lib/utils';
  let { class: className, ...props }: HTMLInputAttributes = $props();
</script>

<input class={cn('input', className)} {...props} />
