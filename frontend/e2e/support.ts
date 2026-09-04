import { Page, expect } from '@playwright/test';

/** The demo owner seeded by the realm export. */
export const DEMO_OWNER = { username: 'owner', password: 'owner' };

/** Emails have to be unique per run, because accounts are never deleted. */
export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}@example.test`;
}

/**
 * Fills Keycloak's sign in form. Where that lands depends on the account: the
 * app for most, the update password page for one signing in the first time.
 */
export async function submitSignIn(page: Page, username: string, password: string): Promise<void> {
  await page.getByLabel(/username or email/i).fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: /sign in/i }).click();
}

/** Signs in an account that is ready to use, and waits for the app to take over. */
export async function signIn(page: Page, username: string, password: string): Promise<void> {
  await submitSignIn(page, username, password);
  await page.waitForURL(/localhost:4200/);
}

/**
 * Keycloak makes an account with a temporary password choose a new one before
 * it lets them in, which is exactly what an assistant meets on their first
 * sign in.
 */
export async function chooseNewPassword(page: Page, password: string): Promise<void> {
  await expect(page.getByRole('heading', { name: /update password/i })).toBeVisible();
  await page.getByLabel('New Password', { exact: true }).fill(password);
  await page.getByLabel(/confirm password/i).fill(password);
  await page.getByRole('button', { name: /submit/i }).click();
  await page.waitForURL(/localhost:4200/);
}

/** Signs out through the app, landing back on Keycloak's sign in page. */
export async function signOut(page: Page): Promise<void> {
  await page.getByRole('button', { name: /sign out/i }).click();
  await page.waitForURL(/\/realms\/bms\/protocol\/openid-connect/);
}

/**
 * Asserts the sidebar entries the signed in user can see.
 *
 * <p>Retrying matters here. Reading the labels into an array and comparing that
 * takes one snapshot, and a snapshot can be taken before the browser has
 * applied a change the test just asked for: signing in, or switching language.
 * `toHaveText` polls until the sidebar settles, so a busy machine is slow
 * rather than red.
 */
export async function expectNavLabels(page: Page, labels: string[]): Promise<void> {
  await expect(page.locator('aside.spine nav a')).toHaveText(labels);
}
