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

    const login$ = this.auth0.handleRedirectCallback().pipe(
      switchMap(() => this.auth0.idTokenClaims$),
      filter((claims): claims is NonNullable<typeof claims> => !!claims?.__raw),
      take(1),

      // 1. Log in met Google
      switchMap((claims) => this.customAuth.loginWithGoogle(claims.__raw)),

      // 2. Haal daarna de rol én organisatie tegelijkertijd op en bundel ze in een object
      switchMap(() => {
        return forkJoin({
          hasValidRole: this.userService.hasValidRole(),
          hasOrganization: this.userService.hasOrganization(),
        });
      }),
    );

    const minDelay$ = timer(2500);

    // Gebruik hier ook een object in plaats van een array, dat is veel beter leesbaar
    forkJoin({
      loginData: login$,
      delay: minDelay$,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ loginData }) => {
          sessionStorage.removeItem('auth0_redirect_pending');

          // Nu kun je gewoon loginData.hasValidRole en loginData.hasOrganization gebruiken
          if (!loginData.hasValidRole) {
            this.router.navigate(['/request-access']);
          } else if (!loginData.hasOrganization) {
            this.router.navigate(['/no-organization']);
          } else {
            this.router.navigate(['/home']);
          }
        },
        error: (err) => {
          console.error('Login callback error', err);
          this.router.navigate(['/forbidden']);
        },
      });
  }
}
