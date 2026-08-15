import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree([auth.homePath()]);
};

/** Platform operators — institute lifecycle only. */
export const platformAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isPlatformAdmin()) {
    return true;
  }
  return router.createUrlTree([auth.homePath()]);
};

/** Institute operators — block platform admin from tenant business screens. */
export const instituteOperatorGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated() && !auth.isPlatformAdmin()) {
    return true;
  }
  return router.createUrlTree([auth.homePath()]);
};
