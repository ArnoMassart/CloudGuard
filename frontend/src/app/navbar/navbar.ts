import { Component, inject } from '@angular/core';
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
import { MatDialog } from '@angular/material/dialog';
import { LogOutDialog } from '../log-out-dialog/log-out-dialog';
import { CookieService } from 'ngx-cookie-service';
import { AuthService } from '../core/auth/auth-service';
import { Router } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule, NavItem],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
  providers: [CookieService],
})
export class Navbar {
  readonly Shield = Shield;
  readonly LogOut = LogOut;

  readonly NavItemsSecurity = [
    { Icon: Shield, Label: 'Dashboard', Route: '/' },
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

  readonly dialog = inject(MatDialog);

  readonly cookieService = inject(CookieService);

  readonly authService = inject(AuthService);

  readonly router = inject(Router);

  openLogoutDialog(): void {
    // create session token for example
    localStorage.setItem('Userdata', 'TEst');
    sessionStorage.setItem('Sessiondata', 'TEST');

    const ref = this.dialog.open(LogOutDialog, {
      width: '500px',
    });

    ref.afterClosed().subscribe((result) => {
      if (result == true) {
        this.onLogout();
      }
    });
  }

  onLogout() {
    this.authService.logout().subscribe({
      error: (err) => console.error('Er ging iets mis bij het uitloggen', err),
    });
  }
}
