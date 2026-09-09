import { test, expect } from '@playwright/test';

test('existing profiles, local Matrix search and evaluation view are usable', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Quem está avaliando?' })).toBeVisible();
  await page.getByRole('button', { name: /Gil Entrar no perfil/ }).click();
  await expect(page.getByRole('heading', { name: /Olá, Gil/ })).toBeVisible();
  await page.getByRole('link', { name: 'Buscar', exact: true }).click();
  await page.getByRole('textbox', { name: 'Busca universal' }).fill('The Matrix');
  const card = page.getByRole('button', { name: /No catálogo The Matrix/ });
  await expect(card).toBeVisible();
  await card.click();
  await expect(page.getByRole('heading', { name: 'The Matrix', exact: true })).toBeVisible();
  await expect(page.getByRole('group', { name: 'Avaliar The Matrix', exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByText('Sua nota, Gil')).toBeVisible();
  await page.screenshot({ path: `test-results/evaluation-${test.info().project.name}.png`, fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});

test('model progress and profile switching work without editing historical ratings', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: /Gil Entrar no perfil/ }).click();
  await page.getByRole('link', { name: 'Modelo', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Modelo de Gil' })).toBeVisible();
  await expect(page.getByText(/Mínimo 8 · Recomendado 25/)).toBeVisible();
  await page.locator('.profile-menu summary').click();
  await page.getByRole('button', { name: 'Trocar perfil' }).click();
  await page.getByRole('button', { name: /Aliny Entrar no perfil/ }).click();
  await expect(page.getByRole('heading', { name: /Modelo de Aliny/ })).toBeVisible();
  await page.getByRole('link', { name: 'Para nós', exact: true }).click();
  await page.getByRole('button', { name: /Gil/ }).click();
  await page.getByRole('button', { name: 'Encontrar algo para nós' }).click();
  await expect(page.getByRole('heading', { name: /A noite de/ })).toBeVisible({ timeout: 60_000 });
});
