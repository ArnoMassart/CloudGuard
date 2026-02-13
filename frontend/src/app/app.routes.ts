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

export const routes: Routes = [
  {
    path: '',
    component: Home,
  },
  {
    path: 'users-groups',
    component: UsersGroups,
  },
  {
    path: 'organizational-units',
    component: OrganizationalUnits,
  },
  {
    path: 'shared-drives',
    component: SharedDrives,
  },
  {
    path: 'mobile-devices',
    component: MobileDevices,
  },
  {
    path: 'app-access',
    component: AppAccess,
  },
  {
    path: 'app-passwords',
    component: AppPasswords,
  },
  {
    path: 'domain-dns',
    component: DomainDns,
  },
  {
    path: 'licenses-billing',
    component: LicensesBilling,
  },
  {
    path: 'reports-reactions',
    component: ReportsReactions,
  },
  {
    path: 'test',
    component: ApiTest,
  },
  { path: '**', redirectTo: '/' },
];
