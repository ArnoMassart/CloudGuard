import { Component, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  readonly Icons = AppIcons;
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
