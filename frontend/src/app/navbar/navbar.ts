import { Component, inject, signal } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
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

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule, NavItem, RouterLinkActive, RouterLink, CommonModule, Profile],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  private auth = inject(AuthService);

  readonly  user$ = this.auth.user$;
  readonly isLoading$ = this.auth.isLoading$;
  readonly profilePopupOpen = signal(false);
  
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

  getInitials(user: {name?: string; given_name?: string; family_name?: string; email?: string;}) {
    if (user.given_name && user.family_name) {
      return (user.given_name[0] + user.family_name[0]).toUpperCase();
    }
    if (user.name) {
      const parts = user.name.trim().split(/\s+/);
      if (parts.length >= 2)
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
      return user.name.slice(0, 2).toUpperCase();
    }
    if (user.email)
      return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  openProfilePopup(){
    this.profilePopupOpen.set(true);
  }

  closeProfilePopup(){
    this.profilePopupOpen.set(false);
  }
}
