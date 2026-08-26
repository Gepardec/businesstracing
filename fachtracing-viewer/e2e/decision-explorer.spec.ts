import { expect, test } from '@playwright/test';
import { mkdir, readFile, readdir } from 'node:fs/promises';
import { basename, delimiter, join, resolve } from 'node:path';
import { displayedEdgeLabel } from '../src/lib/graph/edge-label';
import { crossingGraphFile, cycleGraphFile, duplicateGraphFile, fanInGraphFile, generatedBranchingGraphFile, longShortcutGraphFile } from './visual-fixtures';

function outgoingEdges<T extends { from: string }>(edges: readonly T[], nodeId: string): T[] {
  return edges.filter((edge) => edge.from === nodeId);
}

function reaches(edges: readonly { from: string; to: string }[], start: string, target: string): boolean {
  const pending = [start];
  const visited = new Set<string>();
  while (pending.length > 0) {
    const nodeId = pending.shift()!;
    if (nodeId === target) return true;
    if (visited.has(nodeId)) continue;
    visited.add(nodeId);
    pending.push(...outgoingEdges(edges, nodeId).map((edge) => edge.to));
  }
  return false;
}

async function canvasGeometry(page: import('@playwright/test').Page) {
  return page.evaluate(() => {
    const nodeRects = new Map([...document.querySelectorAll<HTMLElement>('.svelte-flow__node[data-id]')].map((node) => [node.dataset.id!, node.getBoundingClientRect()]));
    const intrusions: string[] = [];
    const endpointViolations: string[] = [];
    const routes: { id: string; source: string; target: string; path: string }[] = [];
    const onBoundary = (point: DOMPoint, box: DOMRect): boolean => {
      const tolerance = 4;
      const withinX = point.x >= box.left - tolerance && point.x <= box.right + tolerance;
      const withinY = point.y >= box.top - tolerance && point.y <= box.bottom + tolerance;
      const boundaryDistance = Math.min(
        Math.abs(point.x - box.left), Math.abs(point.x - box.right),
        Math.abs(point.y - box.top), Math.abs(point.y - box.bottom)
      );
      return withinX && withinY && boundaryDistance <= tolerance;
    };
    for (const edge of document.querySelectorAll<SVGGElement>('.svelte-flow__edge[data-source-node][data-target-node]')) {
      const path = edge.querySelector<SVGPathElement>('path[data-route-edge]');
      if (!path) continue;
      const id = path.dataset.routeEdge!;
      const source = edge.dataset.sourceNode!;
      const target = edge.dataset.targetNode!;
      routes.push({ id, source, target, path: path.getAttribute('d') ?? '' });
      const matrix = path.getScreenCTM();
      if (!matrix) continue;
      const length = path.getTotalLength();
      const startPoint = path.getPointAtLength(0);
      const endPoint = path.getPointAtLength(length);
      const start = new DOMPoint(startPoint.x, startPoint.y).matrixTransform(matrix);
      const end = new DOMPoint(endPoint.x, endPoint.y).matrixTransform(matrix);
      if (!onBoundary(start, nodeRects.get(source)!)) endpointViolations.push(`${id} does not start at ${source}`);
      if (path.dataset.shared !== 'true' && !onBoundary(end, nodeRects.get(target)!)) endpointViolations.push(`${id} does not end at ${target}`);
      for (let offset = 0; offset <= length; offset += 3) {
        const point = path.getPointAtLength(offset);
        const screen = new DOMPoint(point.x, point.y).matrixTransform(matrix);
        for (const [nodeId, box] of nodeRects) {
          if (nodeId === source || nodeId === target) continue;
          if (screen.x > box.left + 1 && screen.x < box.right - 1 && screen.y > box.top + 1 && screen.y < box.bottom - 1) {
            intrusions.push(`${id} enters ${nodeId}`);
            break;
          }
        }
      }
    }
    const labels = [...document.querySelectorAll<HTMLElement>('.business-edge-label')].map((label) => ({ id: label.dataset.edgeLabel!, box: label.getBoundingClientRect() }));
    const labelNodeCollisions = labels.flatMap(({ id, box }) => [...nodeRects].filter(([, node]) => box.left < node.right && box.right > node.left && box.top < node.bottom && box.bottom > node.top).map(([nodeId]) => `${id} covers ${nodeId}`));
    const labelCollisions: string[] = [];
    labels.forEach((first, index) => labels.slice(index + 1).forEach((second) => {
      if (first.box.left < second.box.right && first.box.right > second.box.left && first.box.top < second.box.bottom && first.box.bottom > second.box.top) labelCollisions.push(`${first.id} covers ${second.id}`);
    }));
    const routeLabelCollisions: string[] = [];
    for (const { id: labelId, box } of labels) {
      for (const path of document.querySelectorAll<SVGPathElement>('path[data-route-edge]')) {
        const routeId = path.dataset.routeEdge!;
        if (routeId === labelId) continue;
        const matrix = path.getScreenCTM();
        if (!matrix) continue;
        const length = path.getTotalLength();
        for (let offset = 0; offset <= length; offset += 2) {
          const point = path.getPointAtLength(offset);
          const screen = new DOMPoint(point.x, point.y).matrixTransform(matrix);
          if (screen.x > box.left - 1 && screen.x < box.right + 1 && screen.y > box.top - 1 && screen.y < box.bottom + 1) {
            routeLabelCollisions.push(`${routeId} crosses ${labelId}`);
            break;
          }
        }
      }
    }
    const parallelRoutes = [...Map.groupBy(routes, (route) => `${route.source}\u0000${route.target}`).values()]
      .filter((group) => group.length > 1)
      .map((group) => ({ ids: group.map((route) => route.id), distinctPaths: new Set(group.map((route) => route.path)).size }));
    const handleOpacity = [...document.querySelectorAll<HTMLElement>('.business-handle')].map((handle) => getComputedStyle(handle).opacity);
    return { routeCount: routes.length, intrusions, endpointViolations, labelNodeCollisions, labelCollisions, routeLabelCollisions, parallelRoutes, handleOpacity };
  });
}

