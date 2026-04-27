import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { NavItem } from './nav-item/nav-item';
import { Profile } from '../pages/profile/profile';
import { MatDialog } from '@angular/material/dialog';
import { CookieService } from 'ngx-cookie-service';
import { CustomAuthService } from '../auth/custom-auth-service';
import { Router } from '@angular/router';
import { LogOutDialog } from '../components/log-out-dialog/log-out-dialog';
import { UserService } from '../services/user-service';
import { AppIcons } from '../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageBar } from '../components/language-bar/language-bar';
import { Role, RoleLabels } from '../models/users/User';
import { CLOUDMEN_ADMIN_EMAIL } from '../../env';

type NavItemsType = {
  Icon: LucideIconData;
  Label: string;
  Route: string;
  RequiredRole?: Role;
};

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
  readonly isMobileMenuOpen = signal(false);

  readonly requestedCount = signal(0);

  constructor() {
    effect(() => {
      this.currentUser();
      this.profileImageError.set(false);
      this.getRequestedCount();
    });
  }

  readonly NavItemsSecurity: NavItemsType[] = [
    {
      Icon: this.Icons.Shield,
      Label: 'dashboard',
      Route: '/home',
    },
    {
      Icon: this.Icons.Users,
      Label: 'user-groups',
      Route: '/users-groups',
      RequiredRole: Role.USERS_GROUPS_VIEWER,
    },
    {
      Icon: this.Icons.Building2,
      Label: 'organisational-units',
      Route: '/organizational-units',
      RequiredRole: Role.ORG_UNITS_VIEWER,
    },
    {
      Icon: this.Icons.FolderOpen,
      Label: 'shared-drives',
      Route: '/shared-drives',
      RequiredRole: Role.SHARED_DRIVES_VIEWER,
    },
    {
      Icon: this.Icons.SmartPhone,
      Label: 'devices',
      Route: '/devices',
      RequiredRole: Role.DEVICES_VIEWER,
    },
    {
      Icon: this.Icons.Key,
      Label: 'app-access',
      Route: '/app-access',
      RequiredRole: Role.APP_ACCESS_VIEWER,
    },
    {
      Icon: this.Icons.LayoutGrid,
      Label: 'app-passwords',
      Route: '/app-passwords',
      RequiredRole: Role.APP_PASSWORDS_VIEWER,
    },
    {
      Icon: this.Icons.Lock,
      Label: 'password-settings',
      Route: '/password-settings',
      RequiredRole: Role.PASSWORD_SETTINGS_VIEWER,
    },
    {
      Icon: this.Icons.Globe,
      Label: 'domain-dns',
      Route: '/domain-dns',
      RequiredRole: Role.DOMAIN_DNS_VIEWER,
    },
  ];

  readonly NavItemsControl: NavItemsType[] = [
    {
      Icon: this.Icons.CreditCard,
      Label: 'licenses',
      Route: '/licenses',
      RequiredRole: Role.LICENSES_VIEWER,
    },
    {
      Icon: this.Icons.Bell,
      Label: 'notifications-feedback',
      Route: '/reports-reactions',
    },
    {
      Icon: this.Icons.Settings,
      Label: 'security-preferences',
      Route: '/security-preferences',
      RequiredRole: Role.SECURITY_PREFERENCES_VIEWER,
    },
    {
      Icon: this.Icons.UserCog,
      Label: 'accounts-manager',
      Route: '/accounts-manager',
      RequiredRole: Role.SUPER_ADMIN,
    },
  ];

  readonly filteredSecurityItems = computed(() => {
    const user = this.currentUser();
    if (!user || !user.roles) return [];

    if (user.roles.includes(Role.SUPER_ADMIN)) {
      return this.NavItemsSecurity;
    }

    return this.NavItemsSecurity.filter(
      (item) => user.roles.includes(item.RequiredRole!) || item.Route === '/home'
    );
  });

  readonly filteredControlItems = computed(() => {
    const user = this.currentUser();
    if (!user || !user.roles) return [];

    if (user.roles.includes(Role.SUPER_ADMIN)) {
      if (user.email === CLOUDMEN_ADMIN_EMAIL) {
        return this.NavItemsControl;
      }

      return this.NavItemsControl.filter((item) => item.RequiredRole !== Role.SUPER_ADMIN);
    }

    return this.NavItemsControl.filter(
      (item) => user.roles.includes(item.RequiredRole!) || item.Route === '/reports-reactions'
    );
  });

  toggleMobileMenu() {
    this.isMobileMenuOpen.update((v) => !v);

    if (this.isMobileMenuOpen()) {
      setTimeout(() => {
        const activeItem = document.querySelector('li.selected');

        if (activeItem) {
          activeItem.scrollIntoView({
            behavior: 'smooth',
            block: 'center',
          });
        }
      }, 50);
    }
  }

  closeMobileMenu() {
    this.isMobileMenuOpen.set(false);
  }

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
      panelClass: 'dialog-panel',
      backdropClass: 'dialog-backdrop',
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

  navigateToHome() {
    this.router.navigate(['/']);
  }

  getRoleName(user: { roles: Role[] }): string {
    const role = user.roles.at(0);

    if (role === Role.SUPER_ADMIN) {
      return RoleLabels[Role.SUPER_ADMIN];
    }

    return 'viewer';
  }

  getRequestedCount() {
    if (this.currentUser()) {
      this.userService.refreshRequestedCount().subscribe({
        next: (count) => {
          this.requestedCount.set(count);
        },
        error: (err) => console.error('Error fetching requested count', err),
      });
    }
  }
}
