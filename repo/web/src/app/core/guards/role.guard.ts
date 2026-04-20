import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from '../stores/auth.store';
import { Role } from '../models/user.model';

export const roleGuard: CanActivateFn = (route, _state) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const requiredRoles: Role[] = route.data['roles'] ?? [];

  if (requiredRoles.length === 0) {
    return true;
  }
  const userRole = authStore.userRole();
  if (userRole && requiredRoles.includes(userRole)) {
    return true;
  }
  return router.createUrlTree(['/home']);
};
