import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CustomAuthService } from './custom-auth-service';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(CustomAuthService);
  const router = inject(Router);

  return authService.isLoggedIn$.pipe(
    take(1), // Zodra er een waarde is (true/false), stop met luisteren
    map((isLoggedIn) => {
      if (isLoggedIn) {
        return true;
      } else {
        return router.createUrlTree(['/login'], {
          queryParams: { returnUrl: state.url },
        });
      }
    }),
  );
};