async function canvasEndpointViolations(page: import('@playwright/test').Page): Promise<string[]> {
  return page.evaluate(() => {
    const nodeRects = new Map([...document.querySelectorAll<HTMLElement>('.svelte-flow__node[data-id]')]
      .map((node) => [node.dataset.id!, node.getBoundingClientRect()]));
    const violations: string[] = [];
    const onBoundary = (point: DOMPoint, box: DOMRect): boolean => {
      const tolerance = 4;
      const withinX = point.x >= box.left - tolerance && point.x <= box.right + tolerance;
      const withinY = point.y >= box.top - tolerance && point.y <= box.bottom + tolerance;
      const boundaryDistance = Math.min(
        Math.abs(point.x - box.left), Math.abs(point.x - box.right),
        Math.abs(point.y - box.top), Math.abs(point.y - box.bottom)
      );
      return withinX && withinY && boundaryDistance <= tolerance;
    };

    for (const edge of document.querySelectorAll<SVGGElement>('.svelte-flow__edge[data-source-node][data-target-node]')) {
      const path = edge.querySelector<SVGPathElement>('path[data-route-edge]');
      const source = nodeRects.get(edge.dataset.sourceNode!);
      const target = nodeRects.get(edge.dataset.targetNode!);
      const matrix = path?.getScreenCTM();
      if (!path || !source || !target || !matrix) continue;
      const length = path.getTotalLength();
      const startPoint = path.getPointAtLength(0);
      const endPoint = path.getPointAtLength(length);
      const start = new DOMPoint(startPoint.x, startPoint.y).matrixTransform(matrix);
      const end = new DOMPoint(endPoint.x, endPoint.y).matrixTransform(matrix);
      if (!onBoundary(start, source)) violations.push(`${path.dataset.routeEdge} does not start at ${edge.dataset.sourceNode}`);
      if (path.dataset.shared !== 'true' && !onBoundary(end, target)) violations.push(`${path.dataset.routeEdge} does not end at ${edge.dataset.targetNode}`);
    }
    return violations;
  });
}

async function openGeneratedFachtracingRun(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/runs');
  await page.getByLabel('Correlation name').fill('application');
  await page.getByLabel('Exact stored value').fill('fachtracing');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByRole('link', { name: 'Explain' }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Explain' }).first().click();
  await expect(page.locator('.svelte-flow__node').first()).toBeVisible({ timeout: 15_000 });
}

async function showFullDetail(page: import('@playwright/test').Page): Promise<void> {
  const button = page.getByRole('button', { name: 'Full detail' });
  if (await button.count() === 0) return;
  await button.click();
  await expect(button).toHaveAttribute('aria-pressed', 'true');
}

async function showOverview(page: import('@playwright/test').Page): Promise<void> {
  const button = page.getByRole('button', { name: 'Overview' });
  if (await button.getAttribute('aria-pressed') !== 'true') await button.click();
  await expect(button).toHaveAttribute('aria-pressed', 'true');
}

async function waitForGraphLayout(page: import('@playwright/test').Page): Promise<void> {
  await expect(page.locator('.svelte-flow__node').first()).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText('Arranging', { exact: false })).toHaveCount(0, { timeout: 15_000 });
}

