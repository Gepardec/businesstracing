import { expect, test } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

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
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto('/runs');
  await page.getByLabel('Correlation name').fill('application');
  await page.getByLabel('Exact stored value').fill('fachtracing');
  await page.getByRole('button', { name: 'Search' }).click();
  await expect(page.getByText('include exact node in business graph').first()).toBeVisible();
  await expect(page.getByText('select source inputs for graph analysis').first()).toBeVisible();
  await page.getByRole('link', { name: 'Explain' }).first().click();
  await expect(page.getByRole('complementary', { name: 'Run explanation' })).toContainText(/\d+ steps/);
  await expect(page.locator('.svelte-flow__node').first()).toBeVisible();
  await expect(page.getByLabel('Full path')).toBeChecked();
  await mkdir('test-results/dogfood', { recursive: true });
  await page.screenshot({ path: 'test-results/dogfood/fachtracing-viewer.png', fullPage: true });
});
