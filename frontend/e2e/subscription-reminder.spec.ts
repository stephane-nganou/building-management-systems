import { expect, test } from '@playwright/test';

import { DEMO_ADMIN, chooseNewPassword, signIn, signOut, submitSignIn, uniqueEmail } from './support';

/** A calendar day as a date input takes it, counted from today in the browser's own zone. */
function day(offset: number): string {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${String(date.getDate()).padStart(2, '0')}`;
}

/**
 * An owner whose subscription ends within a week is told so on every screen,
 * and the reminder leads to where they can see everything about it and whom to
 * call to renew.
 */
test('an owner is reminded before their subscription ends and can see whom to contact', async ({ page }) => {
  const email = uniqueEmail('renewal');

  await page.goto('/');
  await signIn(page, DEMO_ADMIN.username, DEMO_ADMIN.password);
  await page.getByRole('button', { name: /add owner/i }).click();
  await page.getByLabel('First name').fill('Rita');
  await page.getByLabel('Last name').fill('Renew');
  await page.getByLabel('Email').fill(email);
  await page.getByRole('button', { name: /create owner/i }).click();
  const temporaryPassword = await page.getByLabel('Temporary password').inputValue();
  await page.getByRole('button', { name: /done/i }).click();

  // Replace the thirty day trial with a subscription whose last day is in three
  // days. It starts yesterday because the browser's today and the server's, in
  // UTC, can be a day apart around midnight.
  await page.getByRole('row', { name: new RegExp(email) }).getByRole('button', { name: 'Subscription' }).click();
  const subscription = page.getByRole('dialog', { name: 'Subscription of Rita Renew' });
  await subscription.getByRole('button', { name: 'End subscription now' }).click();
  await page
    .getByRole('dialog', { name: /end the subscription of rita renew/i })
    .getByRole('button', { name: 'End subscription now' })
    .click();
  await subscription.getByLabel('From').fill(day(-1));
  await subscription.getByLabel('Last day').fill(day(3));
  await subscription.locator('footer').getByRole('button', { name: 'Add period' }).click();
  await subscription.locator('footer').getByRole('button', { name: 'Close' }).click();
  await signOut(page);

  await submitSignIn(page, email, temporaryPassword);
  await chooseNewPassword(page, 'renewal-own-secret');

  const reminder = page.getByRole('status').filter({ hasText: /your subscription ends on/i });
  await expect(reminder).toContainText(/\d days from now/);
  await reminder.getByRole('link', { name: 'Manage subscription' }).click();

  await expect(page).toHaveURL(/\/subscription$/);
  await expect(page.getByRole('heading', { name: 'Subscription', level: 1 })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Customer service' })).toContainText('support@hausbuch.example');
});