function graphSearchStatus(page: import('@playwright/test').Page) {
  return page.locator('.flow-panel + .sr-only[role="status"]');
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

function generatedNodeGrammarFile(): { name: string; mimeType: string; buffer: Buffer } {
  const kinds = ['ENTRY', 'PREDICATE', 'CHOICE', 'COMPUTATION', 'DISPATCH', 'COVERAGE_GAP', 'OUTCOME'];
  const nodes = kinds.map((kind, index) => ({ id: `node-${index}`, kind, label: `generated ${kind.toLowerCase().replace('_', ' ')}`, attributes: {} }));
  const decisionNodes = nodes.slice(1, 3);
  const actionNodes = nodes.slice(3, -1);
  const edges = decisionNodes.map((node, index) => ({ id: `edge-entry-${index}`, from: nodes[0].id, to: node.id, outcome: index % 2 ? 'no' : 'yes' }));
  actionNodes.forEach((node, index) => {
    edges.push({ id: `edge-decision-${index}`, from: decisionNodes[index % decisionNodes.length].id, to: node.id, outcome: index % 2 ? 'no' : 'yes' });
    edges.push({ id: `edge-outcome-${index}`, from: node.id, to: nodes.at(-1)!.id, outcome: 'yes' });
  });
  const document = {
    schema: 'fachtracing-developer-graph/v1',
    sourceOrigins: [{ id: 'generated', kind: 'GENERATED', identity: 'browser node grammar', checksum: 'fixture' }], sourceFiles: [],
    graph: {
      id: 'generated-node-grammar', version: 1, label: 'generated node grammar', entryNodeId: nodes[0].id, completeness: 'INCOMPLETE',
      nodes, edges, coverageGaps: [{ nodeId: nodes.find((node) => node.kind === 'COVERAGE_GAP')!.id, description: 'generated coverage gap' }]
    }
  };
  return { name: 'generated-node-grammar.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(document)) };
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
  test.skip(!process.env.FACHTRACING_DOGFOOD_DIRECTORY || !process.env.FACHTRACING_DATABASE_URL, 'Generated Fachtracing artifacts and PostgreSQL are required');
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
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(graphPath);
  await expect(page.getByRole('heading', { name: document.graph.label })).toBeVisible();
  await expect(page.getByText(`${document.graph.nodes.length} nodes`, { exact: true })).toBeVisible();
  await expect(page.getByText(`${document.graph.edges.length} edges`, { exact: true })).toBeVisible();
  await expect(page.locator('.svelte-flow__node')).toHaveCount(document.graph.nodes.length, { timeout: 15_000 });
  await showOverview(page);
  const geometry = await canvasGeometry(page);
  expect(geometry.routeCount).toBe(document.graph.edges.length);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  expect(geometry.labelNodeCollisions).toEqual([]);
  expect(geometry.labelCollisions).toEqual([]);
  expect(geometry.routeLabelCollisions).toEqual([]);
  expect(geometry.handleOpacity.every((opacity) => opacity === '0')).toBe(true);
  await expect(page.getByText('Your file is not uploaded or saved.')).toBeVisible();
  expect(nonReadRequests).toEqual([]);
  await expect.poll(() => page.evaluate(() => ({ local: localStorage.length, session: sessionStorage.length }))).toEqual(storageBefore);
  await mkdir('test-results/dogfood', { recursive: true });
  await page.screenshot({ path: 'test-results/dogfood/fachtracing-graph-preview-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/dogfood/fachtracing-graph-preview-dark.png', fullPage: true });
});

test('keeps the generated decision explanation clear at every supported width', async ({ page }) => {
  test.skip(!process.env.FACHTRACING_DOGFOOD_DIRECTORY || !process.env.FACHTRACING_DATABASE_URL, 'Generated Fachtracing artifacts and PostgreSQL are required');
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
  const currentStyle = await page.locator('.business-node[data-run-state="current"]').evaluate((node) => ({ border: getComputedStyle(node).borderTopWidth, outline: getComputedStyle(node).outlineStyle }));
  expect(currentStyle).toEqual({ border: '3px', outline: 'none' });
  const stepNumbers = await desktopInspector.locator('.step-number').allTextContents();
  expect(stepNumbers).toEqual(stepNumbers.map((_, index) => String(index + 1)));
  expect(await desktopInspector.evaluate((element) => element.scrollWidth <= element.clientWidth)).toBe(true);

  const edgeLabels = page.locator('.business-edge-label');
  for (const label of await edgeLabels.allTextContents()) expect(label.length).toBeLessThanOrEqual(32);
  const dogfoodGeometry = await canvasGeometry(page);
  expect(dogfoodGeometry.routeCount).toBeGreaterThan(0);
  expect(dogfoodGeometry.intrusions).toEqual([]);
  expect(dogfoodGeometry.endpointViolations).toEqual([]);
  expect(dogfoodGeometry.labelNodeCollisions).toEqual([]);
  expect(dogfoodGeometry.labelCollisions).toEqual([]);
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
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(generatedNodeGrammarFile());
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(7, { timeout: 15_000 });
  for (const kind of ['entry', 'predicate', 'choice', 'computation', 'dispatch', 'outcome', 'coverage_gap']) {
    await expect(page.locator(`.business-node--${kind}`)).toBeVisible();
  }
  const grammar = await page.evaluate(() => ({
    entryRadius: getComputedStyle(document.querySelector<HTMLElement>('.business-node--entry')!).borderRadius,
    choiceClip: getComputedStyle(document.querySelector<HTMLElement>('.business-node--choice')!).clipPath,
    dispatchClip: getComputedStyle(document.querySelector<HTMLElement>('.business-node--dispatch')!).clipPath,
    gapClip: getComputedStyle(document.querySelector<HTMLElement>('.business-node--coverage_gap')!).clipPath,
    outcomeRadius: getComputedStyle(document.querySelector<HTMLElement>('.business-node--outcome')!).borderRadius,
    colors: [...document.querySelectorAll<HTMLElement>('.business-node')].map((node) => getComputedStyle(node).getPropertyValue('--node-color')),
    borders: [...document.querySelectorAll<HTMLElement>('.business-node')].map((node) => getComputedStyle(node).borderTopWidth),
    handles: [...document.querySelectorAll<HTMLElement>('.business-handle')].map((handle) => getComputedStyle(handle).opacity)
  }));
  expect(grammar.entryRadius).not.toBe('12px');
  expect(grammar.choiceClip).not.toBe('none');
  expect(grammar.dispatchClip).not.toBe('none');
  expect(grammar.gapClip).not.toBe('none');
  expect(grammar.outcomeRadius).not.toBe('12px');
  expect(new Set(grammar.colors).size).toBe(7);
  expect(grammar.borders.every((width) => width === '1px')).toBe(true);
  expect(grammar.handles.every((opacity) => opacity === '0')).toBe(true);
  await page.screenshot({ path: 'test-results/visual/node-grammar-1440-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/visual/node-grammar-1440-dark.png', fullPage: true });
  await page.emulateMedia({ colorScheme: 'light', forcedColors: 'active', reducedMotion: 'reduce' });
  await page.screenshot({ path: 'test-results/visual/node-grammar-1440-monochrome.png', fullPage: true });
});

test('keeps a generated 250-node graph navigable', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.emulateMedia({ reducedMotion: 'reduce', colorScheme: 'light' });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(generatedGraphFile(250));
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(250, { timeout: 15_000 });
  await expect(page.locator('.business-minimap')).toHaveCount(0);
  await expect(page.getByLabel('250-node graph navigation')).toContainText('Search to jump');
  await page.getByPlaceholder('Find a node').fill('node-249');
  await page.getByPlaceholder('Find a node').press('Enter');
  await expect(graphSearchStatus(page)).toContainText('Selected generated outcome 249');
  await expect.poll(async () => page.evaluate(() => {
    const canvas = document.querySelector<HTMLElement>('.preview-canvas')?.getBoundingClientRect();
    const node = document.querySelector<HTMLElement>('.svelte-flow__node[data-id="node-249"]')?.getBoundingClientRect();
    return Boolean(canvas && node && node.width > 100 && node.left >= canvas.left && node.right <= canvas.right && node.top >= canvas.top && node.bottom <= canvas.bottom);
  })).toBe(true);
  await page.screenshot({ path: 'test-results/visual/graph-250-focused.png', fullPage: true });
});

