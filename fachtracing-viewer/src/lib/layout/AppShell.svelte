<script lang="ts">
  import Moon from '@lucide/svelte/icons/moon';
  import Network from '@lucide/svelte/icons/network';
  import Sun from '@lucide/svelte/icons/sun';
  import Button from '$components/ui/Button.svelte';
  let { children }: { children?: import('svelte').Snippet } = $props();
  let dark = $state(false);
  function toggleTheme() {
    dark = !dark;
    document.documentElement.classList.toggle('dark', dark);
  }
</script>

<div class="app-shell">
  <header class="app-header">
    <a class="brand" href="/runs"><span class="brand__mark"><Network size={18} /></span><span>Fachtracing</span></a>
    <nav aria-label="Primary navigation"><a class="nav-link" href="/runs">Decisions</a></nav>
    <Button variant="ghost" size="icon" onclick={toggleTheme} aria-label={dark ? 'Use light theme' : 'Use dark theme'}>
      {#if dark}<Sun size={17} />{:else}<Moon size={17} />{/if}
    </Button>
  </header>
  <main class="app-main">{@render children?.()}</main>
</div>
