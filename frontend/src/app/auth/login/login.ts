import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private auth0 = inject(AuthService);
  readonly #router = inject(Router);

  readonly ShieldCheck = ShieldCheck;

  loginWithGoogle() {
    sessionStorage.setItem('auth0_redirect_pending', '1');
    this.auth0.loginWithRedirect({
      appState: { target: '/callback' },
      authorizationParams: {
        connection: 'google-oauth2',
        prompt: 'select_account',
        redirect_uri: window.location.origin + '/callback',
      },
    });
  }
}