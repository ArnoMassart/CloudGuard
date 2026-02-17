import { Component, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CustomAuthService } from '../../core/auth/custom-auth-service';

@Component({
  selector: 'app-profile',
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  readonly authService = inject(CustomAuthService);
  readonly currentUser = this.authService.currentUser;
  closed$ = output<void>();

  close() {
    this.closed$.emit();
  }

  logout() {
    this.authService.logout();
  }

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user?.firstName && user?.lastName)
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    if (user?.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user?.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }
}
