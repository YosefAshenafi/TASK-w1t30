import { test, expect } from './coverage.fixture';
import { API, apiLogin, loginViaUI } from './helpers';

/**
 * Flow 7: Recycle bin restore round-trip — admin soft-deletes a course,
 * confirms it appears in the recycle bin UI, and restores it.
 *
 * The recycle bin supports courses and users. We exercise the course path
 * here; a separate test could exercise the user soft-delete path the same way.
 */
test.describe('Recycle bin restore', () => {
  const adminUser = 'admin';
  const adminPassword = 'Admin@123!';
  let adminToken: string;
  let courseId: string;
  let courseTitle: string;

  test.beforeAll(async ({ request }) => {
    const admin = await apiLogin(request, adminUser, adminPassword);
    adminToken = admin.accessToken;
  });

  test('7a: admin creates a course via API', async ({ request }) => {
    courseTitle = `E2E Recycled Course ${Date.now()}`;
    const res = await request.post(`${API}/api/v1/courses`, {
      headers: { Authorization: `Bearer ${adminToken}` },
      data: {
        code: `E2E-${Date.now()}`,
        title: courseTitle,
        version: '2025.1',
        classification: 'INTERNAL',
      },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    courseId = body.id;
    expect(courseId).toBeTruthy();
  });

  test('7b: admin soft-deletes the course via API', async ({ request }) => {
    const res = await request.delete(`${API}/api/v1/courses/${courseId}`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect([204, 200]).toContain(res.status());
  });

  test('7c: admin sees course in /admin/recycle-bin', async ({ page }) => {
    await loginViaUI(page, adminUser, adminPassword);
    await page.goto('/admin/recycle-bin');

    // Default tab is courses; wait for the entry list to render and
    // assert the soft-deleted title appears.
    await expect(page.getByText(courseTitle)).toBeVisible({ timeout: 10_000 });
  });

  test('7d: admin restores course via API', async ({ request }) => {
    const res = await request.post(
      `${API}/api/v1/admin/recycle-bin/courses/${courseId}/restore`,
      { headers: { Authorization: `Bearer ${adminToken}` }, data: {} },
    );
    expect([204, 200]).toContain(res.status());
  });

  test('7e: restored course is returned by the list API', async ({ request }) => {
    const res = await request.get(`${API}/api/v1/courses?size=200`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    const found = (body.content ?? []).some((c: { id: string }) => c.id === courseId);
    expect(found).toBe(true);
  });
});
