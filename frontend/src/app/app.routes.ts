import { Routes } from '@angular/router';
import { ApiTest } from './pages/api-test/api-test';
import { Home } from './pages/home/home';
import { UsersGroups } from './pages/security-section/users-groups/users-groups';
import { OrganizationalUnits } from './pages/security-section/organizational-units/organizational-units';
import { SharedDrives } from './pages/security-section/shared-drives/shared-drives';
import { MobileDevices } from './pages/security-section/mobile-devices/mobile-devices';
import { AppAccess } from './pages/security-section/app-access/app-access';
import { AppPasswords } from './pages/security-section/app-passwords/app-passwords';
import { DomainDns } from './pages/security-section/domain-dns/domain-dns';
import { LicensesBilling } from './pages/control-section/licenses-billing/licenses-billing';
import { ReportsReactions } from './pages/control-section/reports-reactions/reports-reactions';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: Home,
    canActivate: [authGuard],
  },
  {
    path: 'login',
    component: ApiTest,
  },
  {
    path: 'users-groups',
    component: UsersGroups,
    canActivate: [authGuard],
  },
  {
    path: 'organizational-units',
    component: OrganizationalUnits,
    canActivate: [authGuard],
  },
  {
    path: 'shared-drives',
    component: SharedDrives,
    canActivate: [authGuard],
  },
  {
    path: 'mobile-devices',
    component: MobileDevices,
    canActivate: [authGuard],
  },
  {
    path: 'app-access',
    component: AppAccess,
    canActivate: [authGuard],
  },
  {
    path: 'app-passwords',
    component: AppPasswords,
    canActivate: [authGuard],
  },
  {
    path: 'domain-dns',
    component: DomainDns,
    canActivate: [authGuard],
  },
  {
    path: 'licenses-billing',
    component: LicensesBilling,
    canActivate: [authGuard],
  },
  {
    path: 'reports-reactions',
    component: ReportsReactions,
    canActivate: [authGuard],
  },
  {
    path: 'test',
    component: ApiTest,
  },
  { path: '**', redirectTo: '/' },
];
