import { expect, test } from '@playwright/test';

import {
  DEMO_ADMIN,
  chooseNewPassword,
  expectNavLabels,
  signIn,
  signOut,
  submitSignIn,
  uniqueEmail,
} from './support';

/**
 * The administrator signs a customer up, and their subscription is what lets
 * them change anything: once it has ended, everything stays readable and every
 * change is refused with the reason.
 */
test('an administrator signs an owner up and ends their subscription, leaving it read only', async ({ page }) => {
  const email = uniqueEmail('customer');

  await page.goto('/');
  await signIn(page, DEMO_ADMIN.username, DEMO_ADMIN.password);

  // An administrator owns nothing, so the accounts are all there is.
  await expect(page).toHaveURL(/\/accounts$/);
  await expectNavLabels(page, ['Accounts']);
  await expect(page.locator('.spine-foot .role')).toHaveText('Administrator');

  await page.getByRole('button', { name: /add owner/i }).click();
  await page.getByLabel('First name').fill('Nina');
  await page.getByLabel('Last name').fill('Neu');
  await page.getByLabel('Email').fill(email);
  await page.getByRole('button', { name: /create owner/i }).click();

  await expect(page.getByRole('heading', { name: /hand these over/i })).toBeVisible();
  const temporaryPassword = await page.getByLabel('Temporary password').inputValue();
  await page.getByRole('button', { name: /done/i }).click();

  const row = page.getByRole('row', { name: new RegExp(email) });
  await expect(row).toContainText('Active');

  await row.getByRole('button', { name: 'Subscription' }).click();
  const subscription = page.getByRole('dialog', { name: 'Subscription of Nina Neu' });
  await subscription.getByRole('button', { name: 'End subscription now' }).click();
  await page
    .getByRole('dialog', { name: /end the subscription of nina neu/i })
    .getByRole('button', { name: 'End subscription now' })
    .click();
  // The footer's Close, not the header's icon of the same name.
  await subscription.locator('footer').getByRole('button', { name: 'Close' }).click();
  await expect(row).toContainText('Expired');
  await signOut(page);

  // The new owner can still sign in and read, but is told why nothing changes.
  await submitSignIn(page, email, temporaryPassword);
  await chooseNewPassword(page, 'customer-own-secret');
  // The toasts are a status region too, so the banner is found by what it says.
  await expect(page.getByRole('status').filter({ hasText: /your subscription ended/i })).toBeVisible();

  await page.getByRole('link', { name: 'Buildings' }).click();
  await page.getByRole('button', { name: /add building/i }).first().click();
  await page.getByLabel('Name').fill('Refused Strasse 1');
  await page.getByRole('dialog').getByRole('button', { name: /add building/i }).click();
  await expect(page.getByRole('dialog').getByRole('alert')).toHaveText(
    'The subscription has expired. The data can be read but no longer changed.',
  );
});
