import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { AppIcons } from '../../shared/AppIcons';
import { Router } from '@angular/router';
import { StatusLayout } from '../../components/status-layout/status-layout';

@Component({
  selector: 'app-page-access-denied',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar, StatusLayout],
  templateUrl: './page-access-denied.html',
  styleUrl: './page-access-denied.css',
})
export class PageAccessDenied {
  readonly Icons = AppIcons;

  readonly #router = inject(Router);

  backToHome() {
    this.#router.navigate(['/home']);
  }
}
