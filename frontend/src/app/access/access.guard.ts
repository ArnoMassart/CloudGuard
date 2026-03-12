import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { TeamleaderService } from '../services/teamleader-service';
import { map, Observable } from 'rxjs';

export const accessGuard: CanActivateFn = (route, state): Observable<boolean | UrlTree> => {
  const tlService = inject(TeamleaderService);
  const router = inject(Router);

  return tlService.verifyCloudGuardAccess().pipe(
    map((hasAccess: boolean) => {
      if (!hasAccess) {
        return router.createUrlTree(['/access-denied']);
      }

      return true;
    })
  );
};
