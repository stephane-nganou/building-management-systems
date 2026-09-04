import { defineConfig, devices } from '@playwright/test';

/**
 * The suite drives the real stack: Postgres, Keycloak, the backend and the
 * built frontend, started by scripts/e2e. Nothing is stubbed, because the parts
 * worth testing here are exactly the ones a stub would fake: the Keycloak
 * redirect, the roles in the token and the permissions that decide what a
 * signed in user may see.
 */
export default defineConfig({
  testDir: './e2e',
  // The specs share one realm and one database, so they run one at a time.
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:4200',
    // The app reads the browser's language on a first visit, so pinning it is
    // what keeps the specs asserting English wherever they run.
    locale: 'en-GB',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
