import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { SplashScreen } from '../../components/splash-screen/splash-screen';
import { AuthService } from '@auth0/auth0-angular';
import { CustomAuthService } from '../../core/auth/custom-auth-service';
import { Router } from '@angular/router';
import { filter, switchMap, take } from 'rxjs';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule, SplashScreen],
  styleUrl: './callback.css',
  template: `<app-splash-screen></app-splash-screen>`,
})
export class Callback implements OnInit {
  private auth0 = inject(AuthService);
  private customAuth = inject(CustomAuthService);
  private router = inject(Router);
  
  ngOnInit(): void {
    const returningFromAuth0 = sessionStorage.getItem('auth0_redirect_pending') === '1'; 

    if(!returningFromAuth0){
      this.router.navigate(['/login']);
      return;
    }

    sessionStorage.removeItem('auth0_redirect_pending');

    this.auth0.isLoading$
      .pipe(
        filter((isLoading) => !isLoading),
        take(1),
        switchMap(() => this.auth0.idTokenClaims$),
        filter((claims): claims is NonNullable<typeof claims> => !!claims?.__raw),
        take(1),
        switchMap((claims) => this.customAuth.loginWithGoogle(claims.__raw))
      )
      .subscribe({
        next: () => this.router.navigate(['/home']),
        error: (err) => {
          console.error('Login callback error', err);
          this.router.navigate(['/login']);
        },
      });
  }
}