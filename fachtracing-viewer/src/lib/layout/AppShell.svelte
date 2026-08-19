<script lang="ts">
  import Moon from '@lucide/svelte/icons/moon';
  import Network from '@lucide/svelte/icons/network';
  import Sun from '@lucide/svelte/icons/sun';
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import Button from '$components/ui/Button.svelte';
  let { children }: { children?: import('svelte').Snippet } = $props();
  let dark = $state(false);
  function toggleTheme() {
    dark = !dark;
    document.documentElement.classList.toggle('dark', dark);
    localStorage.setItem('fachtracing-theme', dark ? 'dark' : 'light');
  }
  onMount(() => {
    const stored = localStorage.getItem('fachtracing-theme');
    dark = stored ? stored === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.classList.toggle('dark', dark);
  });
</script>

<div class="app-shell">
  <header class="app-header">
    <a class="brand" href="/runs"><span class="brand__mark"><Network size={18} /></span><span>Fachtracing</span></a>
    <nav aria-label="Primary navigation"><a class="nav-link" class:active={page.url.pathname.startsWith('/runs')} aria-current={page.url.pathname.startsWith('/runs') ? 'page' : undefined} href="/runs">Decisions</a><a class="nav-link" class:active={page.url.pathname.startsWith('/graphs')} aria-current={page.url.pathname.startsWith('/graphs') ? 'page' : undefined} href="/graphs">Graph preview</a></nav>
    <Button variant="ghost" size="icon" onclick={toggleTheme} aria-label={dark ? 'Use light theme' : 'Use dark theme'}>
      {#if dark}<Sun size={17} />{:else}<Moon size={17} />{/if}
    </Button>
  </header>
  <main class="app-main">{@render children?.()}</main>
</div>

<style>
  nav { display: flex; align-items: center; gap: 6px; }
  .nav-link { padding: 6px 9px; border-radius: 7px; }
  .nav-link.active { color: var(--foreground); background: var(--muted); font-weight: 680; }
  @media (max-width: 620px) {
    nav { gap: 2px; }
    .nav-link { padding-inline: 7px; }
    .brand > span:last-child { display: none; }
  }
</style>
