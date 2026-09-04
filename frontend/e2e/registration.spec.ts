import { expect, test } from '@playwright/test';

import { expectNavLabels, signIn, uniqueEmail } from './support';

test.describe('registration', () => {
  test('the sign in page offers a way to register', async ({ page }) => {
    await page.goto('/');
    // The app has no session, so Keycloak's sign in page is what loads.
    await expect(page.getByRole('heading', { name: /sign in to your account/i })).toBeVisible();

    await page.getByRole('link', { name: /register here/i }).click();

    await expect(page).toHaveURL(/\/register$/);
    await expect(page.getByRole('heading', { name: /create your account/i })).toBeVisible();
  });

  test('a new landlord signs up and lands on their own empty portfolio', async ({ page }) => {
    const email = uniqueEmail('landlord');
    await page.goto('/register');

    await page.getByLabel('First name').fill('Petra');
    await page.getByLabel('Last name').fill('Pichler');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill('a-good-secret');
    await page.getByRole('button', { name: /create account/i }).click();

    await expect(page.getByRole('heading', { name: /your account is ready/i })).toBeVisible();

    await page.getByRole('button', { name: /sign in/i }).click();
    await signIn(page, email, 'a-good-secret');

    // A registered user is an owner, so every screen is theirs.
    await expect(page.locator('.spine-foot .role')).toHaveText(/owner/i);
    await expectNavLabels(page, [
      'Overview',
      'Buildings',
      'Apartments',
      'Tenants',
      'Expenses',
      'Invoices',
      'Profit and loss',
      'Assistants',
    ]);
  });

  test('the same email cannot register twice', async ({ page }) => {
    const email = uniqueEmail('twice');

    for (const attempt of [1, 2]) {
      await page.goto('/register');
      await page.getByLabel('First name').fill('Dana');
      await page.getByLabel('Last name').fill('Doppelt');
      await page.getByLabel('Email').fill(email);
      await page.getByLabel('Password').fill('a-good-secret');
      await page.getByRole('button', { name: /create account/i }).click();

      if (attempt === 1) {
        await expect(page.getByRole('heading', { name: /your account is ready/i })).toBeVisible();
      } else {
        await expect(page.locator('.notice')).toContainText(`already exists for ${email}`);
      }
    }
  });
});
