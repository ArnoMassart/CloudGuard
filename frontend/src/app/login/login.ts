import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { CustomAuthService } from '../core/auth/custom-auth-service';
import { Router } from '@angular/router';
import { filter, switchMap, take } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  private auth0 = inject(AuthService);
  readonly #authService = inject(CustomAuthService);
  readonly #router = inject(Router);

  isProcessing = signal(false);

  readonly ShieldCheck = ShieldCheck;

  ngOnInit(): void {
    // Alleen token uitwisselen als we terugkomen van Auth0 redirect.
    // Zonder deze check zouden gecachte Auth0-tokens direct doorsturen naar home.
    const returningFromAuth0 =
      sessionStorage.getItem('auth0_redirect_pending') === '1';

    if (returningFromAuth0) {
      sessionStorage.removeItem('auth0_redirect_pending');

      this.auth0.isLoading$
        .pipe(
          filter((isLoading) => !isLoading),
          take(1),
          switchMap(() => this.auth0.idTokenClaims$),
          filter((claims): claims is NonNullable<typeof claims> => !!claims?.__raw),
          take(1)
        )
        .subscribe({
          next: (claims) => {
            console.log('Redirect succesvol! Token gevonden.');
            this.#handleBackendExchange(claims.__raw);
          },
          error: (err) => console.error('Fout bij ophalen claims:', err),
        });
    }
  }

  loginWithGoogle() {
    sessionStorage.setItem('auth0_redirect_pending', '1');
    this.auth0.loginWithRedirect({
      authorizationParams: {
        connection: 'google-oauth2',
        redirect_uri: window.location.origin + '/login',
      },
    });
  }

  #handleBackendExchange(idToken: string) {
    if (this.isProcessing()) return;
    this.isProcessing.set(true);

    this.#authService.loginWithGoogle(idToken).subscribe({
      next: () => {
        this.#router.navigate(['/home']);
      },
      error: (err) => {
        console.error('Backend error status:', err.status);
        console.error('Backend error message:', JSON.stringify(err.error, null, 2));
        this.isProcessing.set(false);
      },
    });
  }
}
