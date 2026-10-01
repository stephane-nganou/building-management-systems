import { expect, test } from '@playwright/test';

import { DEMO_OWNER, openSignIn, signIn } from './support';

const TITLE = 'Your buildings, rents and receipts in one ledger';

test.describe('landing page', () => {
  test('a visitor sees what Hausbuch does rather than a sign in form', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { level: 1 })).toHaveText(TITLE);
    await expect(page).toHaveURL(/localhost:4200\/$/);
    await expect(page.getByText('30 days free. No card needed, nothing to cancel.')).toBeVisible();
    await expect(page.getByRole('link', { name: 'contact@example.com' })).toHaveAttribute(
      'href',
      'mailto:contact@example.com',
    );
  });

  test('the free month starts from the landing page', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('main').getByRole('link', { name: 'Start your free month' }).click();

    await expect(page).toHaveURL(/\/register$/);
    await expect(page.getByRole('heading', { name: /create your account/i })).toBeVisible();
  });

  test('a French visitor reads it in French', async ({ browser }) => {
    const context = await browser.newContext({ locale: 'fr-FR' });
    const page = await context.newPage();
    await page.goto('/');

    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      'Vos immeubles, loyers et reçus dans un seul registre',
    );
    await context.close();
  });

  test('a screen of the app still asks a visitor to sign in', async ({ page }) => {
    await page.goto('/buildings');

    await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible();
  });

  test('somebody signed in goes past it into the app', async ({ page }) => {
    await openSignIn(page);
    await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);
    await expect(page.locator('aside.spine')).toBeVisible();

    await page.goto('/');
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole('heading', { name: TITLE })).toHaveCount(0);
  });
});
