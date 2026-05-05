import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { Router } from '@angular/router';
import { BrandFooter } from '../../components/brand-footer/brand-footer';
import { BrandHeader } from '../../components/brand-header/brand-header';
import { StatusLayout } from '../../components/status-layout/status-layout';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
    TranslocoPipe,
    LanguageBar,
    BrandFooter,
    BrandHeader,
    StatusLayout,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  readonly Icons = AppIcons;

  private readonly auth0 = inject(AuthService);
  private readonly router = inject(Router);

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

  goToContact() {
    this.router.navigate(['/contact']);
  }
}
