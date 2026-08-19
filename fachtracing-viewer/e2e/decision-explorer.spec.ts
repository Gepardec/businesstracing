import { expect, test } from '@playwright/test';
import { mkdir, readFile, readdir } from 'node:fs/promises';
import { join } from 'node:path';

async function openGeneratedFachtracingRun(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/runs');
  await page.getByLabel('Correlation name').fill('application');
  await page.getByLabel('Exact stored value').fill('fachtracing');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByRole('link', { name: 'Explain' }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Explain' }).first().click();
  await expect(page.locator('.svelte-flow__node').first()).toBeVisible({ timeout: 15_000 });
}

const generatedNodeKinds = ['PREDICATE', 'CHOICE', 'COMPUTATION', 'DISPATCH', 'COVERAGE_GAP'] as const;

function generatedGraphFile(nodeCount: number): { name: string; mimeType: string; buffer: Buffer } {
  const nodes = Array.from({ length: nodeCount }, (_, index) => {
    const kind = index === 0 ? 'ENTRY' : index === nodeCount - 1 ? 'OUTCOME' : generatedNodeKinds[(index - 1) % generatedNodeKinds.length];
    return { id: `node-${index}`, kind, label: `generated ${kind.toLowerCase().replace('_', ' ')} ${index}`, attributes: {} };
  });
  const coverageGaps = nodes.filter((node) => node.kind === 'COVERAGE_GAP').map((node) => ({ nodeId: node.id, description: 'generated coverage gap' }));
  const document = {
    schema: 'fachtracing-developer-graph/v1',
    sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'browser visual test', checksum: 'fixture' }],
    sourceFiles: [],
    graph: {
      id: `generated-browser-${nodeCount}`, version: 1, label: `generated ${nodeCount}-node graph`, entryNodeId: 'node-0',
      completeness: coverageGaps.length ? 'INCOMPLETE' : 'COMPLETE', nodes,
      edges: Array.from({ length: nodeCount - 1 }, (_, index) => ({ id: `edge-${index}`, from: `node-${index}`, to: `node-${index + 1}`, outcome: 'next' })),
      coverageGaps
    }
  };
  return { name: `generated-${nodeCount}.json`, mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(document)) };
}

test('shows a generic correlation search without placing values in the URL', async ({ page }) => {
  await page.goto('/runs');
  await expect(page.getByRole('heading', { name: 'Recorded decisions', exact: true })).toBeVisible();
  await expect(page.getByLabel('Correlation name')).toBeVisible();
  await expect(page.getByLabel('Exact stored value')).toBeVisible();
  await page.getByLabel('Correlation name').fill('routeId');
  await page.getByLabel('Exact stored value').fill('route-17');
  await page.getByRole('button', { name: 'Search' }).click();
  if (process.env.FACHTRACING_DATABASE_URL) await expect(page.getByText('choose a delivery route')).toBeVisible();
  else await expect(page.getByRole('alert')).toContainText('could not be completed');
  await expect(page).toHaveURL(/\/runs$/);
});

test('finds and explains a generated run', async ({ page }) => {
  test.skip(!process.env.FACHTRACING_DATABASE_URL, 'PostgreSQL fixture is required');
  await page.goto('/runs');
  await page.getByLabel('Correlation name').fill('routeId');
  await page.getByLabel('Exact stored value').fill('route-17');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByText('choose a delivery route')).toBeVisible();
  await expect(page).toHaveURL(/\/runs$/);
  await page.getByRole('link', { name: 'Explain' }).first().click();
  await expect(page.getByRole('heading', { name: 'choose a delivery route' })).toBeVisible();
  await expect(page.getByRole('complementary', { name: 'Run explanation' })).toContainText('3 steps');
  await page.getByRole('button', { name: /route is local/ }).click();
  await expect(page.getByRole('complementary', { name: 'Run explanation' })).toContainText('route was Route 17');
});

