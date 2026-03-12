import { Component, inject, signal } from '@angular/core';
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

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule, NavItem, CommonModule, Profile],
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

  readonly NavItemsSecurity = [
    { Icon: this.Icons.Shield, Label: 'Dashboard', Route: '/home' },
    { Icon: this.Icons.Users, Label: 'Gebruikers & Groepen', Route: '/users-groups' },
    { Icon: this.Icons.Building2, Label: 'Organisatie-eenheden', Route: '/organizational-units' },
    { Icon: this.Icons.FolderOpen, Label: 'Gedeelde Drives', Route: '/shared-drives' },
    { Icon: this.Icons.SmartPhone, Label: 'Mobiele Apparaten', Route: '/mobile-devices' },
    { Icon: this.Icons.Key, Label: 'App Toegang', Route: '/app-access' },
    { Icon: this.Icons.Lock, Label: 'App-wachtwoorden', Route: '/app-passwords' },
    { Icon: this.Icons.Globe, Label: 'Domein & DNS', Route: '/domain-dns' },
  ];

  readonly NavItemsControl = [
    { Icon: this.Icons.CreditCard, Label: 'Licenties', Route: '/licenses' },
    { Icon: this.Icons.Bell, Label: 'Meldingen & Feedback', Route: '/reports-reactions' },
  ];

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user.firstName && user.lastName) {
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    }
    if (user.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  openProfilePopup() {
    this.profilePopupOpen.set(true);
  }

  closeProfilePopup() {
    this.profilePopupOpen.set(false);
  }

  readonly dialog = inject(MatDialog);

  readonly cookieService = inject(CookieService);

  readonly router = inject(Router);

  openLogoutDialog(): void {
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
