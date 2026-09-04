import { Page, expect, test } from '@playwright/test';

import { DEMO_OWNER, expectNavLabels, signIn } from './support';

const ENGLISH = [
  'Overview',
  'Buildings',
  'Apartments',
  'Tenants',
  'Expenses',
  'Invoices',
  'Profit and loss',
  'Assistants',
];

const FRENCH = [
  "Vue d'ensemble",
  'Immeubles',
  'Appartements',
  'Locataires',
  'Dépenses',
  'Factures',
  'Compte de résultat',
  'Assistants',
];

/** The EN and FR buttons in the sidebar. */
function switcher(page: Page) {
  return {
    en: page.getByRole('button', { name: 'EN', exact: true }),
    fr: page.getByRole('button', { name: 'FR', exact: true }),
  };
}

test.describe('language', () => {
  test('the whole app switches to French and stays there', async ({ page }) => {
    await page.goto('/');
    await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);

    await expectNavLabels(page, ENGLISH);

    await switcher(page).fr.click();

    // Navigation, headings and the footer all follow, without a reload.
    await expectNavLabels(page, FRENCH);
    await expect(page.locator('.spine-foot .role')).toHaveText(/propriétaire/i);
    await expect(page.getByRole('button', { name: 'Se déconnecter' })).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'fr');

    // A screen loaded after the switch is French from the start.
    await page.getByRole('link', { name: 'Immeubles' }).click();
    await expect(page.getByRole('heading', { name: 'Immeubles' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Ajouter un immeuble' }).first()).toBeVisible();

    // The choice survives a reload, which is the whole point of remembering it.
    await page.reload();
    await expectNavLabels(page, FRENCH);

    await switcher(page).en.click();
    await expectNavLabels(page, ENGLISH);
  });

  test('a French visitor lands on a French registration page', async ({ browser }) => {
    const context = await browser.newContext({ locale: 'fr-FR' });
    const page = await context.newPage();

    await page.goto('/register');

    await expect(page.getByRole('heading', { name: 'Créer votre compte' })).toBeVisible();
    await expect(page.getByLabel('Prénom')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Créer le compte' })).toBeVisible();

    await context.close();
  });
});
