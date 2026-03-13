import { Component, effect, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './profile.html',
})
export class Profile {
  readonly Icons = AppIcons;
  readonly authService = inject(CustomAuthService);
  readonly currentUser = this.authService.currentUser;
  readonly userService = inject(UserService);
  readonly profileImageError = signal(false);
  closed$ = output<void>();

  constructor() {
    effect(() => {
      this.currentUser();
      this.profileImageError.set(false);
    });
  }

  close() {
    this.closed$.emit();
  }

  logout() {
    this.authService.logout();
  }
}
