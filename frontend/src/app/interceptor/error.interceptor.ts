import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpStatusCode,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 502/503: upstream or temporary unavailability — let callers show a message / retry instead of leaving the page
      const isUpstreamOrTemp =
        error.status === HttpStatusCode.BadGateway || error.status === HttpStatusCode.ServiceUnavailable;
      // 0 (offline/CORS) or other 5xx (e.g. 500 internal error)
      if (error.status === 0 || (error.status >= 500 && !isUpstreamOrTemp)) {
        router.navigate(['/server-error']);
      }

      // Geef de error door zodat eventuele specifieke componenten nog actie kunnen ondernemen
      return throwError(() => error);
    })
  );
};
