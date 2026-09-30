import { expect, test } from '@playwright/test';

import { DEMO_OWNER, signIn } from './support';

test.describe('theme', () => {
  test('the app takes on a theme and keeps it', async ({ page }) => {
    await page.goto('/');
    await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);

    const html = page.locator('html');
    await expect(html).toHaveAttribute('data-theme', 'classic');

    await page.getByRole('button', { name: 'Magic', exact: true }).click();
    await expect(html).toHaveAttribute('data-theme', 'magic');
    await expect(page.getByRole('button', { name: 'Magic', exact: true })).toHaveAttribute(
      'aria-pressed',
      'true',
    );

    // The page is repainted, not only relabelled: the ground is Magic's plum.
    await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(27, 18, 51)');

    await page.reload();
    await expect(html).toHaveAttribute('data-theme', 'magic');

    await page.getByRole('button', { name: 'Classic', exact: true }).click();
    await expect(html).toHaveAttribute('data-theme', 'classic');
  });

  test('a visitor can choose one before signing up', async ({ page }) => {
    await page.goto('/register');

    await page.getByRole('button', { name: 'Ocean blue', exact: true }).click();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'ocean');
    await expect(page.locator('body')).toHaveCSS('background-color', 'rgb(234, 244, 243)');

    await page.reload();
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'ocean');
  });
});
