import {CustomAuthService} from '../auth/custom-auth-service';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';

export const cloudmenStaffGuard: CanActivateFn = () => {
  const auth = inject(CustomAuthService);
  const router = inject(Router);

  return auth.isInitialized$.pipe(
    filter((init) => init === true),
    take(1),
    map(() => auth.isCloudmenStaff()? true: router.createUrlTree(['/no-page-access']),
  ),
  );
};