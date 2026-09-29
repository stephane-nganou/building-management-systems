import { Request, expect, test } from '@playwright/test';

import { DEMO_OWNER, signIn } from './support';

const APP_ORIGIN = 'http://localhost:4200/';
const KEYCLOAK_ORIGIN = 'http://localhost:8081/';

/** Anything the application asks for itself, as opposed to a page style or font. */
function isApplicationTraffic(request: Request): boolean {
  return request.resourceType() === 'xhr' || request.resourceType() === 'fetch';
}

/**
 * The rule this application is built to: it knows one host, and it is its own.
 *
 * <p>Signing in is a redirect the backend issues, so the browser does visit
 * Keycloak once, on a page it navigates to rather than one the application
 * fetches. From the moment the portfolio is on screen, nothing the application
 * asks for may leave its own origin, and nothing at all may reach Keycloak.
 *
 * <p>A test that only read the source could be satisfied by a stale import.
 * This watches the wire.
 */
test('the application never calls anything but its own backend', async ({ page }) => {
  await page.goto('/');
  await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);
  await expect(page.locator('aside.spine')).toBeVisible();

  const foreignCalls: string[] = [];
  const keycloakRequests: string[] = [];
  page.on('request', (request) => {
    const url = request.url();
    if (isApplicationTraffic(request) && !url.startsWith(APP_ORIGIN)) {
      foreignCalls.push(`${request.method()} ${url}`);
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

  // The one thing that does leave this origin is the webfont in styles.css,
  // which is why this counts calls rather than every request. A font is not a
  // service the application talks to.
  expect(foreignCalls).toEqual([]);
  expect(keycloakRequests).toEqual([]);
});
