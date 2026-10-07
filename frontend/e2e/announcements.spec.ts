import { Page, expect, test } from '@playwright/test';

import { DEMO_ADMIN, DEMO_OWNER, openSignIn, registerOwner, signIn, signOut } from './support';

function banner(page: Page, text: string) {
  return page.locator('.notice.announcement').filter({ hasText: text });
}

async function openAnnouncementsAsAdmin(page: Page): Promise<void> {
  await openSignIn(page);
  await signIn(page, DEMO_ADMIN.username, DEMO_ADMIN.password);
  await page.locator('aside.spine').getByRole('link', { name: 'Announcements' }).click();
  await expect(page.getByRole('heading', { name: 'Announcements', level: 1 })).toBeVisible();
}

test.describe('announcements', () => {
  /**
   * The administrator tells everybody something; an owner signed in sees it
   * above every screen, in their own language when there is a translation, and
   * can put it away until it next changes.
   */
  test('an announcement reaches an owner, who can dismiss it', async ({ page }) => {
    const message = `Down for maintenance ${Date.now()}`;

    await openAnnouncementsAsAdmin(page);
    await page.getByRole('button', { name: 'New announcement' }).click();
    const dialog = page.getByRole('dialog', { name: 'New announcement' });
    await dialog.getByLabel('Kind').selectOption('WARNING');
    await dialog.getByLabel('Message in English').fill(message);
    await dialog.getByLabel('Message in French').fill(`Maintenance ${message}`);
    await dialog.getByRole('button', { name: 'Publish' }).click();

    const row = page.getByRole('row', { name: new RegExp(message) });
    await expect(row).toContainText('Showing');
    await expect(row).toContainText('Warning');
    // The administrator is signed in too, and sees it at once.
    await expect(banner(page, message)).toBeVisible();
    await signOut(page);

    await registerOwner(page, 'announced');
    await expect(banner(page, message)).toBeVisible();
    await expect(banner(page, message)).toHaveClass(/warning/);

    // The French translation follows the language switch, without asking again.
    await page.getByRole('button', { name: 'FR', exact: true }).click();
    await expect(banner(page, `Maintenance ${message}`)).toBeVisible();
    await page.getByRole('button', { name: 'EN', exact: true }).click();

    await banner(page, message).getByRole('button', { name: 'Dismiss' }).click();
    await expect(banner(page, message)).toHaveCount(0);
    await page.reload();
    await expect(page.locator('aside.spine')).toBeVisible();
    await expect(banner(page, message)).toHaveCount(0);
    await signOut(page);

    // Deleted, it is gone from the list and from every screen.
    await openAnnouncementsAsAdmin(page);
    await row.getByRole('button', { name: 'Delete' }).click();
    await page
      .getByRole('dialog', { name: 'Delete this announcement?' })
      .getByRole('button', { name: 'Delete' })
      .click();
    await expect(row).toHaveCount(0);
    await expect(banner(page, message)).toHaveCount(0);
  });

  test('an owner has no announcements to manage', async ({ page }) => {
    await openSignIn(page);
    await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);
    await expect(page.locator('aside.spine')).toBeVisible();

    await expect(
      page.locator('aside.spine').getByRole('link', { name: 'Announcements' }),
    ).toHaveCount(0);
    await page.goto('/announcements');
    await expect(page).not.toHaveURL(/\/announcements$/);
  });
});
