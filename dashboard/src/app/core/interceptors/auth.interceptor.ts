import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth.service';

/**
 * Attaches the Bearer token to API calls and signs the user out on a 401.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  const authReq =
    token && req.url.startsWith('/api')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((err) => {
      if (err?.status === 401 && !req.url.includes('/auth/login')) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};
