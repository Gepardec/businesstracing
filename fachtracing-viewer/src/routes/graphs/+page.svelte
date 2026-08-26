<script lang="ts">
  import FileCheck from '@lucide/svelte/icons/file-check';
  import ShieldCheck from '@lucide/svelte/icons/shield-check';
  import Badge from '$components/ui/Badge.svelte';
  import Card from '$components/ui/Card.svelte';
  import type { GraphModel } from '$contracts/graph-contract';
  import FlowCanvas from '$graph/FlowCanvas.svelte';
  import GraphUpload from '$graph/GraphUpload.svelte';
  import { formatFileSize, parseGraphFile } from '$graph/graph-file';

  let graph = $state<GraphModel | null>(null);
  let fileName = $state('');
  let fileSize = $state(0);
  let error = $state('');
  let busy = $state(false);

  async function selectFile(file: File) {
    busy = true;
    error = '';
    graph = null;
    fileName = '';
    fileSize = 0;
    try {
      graph = await parseGraphFile(file);
      fileName = file.name;
      fileSize = file.size;
    } catch (cause) {
      error = cause instanceof Error ? cause.message : 'The selected graph could not be read.';
    } finally {
      busy = false;
    }
  }
</script>

<svelte:head><title>Graph preview · Fachtracing</title></svelte:head>

<div class="graphs-page">
  <header class="page-heading">
    <div><span class="eyebrow">Local graph viewer</span><h1>Preview a graph JSON</h1><p>Inspect a generated business graph before you import it into the decision catalog.</p></div>
    <div class="privacy"><ShieldCheck size={17} /><span><strong>Browser only</strong>Your file is not uploaded or saved.</span></div>
  </header>

  {#if error}<div class="alert" role="alert"><strong>The graph cannot be shown.</strong> {error}</div>{/if}

  {#if graph}
    <Card class="graph-summary">
      <div class="file-summary"><span class="file-icon"><FileCheck size={19} /></span><div><strong>{fileName}</strong><span>{formatFileSize(fileSize)} · {graph.schema}</span></div></div>
      <div class="graph-identity"><div><span class="eyebrow">Graph</span><h2>{graph.label}</h2></div><div class="badges"><Badge>{graph.nodes.length} nodes</Badge><Badge>{graph.edges.length} edges</Badge><Badge tone={graph.completeness === 'COMPLETE' ? 'success' : 'warning'}>{graph.completeness.toLowerCase()}</Badge></div></div>
      {#if graph.nodes.length > 250}<p class="profile-note">This graph is larger than the tested 250-node safety profile. The complete graph is still available.</p>{/if}
      <GraphUpload compact {busy} onFile={selectFile} />
    </Card>
    <div class="preview-canvas"><FlowCanvas {graph} highlight={null} fullPath={false} /></div>
  {:else}
    <Card class="upload-card"><GraphUpload {busy} onFile={selectFile} /></Card>
    <p class="contract-note">Accepted formats: <code>fachtracing-developer-graph/v1</code> and <code>fachtracing-business-graph/v1</code>. A page reload clears the preview.</p>
  {/if}
</div>

<style>
  .graphs-page { max-width: 1600px; margin: 0 auto; padding: 36px clamp(18px, 4vw, 54px) 48px; display: grid; gap: 18px; }
  .page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 28px; }
  h1 { margin: 5px 0 4px; font-size: clamp(1.75rem, 3vw, 2.35rem); letter-spacing: -.045em; }
  .page-heading p, .contract-note { margin: 0; color: var(--muted-foreground); font-size: .9rem; }
  .privacy { display: flex; align-items: center; gap: 9px; color: var(--status-success); font-size: .78rem; white-space: nowrap; }
  .privacy span { display: grid; color: var(--muted-foreground); }
  .privacy strong { color: var(--foreground); }
  :global(.upload-card) { padding: 12px; }
  .contract-note { text-align: center; }
  .contract-note code { font-size: .82rem; color: var(--foreground); }
  :global(.graph-summary) { padding: 14px 16px; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 18px; }
  .file-summary { display: flex; align-items: center; gap: 10px; min-width: 220px; }
  .file-summary > div { display: grid; gap: 2px; }
  .file-summary strong { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: .82rem; }
  .file-summary span { color: var(--muted-foreground); font-size: .7rem; }
  .file-icon { width: 36px; height: 36px; display: grid; place-items: center; color: var(--status-success); background: color-mix(in oklch, var(--status-success), transparent 90%); border-radius: 9px; }
  .graph-identity { min-width: 0; display: flex; align-items: center; justify-content: center; gap: 16px; }
  .graph-identity h2 { margin: 2px 0 0; max-width: 520px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 1rem; }
  .badges { display: flex; flex-wrap: wrap; gap: 6px; }
  .profile-note { grid-column: 1 / -1; margin: 0; color: var(--muted-foreground); font-size: .78rem; text-align: center; }
  .preview-canvas { height: calc(100vh - 240px); min-height: 620px; overflow: hidden; border: 1px solid var(--border); border-radius: 12px; box-shadow: var(--shadow); }
  @media (max-width: 900px) {
    .page-heading { align-items: flex-start; flex-direction: column; }
    .privacy { white-space: normal; }
    :global(.graph-summary) { grid-template-columns: 1fr auto; }
    .graph-identity { grid-column: 1 / -1; grid-row: 2; justify-content: flex-start; }
    .preview-canvas { height: 72vh; min-height: 560px; }
  }
</style>
