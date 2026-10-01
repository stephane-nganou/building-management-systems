import { expect, test } from '@playwright/test';

import { DEMO_OWNER, openSignIn, signIn } from './support';

/**
 * The path a landlord actually walks: a building, a unit inside it, and the
 * unit showing up against the right building.
 */
test('an owner adds a building and an apartment inside it', async ({ page }) => {
  const building = `Hauptstrasse ${Date.now()}`;
  // Labels are unique per building, but the list shows every building at once.
  const apartment = `2B-${Date.now()}`;

  await openSignIn(page);
  await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);

  await page.getByRole('link', { name: 'Buildings' }).click();
  await page.getByRole('button', { name: 'Add building' }).first().click();
  await page.getByLabel('Name').fill(building);
  await page.getByLabel('Street').fill('Hauptstrasse 1');
  await page.getByLabel('City').fill('Berlin');
  await page.getByLabel('Postal code').fill('10115');
  await page.locator('.panel').getByRole('button', { name: 'Add building' }).click();

  await expect(page.locator('table.sheet')).toContainText(building);

  await page.getByRole('link', { name: 'Apartments' }).click();
  await page.getByRole('button', { name: 'Add apartment' }).first().click();
  await page.locator('.panel').getByLabel('Building').selectOption({ label: building });
  await page.getByLabel('Number or name').fill(apartment);
  await page.getByLabel('Monthly rent').fill('850');
  await page.locator('.panel').getByRole('button', { name: 'Add apartment' }).click();

  const row = page.locator('table.sheet tbody tr', { hasText: apartment });
  await expect(row).toContainText(building);
});

/** A building abroad keeps its books in its own currency, down to the apartment's rent. */
test('a building in Cameroon shows its rent in CFA francs', async ({ page }) => {
  const building = `Rue de la Joie ${Date.now()}`;
  const apartment = `1A-${Date.now()}`;

  await openSignIn(page);
  await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);

  await page.getByRole('link', { name: 'Buildings' }).click();
  await page.getByRole('button', { name: 'Add building' }).first().click();
  await page.getByLabel('Name').fill(building);
  await page.getByLabel('City').fill('Douala');
  await page.getByLabel('Currency').selectOption('XAF');
  await page.locator('.panel').getByRole('button', { name: 'Add building' }).click();
  await expect(page.locator('table.sheet')).toContainText(building);

  await page.getByRole('link', { name: 'Apartments' }).click();
  await page.getByRole('button', { name: 'Add apartment' }).first().click();
  await page.locator('.panel').getByLabel('Building').selectOption({ label: building });
  await page.getByLabel('Number or name').fill(apartment);
  await page.getByLabel('Monthly rent').fill('150000');
  await page.locator('.panel').getByRole('button', { name: 'Add apartment' }).click();

  // Whole francs: the CFA franc has no cents to show.
  const row = page.locator('table.sheet tbody tr', { hasText: apartment });
  await expect(row).toContainText('FCFA');
  await expect(row).toContainText('150,000');
  await expect(row).not.toContainText('150,000.00');
});
