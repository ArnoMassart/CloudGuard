import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { CircleX, LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/app-icons';

@Component({
  selector: 'app-forbidden',
  imports: [LucideAngularModule],
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