test('keeps optional real graphs readable in Explore and Overview modes', async ({ page }) => {
  const graphPaths = (process.env.FACHTRACING_REAL_GRAPH_FILES ?? '').split(delimiter).filter(Boolean);
  test.skip(graphPaths.length === 0, 'Optional real graph files are required');
  await mkdir('test-results/real-graphs', { recursive: true });
  await page.setViewportSize({ width: 1440, height: 1_000 });

  for (const [index, graphPath] of graphPaths.entries()) {
    const document = JSON.parse(await readFile(graphPath, 'utf8')) as {
      nodes: Array<{ id: string; kind: string; label: string }>;
      edges: Array<{ id: string; from: string; to: string; outcome: string }>;
      decision: string;
      entryNodeIds: string[];
    };
    const entryNode = document.nodes.find((node) => node.id === document.entryNodeIds[0]);
    expect(entryNode, `${basename(graphPath)} must declare a valid entry node`).toBeDefined();
    await page.goto('/graphs');
    const switchToLight = page.getByRole('button', { name: 'Use light theme' });
    if (await switchToLight.count()) await switchToLight.click();
    await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(graphPath);
    await waitForGraphLayout(page);
    const guide = page.getByLabel('Graph explanation');
    await expect(guide.getByText('Decision summary')).toBeVisible();
    const readableButton = page.getByRole('button', { name: 'Readable' });
    if (await readableButton.count()) await expect(readableButton).toHaveAttribute('aria-pressed', 'true');
    const localGeometry = await canvasGeometry(page);
    expect(localGeometry.intrusions).toEqual([]);
    expect(localGeometry.endpointViolations).toEqual([]);
    expect(localGeometry.labelNodeCollisions).toEqual([]);
    expect(localGeometry.labelCollisions).toEqual([]);
    expect(localGeometry.routeLabelCollisions).toEqual([]);
    expect(await page.locator('.business-node').evaluateAll((nodes) => nodes.every((node) => {
      const box = node.getBoundingClientRect();
      return box.width >= 160 && box.height >= 60;
    }))).toBe(true);
    await expect(page.getByText(/nearby nodes · Select a step to continue/)).toBeVisible();
    await expect(page.locator('.business-edge-label', { hasText: /^Path \d+$/ })).toHaveCount(0);
    const stem = basename(graphPath, '.json');
    await page.screenshot({ path: `test-results/real-graphs/${stem}-readable-light.png`, fullPage: true });
    const firstContinuation = guide.locator('.next-connections button').first();
    if (await firstContinuation.count()) {
      const targetLabel = (await firstContinuation.locator('strong').innerText()).trim();
      await firstContinuation.click();
      await expect(guide.getByText('Selected step')).toBeVisible();
      await expect(guide.locator('.current-step h2')).toHaveText(targetLabel);
      await page.screenshot({ path: `test-results/real-graphs/${stem}-guided-step.png`, fullPage: true });
    }
    if (index === 0) {
      await page.setViewportSize({ width: 860, height: 900 });
      await expect.poll(() => page.evaluate(() => {
        const panel = document.querySelector<HTMLElement>('.graph-guide')?.getBoundingClientRect();
        const canvas = document.querySelector<HTMLElement>('.flow-panel')?.getBoundingClientRect();
        const nodes = [...document.querySelectorAll<HTMLElement>('.business-node')].map((node) => node.getBoundingClientRect());
        const wide = Boolean(panel && canvas && panel.width > canvas.width * 0.8);
        const clear = Boolean(panel && nodes.every((node) => node.bottom + 8 <= panel.top));
        return `${wide}:${clear}:${Math.round(panel?.top ?? 0)}:${Math.round(Math.max(0, ...nodes.map((node) => node.bottom)))}`;
      })).toMatch(/^true:true:/);
      await page.screenshot({ path: `test-results/real-graphs/${stem}-guided-narrow.png`, fullPage: true });
      await page.setViewportSize({ width: 1_440, height: 1_000 });
    }
    const labelledDecisions = document.nodes.filter((node) => {
      const outgoing = outgoingEdges(document.edges, node.id);
      return new Set(outgoing.map((edge) => edge.to)).size > 1
        && outgoing.length > 1
        && outgoing.every((edge) => displayedEdgeLabel(edge.outcome, outgoing.length, outgoing.findIndex((candidate) => candidate.id === edge.id)) !== null);
    });
    const labelledDecision = labelledDecisions.find((node) => {
      const targets = outgoingEdges(document.edges, node.id).map((edge) => edge.to);
      return targets.some((source, sourceIndex) => targets.some((target, targetIndex) => sourceIndex !== targetIndex && reaches(document.edges, source, target)));
    }) ?? labelledDecisions[0];
    if (labelledDecision) {
      const outgoing = outgoingEdges(document.edges, labelledDecision.id);
      await showFullDetail(page);
      await waitForGraphLayout(page);
      await page.getByPlaceholder('Find a node').fill(labelledDecision.id);
      await page.getByPlaceholder('Find a node').press('Enter');
      await expect(page.locator(`.svelte-flow__node.selected[data-id="${labelledDecision.id}"]`)).toBeVisible();
      for (const [branchIndex, edge] of outgoing.entries()) {
        const label = displayedEdgeLabel(edge.outcome, outgoing.length, branchIndex)!;
        await expect(page.locator('.business-edge-label', { hasText: new RegExp(`^${label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`) }).first()).toBeVisible();
      }
      expect(await page.locator('path[data-route-edge][data-branch="true"]').evaluateAll((paths) => paths.every((path) => {
        const style = getComputedStyle(path);
        return style.opacity === '1' && style.strokeDasharray === 'none';
      }))).toBe(true);
      await page.screenshot({ path: `test-results/real-graphs/${stem}-branch-review.png`, fullPage: true });
      if (await readableButton.count()) {
        await readableButton.click();
        await waitForGraphLayout(page);
      }
    }
    await page.getByRole('button', { name: 'Overview' }).click();
    await expect(page.getByRole('button', { name: 'Overview' })).toHaveAttribute('aria-pressed', 'true');
    await expect(guide.getByText('Topology map')).toBeVisible();
    const readableSequences = page.locator('.business-node.node-sequence');
    if (await readableSequences.count()) {
      await expect(readableSequences.locator('.sequence-more')).toHaveCount(await readableSequences.count());
      expect(await readableSequences.locator('p').evaluateAll((labels) => labels.every((label) => !label.textContent?.includes('→')))).toBe(true);
    }
    await page.screenshot({ path: `test-results/real-graphs/${stem}-readable-overview-light.png`, fullPage: true });
    await page.getByRole('button', { name: 'Explore' }).click();

    await showFullDetail(page);
    await waitForGraphLayout(page);
    await expect(page.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true');
    const entry = page.locator(`.svelte-flow__node[data-id="${entryNode!.id}"] .business-node`);
    await expect(entry).toBeVisible();
    expect((await entry.boundingBox())!.width).toBeGreaterThanOrEqual(140);
    await page.screenshot({ path: `test-results/real-graphs/${stem}-full-explore.png`, fullPage: true });
    await showOverview(page);
    await expect(page.locator('.svelte-flow__node')).toHaveCount(document.nodes.length, { timeout: 15_000 });
    await expect(page.locator('.view-summary')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Overview' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('.business-node p')).toHaveCount(document.nodes.length);
    expect(await page.locator('.business-node p').evaluateAll((labels) => labels.filter((label) => {
      const style = getComputedStyle(label);
      return style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0';
    }).length)).toBe(0);
    expect(await canvasEndpointViolations(page)).toEqual([]);
    const outgoingBySource = new Map<string, typeof document.edges>();
    for (const edge of document.edges) outgoingBySource.set(edge.from, [...(outgoingBySource.get(edge.from) ?? []), edge]);
    const expectedLabelEdgeIds = document.edges.filter((edge) => {
      const outgoing = outgoingBySource.get(edge.from)!;
      return displayedEdgeLabel(edge.outcome, outgoing.length, outgoing.findIndex((candidate) => candidate.id === edge.id)) !== null;
    }).map((edge) => edge.id);
    const alwaysVisibleLabelEdgeIds = await page.locator('path[data-route-edge]').evaluateAll((paths, labelledIds) => {
      const expected = new Set(labelledIds as string[]);
      return paths
        .filter((path) => (path as SVGPathElement).dataset.feedback === 'false')
        .map((path) => (path as SVGPathElement).dataset.routeEdge!)
        .filter((id) => expected.has(id));
    }, expectedLabelEdgeIds);
    await expect(page.locator('.business-edge-label')).toHaveCount(alwaysVisibleLabelEdgeIds.length);
    for (const edgeId of alwaysVisibleLabelEdgeIds) await expect(page.locator(`[data-edge-label="${edgeId}"]`)).toBeAttached();
    expect(await page.locator('path[data-route-edge][data-feedback="false"]').evaluateAll((paths) => paths.every((path) => {
      const style = getComputedStyle(path);
      return style.opacity === '1' && style.strokeDasharray === 'none';
    }))).toBe(true);
    const outcomesWithIncoming = document.nodes
      .filter((node) => node.kind === 'OUTCOME' && document.edges.some((edge) => edge.to === node.id))
      .map((node) => node.id);
    expect(await page.evaluate((outcomeIds) => outcomeIds.filter((nodeId) => {
      const escaped = CSS.escape(nodeId);
      const direct = document.querySelector<SVGPathElement>(`.svelte-flow__edge[data-target-node="${escaped}"] path[data-route-edge][data-shared="false"]`);
      const sharedArrow = document.querySelector<SVGPolygonElement>(`.shared-segment[data-target-node="${escaped}"] polygon`);
      return !(direct?.getAttribute('marker-end') || sharedArrow);
    }), outcomesWithIncoming)).toEqual([]);
    await page.locator(`.svelte-flow__node[data-id="${entryNode!.id}"]`).hover();
    await expect(page.getByLabel('Zoomed node label')).toContainText(entryNode!.label);
    await page.screenshot({ path: `test-results/real-graphs/${stem}-full-overview.png`, fullPage: true });

    const selected = [...document.nodes].sort((first, second) =>
      (outgoingBySource.get(second.id)?.length ?? 0) - (outgoingBySource.get(first.id)?.length ?? 0) || first.id.localeCompare(second.id)
    )[0];
    await page.getByPlaceholder('Find a node').fill(selected.id);
    await page.getByPlaceholder('Find a node').press('Enter');
    await expect(page.locator(`.svelte-flow__node.selected[data-id="${selected.id}"]`)).toBeVisible();
    await expect(graphSearchStatus(page)).toContainText(`Selected ${selected.label}`);
    await expect(page.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('.node-context-dimmed, .context-dimmed')).toHaveCount(0);
    expect(await page.locator('.business-node').evaluateAll((items) => items.every((item) => getComputedStyle(item).opacity === '1'))).toBe(true);
    const selectedGeometry = () => page.evaluate((nodeId) => {
      const selectedNode = document.querySelector<HTMLElement>(`.svelte-flow__node[data-id="${CSS.escape(nodeId)}"]`)?.getBoundingClientRect();
      const canvas = document.querySelector<HTMLElement>('.flow-panel')?.getBoundingClientRect();
      const overlays = [...document.querySelectorAll<HTMLElement>('.canvas-toolbar, .svelte-flow__controls, .business-minimap, .large-graph-guide, .svelte-flow__attribution')]
        .map((element) => element.getBoundingClientRect());
      if (!selectedNode || !canvas) return { insideCanvas: false, overlayClear: false, selectedNode: null, canvas: null, overlays: [] };
      const insideCanvas = selectedNode.left >= canvas.left + 16 && selectedNode.right <= canvas.right - 16
        && selectedNode.top >= canvas.top + 16 && selectedNode.bottom <= canvas.bottom - 16;
      const overlayClear = overlays.every((overlay) => selectedNode.right + 16 <= overlay.left || selectedNode.left - 16 >= overlay.right || selectedNode.bottom + 16 <= overlay.top || selectedNode.top - 16 >= overlay.bottom);
      const plainRect = (rect: DOMRect) => ({ left: rect.left, top: rect.top, right: rect.right, bottom: rect.bottom, width: rect.width, height: rect.height });
      return { insideCanvas, overlayClear, selectedNode: plainRect(selectedNode), canvas: plainRect(canvas), overlays: overlays.map(plainRect) };
    }, selected.id);
    await expect.poll(async () => {
      const geometry = await selectedGeometry();
      return { insideCanvas: geometry.insideCanvas, overlayClear: geometry.overlayClear };
    }).toEqual({ insideCanvas: true, overlayClear: true });
    await page.screenshot({ path: `test-results/real-graphs/${stem}-selected.png`, fullPage: true });

    const viewport = page.locator('.svelte-flow__viewport');
    const transformBeforeMissingSearch = await viewport.getAttribute('style');
    await page.getByPlaceholder('Find a node').fill('__node_that_does_not_exist__');
    await page.getByPlaceholder('Find a node').press('Enter');
    await expect(graphSearchStatus(page)).toContainText('No matching node was found');
    await expect(page.locator(`.svelte-flow__node.selected[data-id="${selected.id}"]`)).toBeVisible();
    await expect(viewport).toHaveAttribute('style', transformBeforeMissingSearch!);
    await page.getByPlaceholder('Find a node').fill(selected.id);

    if (index === 0) {
      const duplicate = document.nodes.find((node) => document.nodes.filter((candidate) => candidate.label === node.label).length > 1);
      if (duplicate) {
        const duplicates = document.nodes.filter((node) => node.label.toLowerCase().includes(duplicate.label.toLowerCase())).sort((first, second) => first.id.localeCompare(second.id));
        await page.getByPlaceholder('Find a node').fill(duplicate.label);
        await page.getByPlaceholder('Find a node').press('Enter');
        await expect(graphSearchStatus(page)).toContainText(`Match 1 of ${duplicates.length}`);
        await expect(page.locator(`.svelte-flow__node.selected[data-id="${duplicates[0].id}"]`)).toBeVisible();
        await page.getByPlaceholder('Find a node').fill(selected.id);
        await page.getByPlaceholder('Find a node').press('Enter');
      }
    }

    await page.getByRole('button', { name: 'Use dark theme' }).click();
    await expect(page.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator(`.svelte-flow__node.selected[data-id="${selected.id}"]`)).toBeVisible();
    await page.setViewportSize({ width: 1_200, height: 900 });
    await expect.poll(async () => {
      const geometry = await selectedGeometry();
      return { insideCanvas: geometry.insideCanvas, overlayClear: geometry.overlayClear };
    }).toEqual({ insideCanvas: true, overlayClear: true });
    await page.setViewportSize({ width: 1_440, height: 1_000 });
    await expect.poll(async () => {
      const geometry = await selectedGeometry();
      return { insideCanvas: geometry.insideCanvas, overlayClear: geometry.overlayClear };
    }).toEqual({ insideCanvas: true, overlayClear: true });
    await page.screenshot({ path: `test-results/real-graphs/${stem}-full-explore-dark.png`, fullPage: true });
    await page.getByRole('button', { name: 'Overview' }).click();
    await expect(page.getByRole('button', { name: 'Overview' })).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('.business-node p')).toHaveCount(document.nodes.length);
    await expect(page.locator(`.svelte-flow__node.selected[data-id="${selected.id}"]`)).toBeVisible();
    await page.screenshot({ path: `test-results/real-graphs/${stem}-full-overview-dark.png`, fullPage: true });

    if (await readableButton.count()) {
      await readableButton.click();
      await expect(readableButton).toHaveAttribute('aria-pressed', 'true');
      await waitForGraphLayout(page);
      await page.screenshot({ path: `test-results/real-graphs/${stem}-readable-dark.png`, fullPage: true });
    }
  }
});

test('ignores an older real-graph layout after file replacement', async ({ page }) => {
  const graphPaths = (process.env.FACHTRACING_REAL_GRAPH_FILES ?? '').split(delimiter).filter(Boolean);
  test.skip(graphPaths.length < 2, 'Two optional real graph files are required');
  const replacementPath = graphPaths.at(-1)!;
  const replacement = JSON.parse(await readFile(replacementPath, 'utf8')) as { decision: string; nodes: unknown[]; edges: unknown[] };

  await page.goto('/graphs');
  const input = page.getByLabel('Graph JSON', { exact: true });
  await input.setInputFiles(graphPaths[0]);
  await input.setInputFiles(replacementPath);
  await expect(page.getByRole('heading', { name: replacement.decision })).toBeVisible();
  await showFullDetail(page);
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(replacement.nodes.length, { timeout: 15_000 });
  await expect(page.locator('path[data-route-edge]')).toHaveCount(replacement.edges.length);
  await expect(page.getByRole('alert')).toHaveCount(0);
});

test('previews the stable business graph V1 contract from the repository', async ({ page }) => {
  const graphPath = resolve('..', 'conformance/spring-petclinic/src/test/resources/oracles/owner-search-business.json');
  const document = JSON.parse(await readFile(graphPath, 'utf8')) as { decision: string; nodes: unknown[]; edges: unknown[] };
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(graphPath);
  await expect(page.getByRole('heading', { name: document.decision })).toBeVisible();
  await showFullDetail(page);
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(document.nodes.length, { timeout: 15_000 });
  await expect(page.locator('path[data-route-edge]')).toHaveCount(document.edges.length);
  await expect(page.getByRole('alert')).toHaveCount(0);
  const geometry = await canvasGeometry(page);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  expect(geometry.labelNodeCollisions).toEqual([]);
  expect(geometry.labelCollisions).toEqual([]);
  expect(geometry.routeLabelCollisions).toEqual([]);
  expect(geometry.parallelRoutes.length).toBeGreaterThan(0);
  for (const group of geometry.parallelRoutes) expect(group.distinctPaths).toBe(group.ids.length);
  await mkdir('test-results/visual', { recursive: true });
  await page.screenshot({ path: 'test-results/visual/business-v1-preview-light.png', fullPage: true });
});

test('renders branching and parallel routes without overlap in both themes', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.emulateMedia({ reducedMotion: 'reduce', colorScheme: 'light' });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(generatedBranchingGraphFile());
  await showFullDetail(page);
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(4, { timeout: 15_000 });
  await expect(page.locator('path[data-route-edge]')).toHaveCount(5);
  const geometry = await canvasGeometry(page);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  expect(geometry.labelNodeCollisions).toEqual([]);
  expect(geometry.labelCollisions).toEqual([]);
  expect(geometry.routeLabelCollisions).toEqual([]);
  expect(geometry.parallelRoutes).toHaveLength(1);
  expect(geometry.parallelRoutes[0].distinctPaths).toBe(geometry.parallelRoutes[0].ids.length);
  expect(geometry.handleOpacity.every((opacity) => opacity === '0')).toBe(true);
  await page.screenshot({ path: 'test-results/visual/branch-routes-1440-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/visual/branch-routes-1440-dark.png', fullPage: true });
});

