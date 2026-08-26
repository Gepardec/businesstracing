<script lang="ts">
  let { value, min = 320, max = 520, onResize }: {
    value: number; min?: number; max?: number; onResize: (value: number) => void
  } = $props();
  let startX = 0;
  let startValue = 0;
  let dragging = false;

  const bounded = (next: number) => Math.min(max, Math.max(min, next));

  function start(event: PointerEvent) {
    startX = event.clientX;
    startValue = value;
    dragging = true;
    (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  }

  function move(event: PointerEvent) {
    if (!dragging) return;
    onResize(bounded(startValue + startX - event.clientX));
  }

  function key(event: KeyboardEvent) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    onResize(bounded(value + (event.key === 'ArrowLeft' ? 16 : -16)));
  }
</script>

<button
  type="button"
  class="inspector-resizer"
  role="slider"
  aria-label="Explanation panel width"
  aria-orientation="horizontal"
  aria-valuemin={min}
  aria-valuemax={max}
  aria-valuenow={value}
  tabindex="0"
  onpointerdown={start}
  onpointermove={move}
  onpointerup={() => dragging = false}
  onpointercancel={() => dragging = false}
  onkeydown={key}
></button>

<style>
  .inspector-resizer { position: relative; z-index: 4; width: 8px; padding: 0; cursor: col-resize; background: var(--card); border: 0; border-left: 1px solid var(--border); border-right: 1px solid var(--border); touch-action: none; }
  .inspector-resizer::after { content: ''; position: absolute; top: 50%; left: 2px; width: 2px; height: 42px; border-left: 1px solid var(--muted-foreground); border-right: 1px solid var(--muted-foreground); border-radius: 2px; opacity: .45; transform: translateY(-50%); }
  .inspector-resizer:hover, .inspector-resizer:focus-visible { outline: 0; background: color-mix(in oklch, var(--primary), transparent 92%); }
  .inspector-resizer:focus-visible::after { width: 3px; left: 2px; border: 0; background: var(--primary); opacity: 1; }
</style>