test('keeps the explanation available at a narrow width', async ({ page }) => {
  test.skip(!process.env.FACHTRACING_DATABASE_URL, 'PostgreSQL fixture is required');
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/runs/e2e-execution');
  await page.getByRole('button', { name: 'Explanation' }).click();
  await expect(page.getByRole('complementary', { name: 'Run explanation' })).toBeVisible();
});

test('renders Fachtracing from its generated graph and Java-agent run', async ({ page }) => {
  test.skip(!process.env.FACHTRACING_DOGFOOD_DIRECTORY, 'Generated Fachtracing artifacts are required');
  const browserErrors: string[] = [];
  page.on('console', (message) => { if (message.type() === 'error') browserErrors.push(message.text()); });
  page.on('pageerror', (error) => browserErrors.push(error.message));
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/runs');
  await page.getByLabel('Correlation name').fill('application');
  await page.getByLabel('Exact stored value').fill('fachtracing');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByText('include exact node in business graph').first()).toBeVisible();
  await expect(page.getByText('select source inputs for graph analysis').first()).toBeVisible();
  await page.getByRole('link', { name: 'Explain' }).first().click();
  await expect(page.getByRole('complementary', { name: 'Run explanation' })).toContainText(/\d+ steps/);
  await mkdir('test-results/dogfood', { recursive: true });
  try {
    const nodes = page.locator('.svelte-flow__node');
    const layoutError = page.getByRole('alert');
    await expect.poll(async () => await nodes.count() > 0 || await layoutError.count() > 0, { timeout: 15_000 }).toBe(true);
    if (await layoutError.count() > 0) throw new Error(`graph layout failed: ${await layoutError.textContent()}`);
    expect(browserErrors, `browser errors: ${browserErrors.join(' | ')}`).toEqual([]);
    await expect(nodes.first()).toBeVisible();
    await expect(page.getByLabel('Full path')).toBeChecked();
  } finally {
    await page.screenshot({ path: 'test-results/dogfood/fachtracing-viewer.png', fullPage: true });
  }
});

test('previews a generated developer graph JSON file without storage', async ({ page }) => {
  const dogfoodDirectory = process.env.FACHTRACING_DOGFOOD_DIRECTORY;
  test.skip(!dogfoodDirectory, 'Generated Fachtracing artifacts are required');
  const graphDirectory = join(dogfoodDirectory!, 'graphs');
  const graphFiles = (await readdir(graphDirectory)).filter((name) => name.endsWith('.json')).sort();
  expect(graphFiles.length).toBeGreaterThan(0);
  const graphPath = join(graphDirectory, graphFiles[0]);
  const document = JSON.parse(await readFile(graphPath, 'utf8')) as { graph: { label: string; nodes: unknown[]; edges: unknown[] } };

  await page.goto('/graphs');
  const nonReadRequests: string[] = [];
  page.on('request', (request) => {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method())) nonReadRequests.push(`${request.method()} ${request.url()}`);
  });
  const storageBefore = await page.evaluate(() => ({ local: localStorage.length, session: sessionStorage.length }));
  await expect(page.getByRole('heading', { name: 'Preview a graph JSON' })).toBeVisible();
  await page.getByLabel('Developer graph JSON').setInputFiles(graphPath);
  await expect(page.getByRole('heading', { name: document.graph.label })).toBeVisible();
  await expect(page.getByText(`${document.graph.nodes.length} nodes`, { exact: true })).toBeVisible();
  await expect(page.getByText(`${document.graph.edges.length} edges`, { exact: true })).toBeVisible();
  await expect(page.locator('.svelte-flow__node')).toHaveCount(document.graph.nodes.length, { timeout: 15_000 });
  await expect(page.getByText('Your file is not uploaded or saved.')).toBeVisible();
  expect(nonReadRequests).toEqual([]);
  await expect.poll(() => page.evaluate(() => ({ local: localStorage.length, session: sessionStorage.length }))).toEqual(storageBefore);
  await mkdir('test-results/dogfood', { recursive: true });
  await page.screenshot({ path: 'test-results/dogfood/fachtracing-graph-preview.png', fullPage: true });
});

