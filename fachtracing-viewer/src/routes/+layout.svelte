<script lang="ts">
  import '../app.css';
  import AppShell from '$lib/layout/AppShell.svelte';
  let { children }: { children?: import('svelte').Snippet } = $props();
</script>

<svelte:head><title>Fachtracing Decisions</title><meta name="description" content="Explore recorded business decisions and their execution paths." /></svelte:head>
<AppShell>{@render children?.()}</AppShell>