test('renders a twelve-source convergence as one inspectable junction', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.emulateMedia({ reducedMotion: 'reduce', colorScheme: 'light' });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(fanInGraphFile());
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(14, { timeout: 15_000 });
  await expect(page.locator('.svelte-flow__edge')).toHaveCount(24);
  const junction = page.locator('[data-junction]');
  const trunk = page.locator('[data-shared-segment]');
  await expect(junction).toHaveCount(1);
  await expect(junction).toHaveAttribute('data-member-count', '12');
  await expect(trunk).toHaveCount(1);
  await expect(trunk).toHaveAttribute('data-member-count', '12');
  await page.locator('.svelte-flow__edge[data-id="edge-merge-0"]').focus();
  await expect(trunk).toHaveClass(/shared-segment--inspect/);
  await expect(junction).toHaveClass(/junction--inspect/);
  await expect(page.getByText('12 routes converge before combined result.')).toBeAttached();
  const geometry = await canvasGeometry(page);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  expect(geometry.labelNodeCollisions).toEqual([]);
  expect(geometry.labelCollisions).toEqual([]);
  expect(geometry.routeLabelCollisions).toEqual([]);
  await page.screenshot({ path: 'test-results/visual/fan-in-1440-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/visual/fan-in-1440-dark.png', fullPage: true });
  await page.getByPlaceholder('Find a node').fill('combined result');
  await page.getByPlaceholder('Find a node').press('Enter');
  await expect(graphSearchStatus(page)).toContainText('Selected combined result');
  await page.screenshot({ path: 'test-results/visual/fan-in-focused-1440-dark.png', fullPage: true });
});

