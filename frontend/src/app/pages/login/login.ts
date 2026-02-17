import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { CustomAuthService } from '../../core/auth/custom-auth-service';
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
    this.auth0.error$.subscribe((err) => {
      console.log('Auth0 Error detected:', err.message);

      this.#router.navigate(['/forbidden']);
    });

    this.auth0.isLoading$
      .pipe(
        filter((isLoading) => !isLoading),
        take(1),
        switchMap(() => this.auth0.idTokenClaims$)
      )
      .subscribe({
        next: (claims) => {
          // Hebben we een token? Dan komen we net terug van Google!
          if (claims && claims.__raw) {
            console.log('Redirect succesvol! Token gevonden.');
            this.#handleBackendExchange(claims.__raw);
          }
        },
        error: (err) => console.error('Fout bij ophalen claims:', err),
      });

    this.auth0.idTokenClaims$.subscribe((claims) => {
      if (claims && claims.__raw) {
        this.#handleBackendExchange(claims.__raw);
      }
    });
  }

  loginWithGoogle() {
    sessionStorage.setItem('auth0_redirect_pending', '1');
    this.auth0.loginWithRedirect({
      authorizationParams: {
        connection: 'google-oauth2',
        prompt: 'select_account',
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
