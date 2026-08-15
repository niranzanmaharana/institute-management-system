import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.accessToken();
  const isAuthApi = req.url.includes('/api/v1/auth/');

  const outbound =
    !token || isAuthApi
      ? req
      : req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        });

  return next(outbound).pipe(
    catchError((err: unknown) => {
      if (
        err instanceof HttpErrorResponse &&
        err.status === 401 &&
        !isAuthApi &&
        auth.isAuthenticated()
      ) {
        auth.clearLocalSession();
        void router.navigate(['/login'], {
          queryParams: { reason: 'session' },
          replaceUrl: true,
        });
      }
      return throwError(() => err);
    }),
  );
};
