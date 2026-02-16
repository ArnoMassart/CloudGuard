import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { LucideAngularModule, ShieldCheck } from 'lucide-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private auth = inject(AuthService);
  readonly ShieldCheck = ShieldCheck;

  loginWithGoogle() {
    this.auth
      .loginWithRedirect({
        authorizationParams: { connection: 'google-oauth2' },
      })
      .subscribe();
  }

}
