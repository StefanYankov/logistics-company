import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Functional HTTP Interceptor for Angular 19+ Standalone applications.
 * Automatically attaches the JWT Authorization header to outgoing requests
 * if a token exists in local storage.
 *
 * @param req The outgoing HTTP request.
 * @param next The next handler in the interceptor chain.
 * @returns An Observable of the HTTP response.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Inject the AuthService using the modern inject() function
  const authService = inject(AuthService);
  const token = authService.getToken();

  // If a token exists, clone the request and append the Authorization header
  if (token) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    // Pass the modified request to the next handler
    return next(authReq);
  }

  // If no token exists, pass the original request unmodified
  return next(req);
};
