<script lang="ts">
  import FileJson from '@lucide/svelte/icons/file-json';
  import Upload from '@lucide/svelte/icons/upload';
  import Button from '$components/ui/Button.svelte';

  let { busy = false, compact = false, onFile }: {
    busy?: boolean; compact?: boolean; onFile: (file: File) => void
  } = $props();
  let input: HTMLInputElement;
  let dragging = $state(false);

  function choose() {
    input.value = '';
    input.click();
  }

  function selected(files: FileList | null) {
    const file = files?.item(0);
    if (file) onFile(file);
  }
</script>

<div
  role="group"
  aria-label="Graph JSON file selection"
  class:compact
  class:dragging
  class="graph-upload"
  ondragenter={(event) => { event.preventDefault(); dragging = true; }}
  ondragover={(event) => event.preventDefault()}
  ondragleave={(event) => { if (!event.currentTarget.contains(event.relatedTarget as Node | null)) dragging = false; }}
  ondrop={(event) => { event.preventDefault(); dragging = false; selected(event.dataTransfer?.files ?? null); }}
>
  <input
    class="file-input"
    bind:this={input}
    id="developer-graph-file"
    type="file"
    accept=".json,application/json"
    aria-label="Developer graph JSON"
    disabled={busy}
    onchange={(event) => selected(event.currentTarget.files)}
  />
  {#if !compact}
    <span class="upload-icon"><FileJson size={26} /></span>
    <div class="upload-copy">
      <strong>Drop a developer graph JSON file here</strong>
      <span>Current V1 JSON · 5 MiB maximum · browser memory only</span>
    </div>
  {/if}
  <Button variant={compact ? 'outline' : 'default'} onclick={choose} disabled={busy}>
    <Upload size={16} />{busy ? 'Reading…' : compact ? 'Choose another file' : 'Choose JSON file'}
  </Button>
</div>

<style>
  .graph-upload { min-height: 240px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 18px; padding: 32px; border: 1.5px dashed var(--border); border-radius: 12px; background: color-mix(in oklch, var(--muted), transparent 46%); text-align: center; transition: border-color 140ms, background 140ms; }
  .graph-upload.dragging { border-color: var(--primary); background: color-mix(in oklch, var(--primary), transparent 92%); }
  .graph-upload.compact { min-height: 0; display: block; padding: 0; border: 0; background: transparent; }
  .upload-icon { width: 54px; height: 54px; display: grid; place-items: center; color: var(--primary); background: color-mix(in oklch, var(--primary), transparent 90%); border-radius: 15px; }
  .upload-copy { display: grid; gap: 5px; }
  .upload-copy strong { font-size: 1rem; }
  .upload-copy span { color: var(--muted-foreground); font-size: .82rem; }
  .file-input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
</style>
