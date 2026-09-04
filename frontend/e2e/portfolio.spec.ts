import { expect, test } from '@playwright/test';

import { DEMO_OWNER, signIn } from './support';

/**
 * The path a landlord actually walks: a building, a unit inside it, and the
 * unit showing up against the right building.
 */
test('an owner adds a building and an apartment inside it', async ({ page }) => {
  const building = `Hauptstrasse ${Date.now()}`;
  // Labels are unique per building, but the list shows every building at once.
  const apartment = `2B-${Date.now()}`;

  await page.goto('/');
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
