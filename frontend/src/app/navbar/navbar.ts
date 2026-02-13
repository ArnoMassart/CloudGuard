import { Component } from '@angular/core';
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
import { RouterLinkActive, RouterLink } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule, NavItem, RouterLinkActive, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
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
}
