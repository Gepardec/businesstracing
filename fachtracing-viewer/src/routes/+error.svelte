<script lang="ts">
  import { page } from '$app/state';
  import Button from '$components/ui/Button.svelte';
</script>

<div class="error-page"><span class="eyebrow">{page.status}</span><h1>{page.error?.message ?? 'The page is unavailable'}</h1><p>The stored decision was not changed. Return to the decision list and try again.</p><Button onclick={() => location.href = '/runs'}>Return to decisions</Button></div>
<style>.error-page { max-width: 620px; margin: 14vh auto; padding: 30px; text-align: center; } h1 { margin: 8px 0; } p { color: var(--muted-foreground); margin: 0 0 22px; }</style>
