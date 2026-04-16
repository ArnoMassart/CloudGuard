import { Component, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-no-organization',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './no-organization.html',
  styleUrl: './no-organization.css',
})
export class NoOrganization {
  readonly Icons = AppIcons;

  readonly #userService = inject(UserService);
  readonly #authService = inject(CustomAuthService);

  requestSent = signal(false);

  ngOnInit(): void {
    this.getRequestSent();
  }

  requestAccess() {
    this.#userService.requestNoOrganization().subscribe({
      next: () => {
        this.requestSent.set(true);
      },
      error: (err) => console.error('Fout bij aanvraag', err),
    });
  }

  getRequestSent() {
    this.#userService.getRequestNoOrganizationSent().subscribe({
      next: (val) => {
        this.requestSent.set(val);
      },
      error: (err) => console.error('Fout bij ophalen request', err),
    });
  }

  tryAgain() {
    this.#authService.logout();
  }
}
