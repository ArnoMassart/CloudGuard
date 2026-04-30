import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { LanguageBar } from '../../components/language-bar/language-bar';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-request-role',
  imports: [LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './request-role.html',
  styleUrl: './request-role.css',
})
export class RequestRole implements OnInit {
  readonly Icons = AppIcons;

  readonly #userService = inject(UserService);
  readonly #authService = inject(CustomAuthService);

  requestSent = signal(false);

  ngOnInit(): void {
    this.getRequestSent();
  }

  requestRole() {
    this.#userService.requestAccess('/request-role').subscribe({
      next: () => {
        this.requestSent.set(true);
      },
      error: (err) => console.error('Fout bij aanvraag', err),
    });
  }

  getRequestSent() {
    this.#userService.getRequestSent('/request-role').subscribe({
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
