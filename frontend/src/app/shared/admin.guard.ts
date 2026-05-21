import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from './auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.hasRole('ROLE_ADMIN')) {
    return true;
  }

  // Redirect to the default authenticated page if not an admin
  return router.parseUrl('/app');
};
