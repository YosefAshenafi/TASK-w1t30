import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../stores/toast.service';

interface ErrorBody { code?: string; message?: string; details?: unknown; }

function extractBody(err: HttpErrorResponse): ErrorBody {
  // Backend wraps errors as { error: { code, message, details } } (see ErrorEnvelope.java).
  // Fall back to the top-level shape for older responses.
  const envelope = err.error?.error;
  if (envelope && typeof envelope === 'object') return envelope as ErrorBody;
  if (err.error && typeof err.error === 'object') return err.error as ErrorBody;
  return {};
}

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 0) {
        return throwError(() => err);
      }
      const body = extractBody(err);
      if (err.status === 401 || err.status === 403) {
        if (err.status === 403) {
          const code = body.code ?? 'FORBIDDEN';
          toast.error(code === 'ACCOUNT_LOCKED' ? 'Account is locked. Contact your administrator.' : 'Access denied.');
        }
        return throwError(() => err);
      }
      if (err.status === 409) {
        if (body.code === 'IDEMPOTENCY_MISMATCH') {
          toast.warn('Request already submitted with different parameters.');
        } else {
          toast.warn(body.message ?? 'Conflict');
        }
        return throwError(() => err);
      }
      if (err.status >= 500) {
        toast.error('Server error. Please try again later.');
      } else if (err.status >= 400) {
        toast.warn(body.message ?? err.message ?? 'Request failed.');
      }
      return throwError(() => err);
    })
  );
};
