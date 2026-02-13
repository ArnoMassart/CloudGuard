import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth-service';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isLoggedIn$.pipe(
    // filter(val => val !== null), // Alleen nodig als je een BehaviorSubject met null gebruikt
    take(1), // Zodra er een waarde is (true/false), stop met luisteren
    map((isLoggedIn) => {
      if (isLoggedIn) {
        return true;
      } else {
        return router.parseUrl('/login');
      }
    }),
  );
};
