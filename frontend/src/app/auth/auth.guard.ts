import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CustomAuthService } from './custom-auth-service';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(CustomAuthService);
  const router = inject(Router);

  return authService.checkServerSession().pipe(
    take(1),
    map((isLoggedIn) => {
      if (isLoggedIn) {
        return true;
      } else {
        return router.createUrlTree(['/login'], {
          queryParams: { returnUrl: state.url },
        });
      }
    })
  );
};
