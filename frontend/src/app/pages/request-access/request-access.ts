import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';
import { UserService } from '../../services/user-service';
import { StatusLayout } from '../../components/status-layout/status-layout';

@Component({
  selector: 'app-request-access',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar, StatusLayout],
  templateUrl: './request-access.html',
  styleUrl: './request-access.css',
})
export class RequestAccess implements OnInit {
  readonly Icons = AppIcons;

  readonly #userService = inject(UserService);
  readonly #authService = inject(CustomAuthService);

  requestSent = signal(false);

  ngOnInit(): void {
    this.getRequestSent();
  }

  requestAccess() {
    this.#userService.requestAccess('/request-access').subscribe({
      next: () => {
        this.requestSent.set(true);
      },
      error: (err) => console.error('Fout bij aanvraag', err),
    });
  }

  getRequestSent() {
    this.#userService.getRequestSent('/request-access').subscribe({
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
