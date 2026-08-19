import { defineConfig, devices } from '@playwright/test';

const executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  globalSetup: './e2e/global-setup.ts',
  use: {
    baseURL: 'http://127.0.0.1:4177',
    trace: 'retain-on-failure',
    ...(executablePath ? { launchOptions: { executablePath } } : {})
  },
  webServer: {
    command: 'node build',
    port: 4177,
    reuseExistingServer: !process.env.CI,
    env: { ...process.env, HOST: '127.0.0.1', PORT: '4177' }
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
});
