import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AccessService } from './access-service';
import { map, Observable } from 'rxjs';
import { CustomAuthService } from '../auth/custom-auth-service';
import { Role } from '../models/users/User';
import { combineLatest, filter, take } from 'rxjs';
import { toObservable } from '@angular/core/rxjs-interop';

export const accessGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const tlService = inject(AccessService);
  const authService = inject(CustomAuthService);
  const router = inject(Router);

  return combineLatest([
    toObservable(authService.currentUser),
    tlService.verifyCloudGuardAccess(),
  ]).pipe(
    filter(([user]) => user != null),
    take(1),
    map(([user, hasAccess]) => {
      if (!user!.roles || user!.roles.length === 0) {
        return router.createUrlTree(['/no-access']);
      }
      if (!hasAccess) {
        return router.createUrlTree(['/access-denied']);
      }
      return true;
    })
  );
  /*
  return tlService.verifyCloudGuardAccess().pipe(
    map((hasAccess: boolean) => {
      const user = authService.currentUser();

      if (!user) {
        return router.createUrlTree(['/no-access']);
      }

      if (user.accessDenied) {
        return router.createUrlTree(['/denied']);
      }

      if (!user.accessAccepted) {
        return router.createUrlTree(['/request-access']);
      }

      if (!user.organizationId || user.organizationId === 0) {
        return router.createUrlTree(['/no-organization']);
      }

      if (!user.roles || user.roles.length === 0) {
        return router.createUrlTree(['/no-access']);
      }

      if (user.roles.includes(Role.UNASSIGNED)) {
        return router.createUrlTree(['/request-role']);
      }

      if (!hasAccess) {
        return router.createUrlTree(['/access-denied']);
      }

      return true;
    })
  );
  */
};
