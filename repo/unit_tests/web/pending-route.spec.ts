import { routes } from '../../web/src/app/app.routes';

/**
 * Regression test for the pending-account redirect bug.
 *
 * A login with a PENDING account is rejected by the backend with a 403. The
 * login form catches ACCOUNT_PENDING and navigates to /pending — which must
 * therefore be a public route (no authGuard), otherwise the auth guard will
 * loop the user straight back to /login and the pending-state UI will never
 * render.
 */
describe('pending route registration', () => {
  it('exposes /pending at the top level (outside the authenticated shell)', () => {
    const publicPending = routes.find(r => r.path === 'pending');
    expect(publicPending).toBeDefined();
    expect(publicPending?.component).toBeDefined();
    // Top-level pending route must not have canActivate guards.
    expect(publicPending?.canActivate).toBeFalsy();
  });

  it('does not duplicate /pending inside the authenticated shell', () => {
    const shell = routes.find(r => r.path === '' && Array.isArray(r.children));
    const nestedPending = shell?.children?.find(c => c.path === 'pending');
    expect(nestedPending).toBeUndefined();
  });
});