test('renders duplicate context and a cycle without changing semantic node count', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(duplicateGraphFile());
  await expect(page.locator('.svelte-flow__node')).toHaveCount(4, { timeout: 15_000 });
  await showOverview(page);
  await expect(page.getByText('1 of 2')).toBeVisible();
  await expect(page.getByText('2 of 2')).toBeVisible();
  await expect(page.locator('.svelte-flow__node[data-id="first-check"]')).toHaveAttribute('aria-label', /PREDICATE: email exists.*Occurrence 1 of 2/);
  await page.screenshot({ path: 'test-results/visual/duplicates-1440-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/visual/duplicates-1440-dark.png', fullPage: true });

  await page.getByRole('button', { name: 'Choose another file' }).click();
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(cycleGraphFile());
  await expect(page.locator('.svelte-flow__node')).toHaveCount(4, { timeout: 15_000 });
  await showOverview(page);
  await expect(page.locator('.structural-region')).toHaveCount(0);
  const feedbackRoute = page.locator('path[data-feedback="true"]').first();
  await expect(feedbackRoute).toBeAttached();
  expect(await feedbackRoute.evaluate((route) => getComputedStyle(route).strokeDasharray)).toBe('none');
  expect(await feedbackRoute.evaluate((route) => getComputedStyle(route).opacity)).toBe('1');
  const feedbackRouteId = await feedbackRoute.getAttribute('data-route-edge');
  await page.locator(`.svelte-flow__edge[data-id="${feedbackRouteId}"]`).focus();
  await expect.poll(() => feedbackRoute.evaluate((route) => getComputedStyle(route).opacity)).toBe('1');
  await expect(page.locator('.semantic-node-list summary')).toHaveText('Accessible graph list (4 nodes, 4 edges)');
  const geometry = await canvasGeometry(page);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  expect(geometry.labelNodeCollisions).toEqual([]);
  expect(geometry.labelCollisions).toEqual([]);
  expect(geometry.routeLabelCollisions).toEqual([]);
  await page.screenshot({ path: 'test-results/visual/cycle-1440-dark.png', fullPage: true });
  await page.getByRole('button', { name: 'Use light theme' }).click();
  await page.screenshot({ path: 'test-results/visual/cycle-1440-light.png', fullPage: true });
});

