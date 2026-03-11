import { Component, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-server-error',
  imports: [LucideAngularModule],
  templateUrl: './server-error.html',
  styleUrl: './server-error.css',
})
export class ServerError {
  readonly Icons = AppIcons;

  readonly #authService = inject(CustomAuthService);

  tryAgain() {
    this.#authService.logout();
  }
}
