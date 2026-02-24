import { Component, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-profile',
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  readonly authService = inject(CustomAuthService);
  readonly currentUser = this.authService.currentUser;
  readonly userService = inject(UserService);
  closed$ = output<void>();

  close() {
    this.closed$.emit();
  }

  logout() {
    this.authService.logout();
  }
}