test('keeps a long forward shortcut solid and inside the normal flow', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(longShortcutGraphFile());
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(6, { timeout: 15_000 });
  const longRoute = page.locator('path.long-route');
  await expect(longRoute).toHaveCount(1);
  await expect(page.locator('path.secondary-route')).toHaveCount(0);
  expect(await longRoute.evaluate((route) => getComputedStyle(route).strokeDasharray)).toBe('none');
  expect(await longRoute.evaluate((route) => getComputedStyle(route).opacity)).toBe('1');
  const geometry = await canvasGeometry(page);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  expect(geometry.labelNodeCollisions).toEqual([]);
  expect(geometry.labelCollisions).toEqual([]);
  expect(geometry.routeLabelCollisions).toEqual([]);
  await page.screenshot({ path: 'test-results/visual/long-route-1440-light.png', fullPage: true });
});

test('renders crossings as bridges without adding graph nodes', async ({ page }) => {
  await mkdir('test-results/visual', { recursive: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto('/graphs');
  await page.getByLabel('Graph JSON', { exact: true }).setInputFiles(crossingGraphFile());
  await showOverview(page);
  await expect(page.locator('.svelte-flow__node')).toHaveCount(7, { timeout: 15_000 });
  await expect(page.locator('.svelte-flow__edge')).toHaveCount(12);
  await expect(page.locator('[data-route-crossing]').first()).toBeAttached();
  await expect(page.locator('.semantic-node-list summary')).toHaveText('Accessible graph list (7 nodes, 12 edges)');
  const geometry = await canvasGeometry(page);
  expect(geometry.intrusions).toEqual([]);
  expect(geometry.endpointViolations).toEqual([]);
  await page.screenshot({ path: 'test-results/visual/crossings-1440-light.png', fullPage: true });
  await page.getByRole('button', { name: 'Use dark theme' }).click();
  await page.screenshot({ path: 'test-results/visual/crossings-1440-dark.png', fullPage: true });
});
