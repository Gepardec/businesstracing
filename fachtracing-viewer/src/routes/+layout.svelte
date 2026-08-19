<script lang="ts">
  import '../app.css';
  import AppShell from '$lib/layout/AppShell.svelte';
  let { children }: { children?: import('svelte').Snippet } = $props();
</script>

<svelte:head><title>Fachtracing</title><meta name="description" content="Explore recorded business decisions and preview generated business graphs." /></svelte:head>
<AppShell>{@render children?.()}</AppShell>
