import { Component, inject, input, Input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-status-error-card',
  imports: [LucideAngularModule, TranslocoPipe],
  templateUrl: './status-error-card.html',
  styleUrl: './status-error-card.css',
})
export class StatusErrorCard {
  readonly Icons = AppIcons;

  Icon = input<LucideIconData>(AppIcons.CircleX);
  TitleKey = input<string>('forbidden.access-denied');
  DescriptionKey = input.required<string>();
  Note = input<string>('forbidden.note');
  SubDescriptionKey = input.required<string>();
  BackToLogin = input<boolean>(true);

  readonly #authService = inject(CustomAuthService);
  readonly #router = inject(Router);

  backToLogin() {
    this.#authService.logout();
  }

  backToHome() {
    this.#router.navigate(['/home']);
  }

  openContactPage() {
    window.open('https://cloudmen.com/pages/contact', '_blank');
  }
}
