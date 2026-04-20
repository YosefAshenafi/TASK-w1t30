import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthStore } from '../stores/auth.store';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

const ACCESS_TOKEN_KEY = 'meridian_access_token';
const REFRESH_TOKEN_KEY = 'meridian_refresh_token';

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function storeTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

function addAuthHeader(req: HttpRequest<unknown>): HttpRequest<unknown> {
  const token = getAccessToken();
  if (!token) return req;
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const http = inject(HttpClient);

  // Skip auth header for auth endpoints to avoid circular issues
  if (req.url.includes('/auth/refresh') || req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
    return next(req);
  }

  return next(addAuthHeader(req)).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.includes('/auth/')) {
        const refreshToken = getRefreshToken();
        if (!refreshToken) {
          authStore.clearProfile();
          clearTokens();
          router.navigate(['/login']);
          return throwError(() => err);
        }
        return http.post<{ accessToken: string; refreshToken: string }>(
          '/api/v1/auth/refresh',
          { refreshToken }
        ).pipe(
          switchMap(tokens => {
            storeTokens(tokens.accessToken, tokens.refreshToken);
            return next(addAuthHeader(req));
          }),
          catchError(refreshErr => {
            authStore.clearProfile();
            clearTokens();
            router.navigate(['/login']);
            return throwError(() => refreshErr);
          })
        );
      }
      return throwError(() => err);
    })
  );
};
