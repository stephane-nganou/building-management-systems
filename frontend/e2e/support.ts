import { Page, expect } from '@playwright/test';

/** The demo owner seeded by the realm export. */
export const DEMO_OWNER = { username: 'owner', password: 'owner' };

/** Emails have to be unique per run, because accounts are never deleted. */
export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}@example.test`;
}

/**
 * Fills the sign in form.
 *
 * <p>This is the one page of Keycloak anybody still sees, and the application
 * does not send anyone here itself: it asks the backend to sign the browser in,
 * and the backend is what redirects. Where it lands depends on the account: the
 * portfolio for most, our own password screen for one signing in the first time.
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
 * An assistant signs in holding a password their owner read out to them, and the
 * application shows them nothing else until they have replaced it. The screen is
 * ours, not Keycloak's: the obligation is recorded on our own record, so that
 * nobody has to leave the application to discharge it.
 */
export async function chooseNewPassword(page: Page, password: string): Promise<void> {
  await expect(page.getByRole('heading', { name: /choose your password/i })).toBeVisible();
  await page.getByLabel('New password', { exact: true }).fill(password);
  await page.getByLabel('Repeat it').fill(password);
  await page.getByRole('button', { name: /save password/i }).click();
  await expect(page.locator('aside.spine')).toBeVisible();
}

/**
 * Signs out through the app. The backend ends the session at both ends, and the
 * browser comes back to an application that has nobody signed in, which asks to
 * sign in again.
 */
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
