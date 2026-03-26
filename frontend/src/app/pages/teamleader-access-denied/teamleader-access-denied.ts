import { Component, inject } from '@angular/core';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageBar } from '../../components/language-bar/language-bar';

@Component({
  selector: 'app-teamleader-access-denied',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './teamleader-access-denied.html',
  styleUrl: './teamleader-access-denied.css',
})
export class TeamleaderAccessDenied {
  readonly Icons = AppIcons;

  readonly #authService = inject(CustomAuthService);

  tryAgain() {
    this.#authService.logout();
  }
}
