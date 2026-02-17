import { Component, inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { CircleX, LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { CustomAuthService } from '../../core/auth/custom-auth-service';

@Component({
  selector: 'app-forbidden',
  imports: [LucideAngularModule],
  templateUrl: './forbidden.html',
  styleUrl: './forbidden.css',
})
export class Forbidden {
  readonly shieldIcon = ShieldCheck;
  readonly xIcon = CircleX;

  readonly #authService = inject(CustomAuthService);

  tryAgain() {
    this.#authService.logout();
  }
}
