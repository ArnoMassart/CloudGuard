import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // 0 (Server down/Cors fout) of 5xx (Interne Server Crash)
      if (error.status === 0 || error.status >= 500) {
        router.navigate(['/server-error']);
      }

      // Geef de error door zodat eventuele specifieke componenten nog actie kunnen ondernemen
      return throwError(() => error);
    })
  );
};
