import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Functional Route Guard for Angular 19+ Standalone applications.
 * Prevents unauthorized users from accessing protected routes by redirecting
 * them to the login page if they do not have a valid token.
 *
 * @param route The requested route.
 * @param state The current router state.
 * @returns True if the user is authenticated, a UrlTree redirect to '/login' otherwise.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // If the user is logged in, allow navigation to the protected route
  if (authService.isLoggedIn()) {
    return true;
  }

  // Otherwise, redirect to the login page
  // Optionally, you could pass the attempted URL as a query parameter
  // so the login page can redirect them back after successful authentication.
  return router.createUrlTree(['/login']);
};
