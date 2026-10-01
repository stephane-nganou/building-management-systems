import { expect, test } from '@playwright/test';

import { DEMO_OWNER, openSignIn, signIn } from './support';

const APP_ORIGIN = 'http://localhost:4200/';
const KEYCLOAK_ORIGIN = 'http://localhost:8081/';

/**
 * The rule this application is built to: it knows one host, and it is its own.
 *
 * <p>Signing in is a redirect the backend issues, so the browser does visit
 * Keycloak once, on a page it navigates to rather than one the application
 * fetches. From the moment the portfolio is on screen, nothing may leave its own
 * origin: not a call, and not a stylesheet or a font either, since those are
 * served from here too. Nothing at all may reach Keycloak.
 *
 * <p>A test that only read the source could be satisfied by a stale import.
 * This watches the wire.
 */
test('the application never calls anything but its own backend', async ({ page }) => {
  await openSignIn(page);
  await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);
  await expect(page.locator('aside.spine')).toBeVisible();

  const foreignRequests: string[] = [];
  const keycloakRequests: string[] = [];
  page.on('request', (request) => {
    const url = request.url();
    if (!url.startsWith(APP_ORIGIN)) {
      foreignRequests.push(`${request.resourceType()} ${url}`);
    }
    if (url.startsWith(KEYCLOAK_ORIGIN)) {
      keycloakRequests.push(`${request.resourceType()} ${url}`);
    }
  });

  // Walk the app: every screen, each fetching its own data.
  for (const label of ['Buildings', 'Apartments', 'Tenants', 'Expenses', 'Invoices']) {
    await page.getByRole('link', { name: label }).click();
    await expect(page.getByRole('heading', { name: label, exact: false }).first()).toBeVisible();
  }
  await page.reload();
  await expect(page.locator('aside.spine')).toBeVisible();

  expect(foreignRequests).toEqual([]);
  expect(keycloakRequests).toEqual([]);
});
