import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { NavItem } from './nav-item/nav-item';
import { Profile } from '../pages/profile/profile';
import { MatDialog } from '@angular/material/dialog';
import { CookieService } from 'ngx-cookie-service';
import { CustomAuthService } from '../auth/custom-auth-service';
import { Router } from '@angular/router';
import { LogOutDialog } from '../components/log-out-dialog/log-out-dialog';
import { UserService } from '../services/user-service';
import { AppIcons } from '../shared/AppIcons';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LanguageBar } from '../components/language-bar/language-bar';

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule, NavItem, CommonModule, Profile, LanguageBar, TranslocoPipe],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  providers: [CookieService],
})
export class Navbar {
  readonly Icons = AppIcons;

  readonly authService = inject(CustomAuthService);
  readonly userService = inject(UserService);

  readonly currentUser = this.authService.currentUser;
  readonly profilePopupOpen = signal(false);
  readonly profileImageError = signal(false);

  constructor() {
    effect(() => {
      this.currentUser();
      this.profileImageError.set(false);
    });
  }

  readonly NavItemsSecurity = [
    { Icon: this.Icons.Shield, Label: 'dashboard', Route: '/home' },
    { Icon: this.Icons.Users, Label: 'user-groups', Route: '/users-groups' },
    { Icon: this.Icons.Building2, Label: 'organisational-units', Route: '/organizational-units' },
    { Icon: this.Icons.FolderOpen, Label: 'shared-drives', Route: '/shared-drives' },
    { Icon: this.Icons.SmartPhone, Label: 'devices', Route: '/devices' },
    { Icon: this.Icons.Key, Label: 'app-access', Route: '/app-access' },
    { Icon: this.Icons.LayoutGrid, Label: 'app-passwords', Route: '/app-passwords' },
    { Icon: this.Icons.Lock, Label: 'password-settings', Route: '/password-settings' },
    { Icon: this.Icons.Globe, Label: 'domain-dns', Route: '/domain-dns' },
  ];

  readonly NavItemsControl = [
    { Icon: this.Icons.CreditCard, Label: 'licenses', Route: '/licenses' },
    { Icon: this.Icons.Bell, Label: 'notifications-feedback', Route: '/reports-reactions' },
    { Icon: this.Icons.Settings, Label: 'security-preferences', Route: '/security-preferences' },
  ];

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user.firstName && user.lastName) {
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    }
    if (user.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  openProfilePopup(event: MouseEvent) {
    event.stopPropagation();
    this.profilePopupOpen.set(true);
  }

  closeProfilePopup() {
    this.profilePopupOpen.set(false);
  }

  readonly dialog = inject(MatDialog);

  readonly cookieService = inject(CookieService);

  readonly router = inject(Router);

  openLogoutDialog(event: MouseEvent): void {
    event.stopPropagation();
    const ref = this.dialog.open(LogOutDialog, {
      width: '500px',
      panelClass: 'logout-dialog-panel',
      backdropClass: 'logout-dialog-backdrop',
    });

    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.onLogout();
      }
    });
  }

  onLogout() {
    this.authService.logout();
  }
}
