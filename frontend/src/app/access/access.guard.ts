import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AccessService } from './access-service';
import { map, Observable } from 'rxjs';
import { CustomAuthService } from '../auth/custom-auth-service';

export const accessGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const tlService = inject(AccessService);
  const authService = inject(CustomAuthService);
  const router = inject(Router);

  return tlService.verifyCloudGuardAccess().pipe(
    map((hasAccess: boolean) => {
      const user = authService.currentUser();

      if (!user) {
        return router.createUrlTree(['/no-access']);
      }

      if (!user.roles || user.roles.length === 0) {
        return router.createUrlTree(['/no-access']);
      }

      if (!hasAccess) {
        return router.createUrlTree(['/access-denied']);
      }

      return true;
    })
  );
};
