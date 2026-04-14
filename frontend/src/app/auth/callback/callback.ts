import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SplashScreen } from '../../components/splash-screen/splash-screen';
import { AuthService } from '@auth0/auth0-angular';
import { CustomAuthService } from '../custom-auth-service';
import { Router } from '@angular/router';
import { switchMap, filter, take, forkJoin, timer } from 'rxjs';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule, SplashScreen],
  styleUrl: './callback.css',
  template: `<app-splash-screen></app-splash-screen>`,
})
export class Callback implements OnInit {
  private readonly auth0 = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly customAuth = inject(CustomAuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const returningFromAuth0 = sessionStorage.getItem('auth0_redirect_pending') === '1';

    if (!returningFromAuth0) {
      this.router.navigate(['/login']);
      return;
    }

    // Manually handle the Auth0 redirect (skipRedirectCallback is true on /callback).
    // This exchanges the authorization code for tokens without the SDK navigating away.
    const login$ = this.auth0.handleRedirectCallback().pipe(
      switchMap(() => this.auth0.idTokenClaims$),
      filter((claims): claims is NonNullable<typeof claims> => !!claims?.__raw),
      take(1),
      switchMap((claims) => this.customAuth.loginWithGoogle(claims.__raw)),
      switchMap(() => this.userService.hasValidRole())
    );

    const minDelay$ = timer(2500);

    forkJoin([login$, minDelay$])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ([hasValidRole, _]) => {
          sessionStorage.removeItem('auth0_redirect_pending');

          if (hasValidRole) {
            this.router.navigate(['/home']);
          } else {
            this.router.navigate(['/request-access']);
          }
        },
        error: (err) => {
          console.error('Login callback error', err);
          this.router.navigate(['/forbidden']);
        },
      });
  }
}
