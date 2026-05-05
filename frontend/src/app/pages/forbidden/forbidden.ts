import { Component, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { StatusLayout } from '../../components/status-layout/status-layout';

@Component({
  selector: 'app-forbidden',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar, StatusLayout],
  templateUrl: './forbidden.html',
  styleUrl: './forbidden.css',
})
export class Forbidden {
  readonly Icons = AppIcons;

  readonly #authService = inject(CustomAuthService);

  tryAgain() {
    this.#authService.logout();
  }
}
