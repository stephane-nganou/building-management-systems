import { expect, test } from '@playwright/test';

import {
  confirmationLink,
  expectNavLabels,
  openSignIn,
  signIn,
  submitSignIn,
  uniqueEmail,
} from './support';

test.describe('registration', () => {
  test('the sign in page offers a way to register', async ({ page }) => {
    await openSignIn(page);
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

    // The first sign in stops until the address is confirmed. Following the
    // link in the same browser confirms it and finishes signing in.
    await page.getByRole('button', { name: /sign in/i }).click();
    await submitSignIn(page, email, 'a-good-secret');
    await expect(page.getByRole('heading', { name: /email verification/i })).toBeVisible();
    await page.goto(await confirmationLink(page, email));
    await page.waitForURL(/localhost:4200/);

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

  /**
   * Opened on another device, such as a phone, the link cannot finish the sign
   * in that started elsewhere. It confirms the address, then offers the way back
   * to the application rather than leaving the owner on Keycloak's page.
   */
  test('an address confirmed on another device leads back to the application', async ({ page, browser }) => {
    const email = uniqueEmail('phone');
    await page.goto('/register');
    await page.getByLabel('First name').fill('Paul');
    await page.getByLabel('Last name').fill('Phone');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password').fill('a-good-secret');
    await page.getByRole('button', { name: /create account/i }).click();
    await page.getByRole('button', { name: /sign in/i }).click();
    await submitSignIn(page, email, 'a-good-secret');
    const link = await confirmationLink(page, email);

    const phone = await browser.newPage();
    await phone.goto(link);
    await phone.getByRole('link', { name: /click here to proceed/i }).click();
    await expect(phone.getByRole('heading', { name: /email address verified/i })).toBeVisible();
    await phone.getByRole('link', { name: /back to application/i }).click();
    // The application's address is its landing page, which a visitor signs in from.
    await phone.getByRole('button', { name: 'Sign in', exact: true }).click();
    await signIn(phone, email, 'a-good-secret');
    await expect(phone.locator('.spine-foot .role')).toHaveText(/owner/i);
    await phone.close();
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
