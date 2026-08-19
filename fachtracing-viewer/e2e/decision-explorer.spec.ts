import { expect, test } from '@playwright/test';
import { mkdir, readFile, readdir } from 'node:fs/promises';
import { join } from 'node:path';

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