test('keeps the generated decision explanation clear at every supported width', async ({ page }) => {
  test.skip(!process.env.FACHTRACING_DOGFOOD_DIRECTORY, 'Generated Fachtracing artifacts are required');
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await mkdir('test-results/visual', { recursive: true });
  await page.setViewportSize({ width: 1440, height: 900 });
  await openGeneratedFachtracingRun(page);

  const header = page.locator('.decision-header');
  const desktopInspector = page.locator('.desktop-inspector');
  const sheetTrigger = page.getByRole('button', { name: 'Open run explanation' });
  await expect(desktopInspector).toBeVisible();
  await expect(sheetTrigger).toBeHidden();
  expect((await header.boundingBox())!.height).toBeLessThanOrEqual(170);
  const inspectorWidth = (await desktopInspector.boundingBox())!.width;
  expect(inspectorWidth).toBeGreaterThanOrEqual(320);
  expect(inspectorWidth).toBeLessThanOrEqual(520);
  const resizer = page.getByRole('slider', { name: 'Explanation panel width' });
  await resizer.focus();
  await page.keyboard.press('ArrowLeft');
  await expect(resizer).toHaveAttribute('aria-valuenow', String(inspectorWidth + 16));
  await page.keyboard.press('ArrowRight');
  await page.evaluate(() => (document.activeElement as HTMLElement | null)?.blur());
  await expect(page.locator('.business-node[data-run-state="current"]')).toHaveCount(1);
  const stepNumbers = await desktopInspector.locator('.step-number').allTextContents();
  expect(stepNumbers).toEqual(stepNumbers.map((_, index) => String(index + 1)));
  expect(await desktopInspector.evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(true);

  const edgeLabels = page.locator('.business-edge-label');
  for (const label of await edgeLabels.allTextContents()) expect(label.length).toBeLessThanOrEqual(32);
  const edgeNodeCollisions = await page.evaluate(() => {
    const labels = [...document.querySelectorAll<HTMLElement>('.business-edge-label')].map((item) => item.getBoundingClientRect());
    const nodes = [...document.querySelectorAll<HTMLElement>('.business-node')].map((item) => item.getBoundingClientRect());
    return labels.filter((label) => nodes.some((node) => label.left < node.right && label.right > node.left && label.top < node.bottom && label.bottom > node.top)).length;
  });
  expect(edgeNodeCollisions).toBe(0);
  if (await page.locator('.svelte-flow__node').count() <= 8) await expect(page.locator('.business-minimap')).toHaveCount(0);
  await page.screenshot({ path: 'test-results/visual/decision-1440-light.png', fullPage: true });

  await page.setViewportSize({ width: 1024, height: 800 });
  await expect(desktopInspector).toBeVisible();
  await expect(sheetTrigger).toBeHidden();
  await expect.poll(async () => page.evaluate(() => {
    const canvas = document.querySelector<HTMLElement>('.canvas')?.getBoundingClientRect();
    if (!canvas) return false;
    return [...document.querySelectorAll<HTMLElement>('.business-node')].every((node) => {
      const box = node.getBoundingClientRect();
      return box.left >= canvas.left && box.right <= canvas.right && box.top >= canvas.top && box.bottom <= canvas.bottom;
    });
  })).toBe(true);
  await page.screenshot({ path: 'test-results/visual/decision-1024-light.png', fullPage: true });

  await page.setViewportSize({ width: 900, height: 800 });
  await expect(desktopInspector).toBeHidden();
  await expect(sheetTrigger).toBeVisible();
  await sheetTrigger.click();
  const dialog = page.getByRole('dialog', { name: 'Run explanation' });
  await expect(dialog).toBeVisible();
  await page.keyboard.press('Tab');
  expect(await page.evaluate(() => document.activeElement?.closest('.sheet-content') !== null)).toBe(true);
  await page.keyboard.press('Escape');
  await expect(dialog).toBeHidden();
  await expect(sheetTrigger).toBeFocused();
  await sheetTrigger.click();
  await expect(dialog).toBeVisible();
  await page.locator('.sheet-overlay').click({ position: { x: 10, y: 10 } });
  await expect(dialog).toBeHidden();

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(sheetTrigger).toBeVisible();
  await page.screenshot({ path: 'test-results/visual/decision-390-closed-light.png', fullPage: true });
  await sheetTrigger.click();
  await expect(dialog).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  const sheetBox = (await dialog.boundingBox())!;
  expect(sheetBox.x).toBeGreaterThanOrEqual(0);
  expect(sheetBox.x + sheetBox.width).toBeLessThanOrEqual(390);
  await page.screenshot({ path: 'test-results/visual/decision-390-sheet-light.png', fullPage: true });
  await page.keyboard.press('Escape');

  await page.setViewportSize({ width: 1440, height: 900 });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await expect(desktopInspector).toBeVisible();
  await page.screenshot({ path: 'test-results/visual/decision-1440-dark.png', fullPage: true });
});

test('renders the complete generated node grammar in both themes', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.emulateMedia({ reducedMotion: 'reduce', colorScheme: 'light' });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/graphs');
  await page.getByLabel('Developer graph JSON').setInputFiles(generatedGraphFile(7));
  await expect(page.locator('.svelte-flow__node')).toHaveCount(7, { timeout: 15_000 });
  for (const kind of ['entry', 'predicate', 'choice', 'computation', 'dispatch', 'outcome', 'coverage_gap']) {
    await expect(page.locator(`.business-node--${kind}`)).toBeVisible();
  }
  const grammar = await page.evaluate(() => ({
    entryRadius: getComputedStyle(document.querySelector<HTMLElement>('.business-node--entry')!).borderRadius,
    choiceClip: getComputedStyle(document.querySelector<HTMLElement>('.business-node--choice')!).clipPath,
    dispatchClip: getComputedStyle(document.querySelector<HTMLElement>('.business-node--dispatch')!).clipPath,
    gapClip: getComputedStyle(document.querySelector<HTMLElement>('.business-node--coverage_gap')!).clipPath,
    outcomeShadow: getComputedStyle(document.querySelector<HTMLElement>('.business-node--outcome')!).boxShadow,
    colors: [...document.querySelectorAll<HTMLElement>('.business-node')].map((node) => getComputedStyle(node).borderLeftColor)
  }));
  expect(grammar.entryRadius).not.toBe('12px');
  expect(grammar.choiceClip).not.toBe('none');
  expect(grammar.dispatchClip).not.toBe('none');
  expect(grammar.gapClip).not.toBe('none');
  expect(grammar.outcomeShadow).toContain('inset');
  expect(new Set(grammar.colors).size).toBe(7);
  await page.screenshot({ path: 'test-results/visual/node-grammar-1440-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/visual/node-grammar-1440-dark.png', fullPage: true });
});

