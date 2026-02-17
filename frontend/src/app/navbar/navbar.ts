import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  Shield,
  Users,
  FolderOpen,
  Building2,
  Smartphone,
  Key,
  Lock,
  Globe,
  CreditCard,
  Bell,
  LogOut,
} from 'lucide-angular';
import { NavItem } from './nav-item/nav-item';
import { Profile } from '../pages/profile/profile';
import { RouterLinkActive, RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { LogOutDialog } from '../log-out-dialog/log-out-dialog';
import { CookieService } from 'ngx-cookie-service';
import { CustomAuthService } from '../core/auth/custom-auth-service';
import { Router } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule, NavItem, CommonModule, Profile],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  providers: [CookieService],
})
export class Navbar {
  readonly authService = inject(CustomAuthService);

  readonly currentUser = this.authService.currentUser;
  readonly profilePopupOpen = signal(false);

  readonly Shield = Shield;
  readonly LogOut = LogOut;

  readonly NavItemsSecurity = [
    { Icon: Shield, Label: 'Dashboard', Route: '/home' },
    { Icon: Users, Label: 'Gebruikers & Groepen', Route: '/users-groups' },
    { Icon: Building2, Label: 'Organisatie-eenheden', Route: '/organizational-units' },
    { Icon: FolderOpen, Label: 'Gedeelde Drives', Route: '/shared-drives' },
    { Icon: Smartphone, Label: 'Mobiele Apparaten', Route: '/mobile-devices' },
    { Icon: Key, Label: 'App Toegang', Route: '/app-access' },
    { Icon: Lock, Label: 'App-wachtwoorden', Route: '/app-passwords' },
    { Icon: Globe, Label: 'Domein & DNS', Route: '/domain-dns' },
  ];

  readonly NavItemsControl = [
    { Icon: CreditCard, Label: 'Licenties & Billing', Route: '/licenses-billing' },
    { Icon: Bell, Label: 'Meldingen & Feedback', Route: '/reports-reactions' },
  ];

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user.firstName && user.lastName) {
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    }
    if (user.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  openProfilePopup(){
    this.profilePopupOpen.set(true);
  }

  closeProfilePopup(){
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
      if (result == true) {
        this.onLogout();
      }
    });
  }

  onLogout() {
    this.authService.logout();
  }
}
