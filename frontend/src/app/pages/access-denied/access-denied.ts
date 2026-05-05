import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { AppIcons } from '../../shared/AppIcons';
import { Router } from '@angular/router';
import { StatusLayout } from '../../components/status-layout/status-layout';

@Component({
  selector: 'app-access-denied',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar, StatusLayout],
  templateUrl: './access-denied.html',
  styleUrl: './access-denied.css',
})
export class AccessDenied {
  readonly Icons = AppIcons;

  readonly #authService = inject(CustomAuthService);

  backToLogin() {
    this.#authService.logout();
  }
}
