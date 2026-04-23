import { CanActivateFn, Router } from '@angular/router';
import { CustomAuthService } from '../auth/custom-auth-service';
import { inject } from '@angular/core';
import { Role } from '../models/users/User';
import { filter, map, take } from 'rxjs';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(CustomAuthService);
  const router = inject(Router);

  const requiredRoles = route.data['requiredRoles'] as Role[];
  const allowedEmails = route.data['allowedEmails'] as string[];

  if (
    (!requiredRoles || requiredRoles.length === 0) &&
    (!allowedEmails || allowedEmails.length === 0)
  ) {
    return true;
  }

  return authService.isInitialized$.pipe(
    filter((isInit) => isInit === true),
    take(1),
    map(() => {
      const user = authService.currentUser();

      if (!user) {
        return router.createUrlTree(['/no-access']);
      }

      if (allowedEmails && allowedEmails.length > 0) {
        if (!allowedEmails.includes(user.email)) {
          return router.createUrlTree(['/no-access']); // Kick them out if email doesn't match
        }
      }

      if (!user.roles || user.roles.length === 0) {
        return router.createUrlTree(['/no-access']);
      }

      if (user.roles.includes(Role.SUPER_ADMIN)) {
        return true;
      }

      const hasAccess = requiredRoles.some((role) => user.roles.includes(role));

      if (hasAccess) {
        return true;
      }

      return router.createUrlTree(['/no-page-access']);
    })
  );
};
