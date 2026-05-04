import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { LanguageBar } from '../../components/language-bar/language-bar';

@Component({
  selector: 'app-inactive',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './inactive.html',
  styleUrl: './inactive.css',
})
export class Inactive {
  readonly Icons = AppIcons;

  readonly #authService = inject(CustomAuthService);

  backToLogin() {
    this.#authService.logout();
  }
}
