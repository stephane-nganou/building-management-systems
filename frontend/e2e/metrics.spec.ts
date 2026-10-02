import { expect, test } from '@playwright/test';

import { DEMO_ADMIN, DEMO_OWNER, openSignIn, signIn } from './support';

test.describe('metrics', () => {
  test('an administrator reads how the service is used, in any period', async ({ page }) => {
    const violations: string[] = [];
    page.on('console', (message) => {
      if (message.text().includes('Content Security Policy')) {
        violations.push(message.text());
      }
    });

    await openSignIn(page);
    await signIn(page, DEMO_ADMIN.username, DEMO_ADMIN.password);
    await page.locator('aside.spine').getByRole('link', { name: 'Metrics' }).click();

    await expect(page).toHaveURL(/\/metrics$/);
    await expect(page.getByRole('heading', { name: 'Metrics', level: 1 })).toBeVisible();
    // Signing in just now made the administrator's own requests, and they count.
    await expect(page.getByText('API requests', { exact: true })).toBeVisible();
    await expect(page.locator('canvas[role="img"]')).toHaveCount(7);
    await expect(page.getByRole('img', { name: 'Sign-ins per day' })).toBeVisible();

    // A chart's numbers are there for anybody who cannot read the picture.
    const signIns = page.locator('.metrics-panel').filter({ hasText: 'Sign-ins per day' });
    await signIns.getByText('Show the numbers').click();
    await expect(signIns.locator('tbody tr')).toHaveCount(30);

    const week = page.waitForRequest((request) =>
      request.url().includes('/api/admin/metrics?days=7'),
    );
    await page.getByRole('button', { name: '7 days' }).click();
    await week;
    await expect(page.getByRole('button', { name: '7 days' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await expect(signIns.locator('tbody tr')).toHaveCount(7);

    expect(violations).toEqual([]);
  });

  test('an owner has no metrics to see', async ({ page }) => {
    await openSignIn(page);
    await signIn(page, DEMO_OWNER.username, DEMO_OWNER.password);
    await expect(page.locator('aside.spine')).toBeVisible();

    await expect(page.locator('aside.spine').getByRole('link', { name: 'Metrics' })).toHaveCount(0);
    await page.goto('/metrics');
    await expect(page).not.toHaveURL(/\/metrics$/);
  });
});
