import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';

const authFile = path.join(__dirname, 'e2e', '.auth', 'admin.json');

/**
 * Step 7.4 — viewport projects for responsive QA (375 / 768 / 1440).
 * Chromium only. One shared login via setup project.
 *
 *   npm run e2e:responsive
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  workers: 2,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  timeout: 60_000,
  use: {
    baseURL: process.env['IMS_UI_BASE'] || 'http://localhost:4200',
    ...devices['Desktop Chrome'],
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts/ },
    {
      name: 'mobile-375',
      dependencies: ['setup'],
      use: { viewport: { width: 375, height: 812 }, storageState: authFile },
      testIgnore: /auth\.setup\.ts/,
    },
    {
      name: 'tablet-768',
      dependencies: ['setup'],
      use: { viewport: { width: 768, height: 1024 }, storageState: authFile },
      testIgnore: /auth\.setup\.ts/,
    },
    {
      name: 'desktop-1440',
      dependencies: ['setup'],
      use: { viewport: { width: 1440, height: 900 }, storageState: authFile },
      testIgnore: /auth\.setup\.ts/,
    },
  ],
});
