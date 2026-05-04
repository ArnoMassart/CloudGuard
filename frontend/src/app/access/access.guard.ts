import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AccessService } from './access-service';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, switchMap, take } from 'rxjs/operators';
import { CustomAuthService } from '../auth/custom-auth-service';
import { Role } from '../models/users/User';
import { toObservable } from '@angular/core/rxjs-interop';

export const accessGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const tlService = inject(AccessService);
  const authService = inject(CustomAuthService);
  const router = inject(Router);

  return toObservable(authService.currentUser).pipe(
    filter((user) => user != null),
    take(1),

    switchMap((user) => {
      if (user.accessDenied) return of(router.createUrlTree(['/denied']));
      if (!user.isActive) return of(router.createUrlTree(['/inactive']));
      if (!user.accessAccepted) return of(router.createUrlTree(['/request-access']));

      if (!user.organizationId || user.organizationId === 0) {
        return of(router.createUrlTree(['/no-organization']));
      }

      if (!user.roles || user.roles.length === 0) return of(router.createUrlTree(['/no-access']));
      if (user.roles.includes(Role.UNASSIGNED)) return of(router.createUrlTree(['/request-role']));

      return tlService.verifyCloudGuardAccess().pipe(
        map((hasAccess) => {
          if (!hasAccess) {
            return router.createUrlTree(['/access-denied']);
          }
          return true;
        }),
        catchError((error) => {
          console.error('Fout bij verifiëren CloudGuard access:', error);
          return of(router.createUrlTree(['/access-denied']));
        })
      );
    })
  );
};