test('keeps a generated 250-node graph navigable', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.emulateMedia({ reducedMotion: 'reduce', colorScheme: 'light' });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/graphs');
  await page.getByLabel('Developer graph JSON').setInputFiles(generatedGraphFile(250));
  await expect(page.locator('.svelte-flow__node')).toHaveCount(250, { timeout: 15_000 });
  await expect(page.locator('.business-minimap')).toHaveCount(0);
  await expect(page.getByLabel('250-node graph navigation')).toContainText('Search to jump');
  await page.getByPlaceholder('Find a node').fill('node-249');
  await page.getByPlaceholder('Find a node').press('Enter');
  await expect(page.getByRole('status')).toContainText('Focused generated outcome 249');
  await expect.poll(async () => page.evaluate(() => {
    const canvas = document.querySelector<HTMLElement>('.preview-canvas')?.getBoundingClientRect();
    const node = document.querySelector<HTMLElement>('.svelte-flow__node[data-id="node-249"]')?.getBoundingClientRect();
    return Boolean(canvas && node && node.width > 100 && node.left >= canvas.left && node.right <= canvas.right && node.top >= canvas.top && node.bottom <= canvas.bottom);
  })).toBe(true);
  await page.screenshot({ path: 'test-results/visual/graph-250-focused.png', fullPage: true });
});
