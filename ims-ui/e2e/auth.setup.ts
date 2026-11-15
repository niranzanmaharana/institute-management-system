import { expect, test as setup } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';

const authFile = path.join(__dirname, '.auth', 'admin.json');
const apiBase = process.env['IMS_API_BASE'] || 'http://localhost:8088';

setup('authenticate', async ({ page, request }) => {
  const username = process.env['IMS_USER'] || 'admin-a';
  const password = process.env['IMS_PASSWORD'] || 'Password@123';
  const institute = process.env['IMS_INSTITUTE'] || 'DEMO_A';

  const login = await request.post(`${apiBase}/api/v1/auth/login`, {
    data: { username, password, instituteCode: institute },
  });
  expect(login.ok(), `login HTTP ${login.status()}: ${await login.text()}`).toBeTruthy();
  const tokens = (await login.json()) as {
    accessToken: string;
    refreshToken: string;
    user: unknown;
  };

  fs.mkdirSync(path.dirname(authFile), { recursive: true });

  await page.goto('/login');
  await page.evaluate(
    ({ accessToken, refreshToken, user }) => {
      localStorage.setItem('ims.accessToken', accessToken);
      localStorage.setItem('ims.refreshToken', refreshToken);
      localStorage.setItem('ims.user', JSON.stringify(user));
    },
    tokens,
  );
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/dashboard/, { timeout: 15_000 });
  await page.context().storageState({ path: authFile });
});
