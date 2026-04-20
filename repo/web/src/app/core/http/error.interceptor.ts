import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../stores/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 0) {
        // Network error — offline interceptor already queued it; skip toast
        return throwError(() => err);
      }
      if (err.status === 401 || err.status === 403) {
        // auth interceptor handles redirect; only show toast for 403
        if (err.status === 403) {
          const code = err.error?.code ?? 'FORBIDDEN';
          toast.error(code === 'ACCOUNT_LOCKED' ? 'Account is locked. Contact your administrator.' : 'Access denied.');
        }
        return throwError(() => err);
      }
      if (err.status === 409) {
        const code = err.error?.code;
        if (code === 'IDEMPOTENCY_MISMATCH') {
          toast.warn('Request already submitted with different parameters.');
        } else {
          toast.warn(err.error?.message ?? 'Conflict');
        }
        return throwError(() => err);
      }
      if (err.status >= 500) {
        toast.error('Server error. Please try again later.');
      } else if (err.status >= 400) {
        const message = err.error?.message ?? err.message ?? 'Request failed.';
        toast.warn(message);
      }
      return throwError(() => err);
    })
  );
};
