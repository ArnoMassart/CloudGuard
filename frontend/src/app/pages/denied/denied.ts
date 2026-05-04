import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-denied',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './denied.html',
  styleUrl: './denied.css',
})
export class Denied {
  readonly Icons = AppIcons;

  readonly #authService = inject(CustomAuthService);

  backToLogin() {
    this.#authService.logout();
  }
}
