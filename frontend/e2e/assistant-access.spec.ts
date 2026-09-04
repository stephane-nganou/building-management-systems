import { expect, test } from '@playwright/test';

import {
  DEMO_OWNER,
  chooseNewPassword,
  navLabels,
  signIn,
  signOut,
  submitSignIn,
  uniqueEmail,
} from './support';

/**
 * The heart of the product's access rules: an owner creates an assistant, hands
 * over a password, and that assistant sees only what they were granted. Nothing
 * else may even hint at existing.
 */
test('an owner creates an assistant who then sees only what they were granted', async ({ page }) => {
  const email = uniqueEmail('helper');

  await page.goto('/');
  await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);

  await page.getByRole('link', { name: 'Assistants' }).click();
  await page.getByRole('button', { name: /add assistant/i }).first().click();

  await page.getByLabel('First name').fill('Karl');
  await page.getByLabel('Last name').fill('Helfer');
  await page.getByLabel('Their email').fill(email);
  await page.getByLabel('View expenses').check();
  await page.getByRole('button', { name: /create assistant/i }).click();

  // The password is shown once, for the owner to pass on.
  await expect(page.getByRole('heading', { name: /hand these over/i })).toBeVisible();
  const temporaryPassword = await page.getByLabel('Temporary password').inputValue();
  expect(temporaryPassword).not.toBe('');
  await page.getByRole('button', { name: /done/i }).click();

  await expect(page.locator('table.sheet')).toContainText(email);
  await signOut(page);

  // The assistant signs in for the first time and has to choose their own password.
  await submitSignIn(page, email, temporaryPassword);
  await chooseNewPassword(page, 'assistant-own-secret');

  await expect(page.locator('.spine-foot .role')).toHaveText(/assisting/i);
  expect(await navLabels(page)).toEqual(['Expenses']);

  // Asking for a screen they were not granted takes them somewhere they may be,
  // and the Assistants screen is never theirs.
  await page.goto('/invoices');
  await expect(page).toHaveURL(/\/expenses$/);
  await page.goto('/assistants');
  await expect(page).toHaveURL(/\/expenses$/);
});
