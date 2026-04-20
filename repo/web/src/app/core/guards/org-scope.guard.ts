import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../stores/auth.store';

export const orgScopeGuard: CanActivateFn = (_route, _state) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const role = authStore.userRole();

  if (role === 'CORPORATE_MENTOR' && authStore.organizationId()) {
    return true;
  }
  return router.createUrlTree(['/home']);
};
