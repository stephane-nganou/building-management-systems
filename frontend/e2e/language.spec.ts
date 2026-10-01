import { Page, expect, test } from '@playwright/test';

import { DEMO_OWNER, expectNavLabels, openSignIn, signIn } from './support';

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

const GERMAN = [
  'Übersicht',
  'Gebäude',
  'Wohnungen',
  'Mieter',
  'Ausgaben',
  'Rechnungen',
  'Gewinn und Verlust',
  'Assistenz',
];

/** The EN, FR and DE buttons in the sidebar. */
function switcher(page: Page) {
  return {
    en: page.getByRole('button', { name: 'EN', exact: true }),
    fr: page.getByRole('button', { name: 'FR', exact: true }),
    de: page.getByRole('button', { name: 'DE', exact: true }),
  };
}

test.describe('language', () => {
  test('the whole app switches to French and stays there', async ({ page }) => {
    await openSignIn(page);
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

  test('the whole app switches to German', async ({ page }) => {
    await openSignIn(page);
    await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);

    await switcher(page).de.click();

    await expectNavLabels(page, GERMAN);
    await expect(page.locator('.spine-foot .role')).toHaveText(/eigentümer/i);
    await expect(page.locator('html')).toHaveAttribute('lang', 'de');

    // The overview has a "Gebäude" link of its own; this is the sidebar's.
    await page.locator('aside.spine').getByRole('link', { name: 'Gebäude' }).click();
    await expect(page.getByRole('button', { name: 'Gebäude anlegen' }).first()).toBeVisible();

    await switcher(page).en.click();
    await expectNavLabels(page, ENGLISH);
  });

  /** The sign in page is Keycloak's, so it takes realm, theme and backend together to speak German. */
  test('a German visitor signs in on a German page', async ({ browser }) => {
    const context = await browser.newContext({ locale: 'de-DE' });
    const page = await context.newPage();

    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      'Ihre Gebäude, Mieten und Belege in einem Buch',
    );
    await page.getByRole('button', { name: 'Anmelden', exact: true }).click();

    await expect(page.getByRole('link', { name: 'Hier registrieren' })).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'de');

    await context.close();
  });

  test('a German visitor lands on a German registration page', async ({ browser }) => {
    const context = await browser.newContext({ locale: 'de-DE' });
    const page = await context.newPage();

    await page.goto('/register');

    await expect(page.getByRole('heading', { name: 'Konto erstellen' })).toBeVisible();
    await expect(page.getByLabel('Vorname')).toBeVisible();

    await context.close();
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
