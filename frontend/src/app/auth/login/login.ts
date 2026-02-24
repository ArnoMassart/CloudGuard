import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule } from 'lucide-angular';
import { Router } from '@angular/router';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  readonly Icons = AppIcons;

  private auth0 = inject(AuthService);
  readonly #router = inject(Router);

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
