import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageBar } from '../../components/language-bar/language-bar';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  readonly Icons = AppIcons;

  private readonly auth0 = inject(AuthService);

  loginWithGoogle() {
    sessionStorage.setItem('auth0_redirect_pending', '1');
    this.auth0.loginWithRedirect({
      appState: { target: '/callback' },
      authorizationParams: {
        connection: 'google-oauth2',
        prompt: 'select_account',
        redirect_uri: globalThis.location.origin + '/callback',
      },
    });
  }
}
