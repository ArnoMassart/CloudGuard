import { Routes } from '@angular/router';
import { ApiTest } from './pages/api-test/api-test';
import { Login } from './auth/login/login';
import { Callback } from './auth/callback/callback';
import { Home } from './pages/home/home';
import { UsersGroups } from './pages/security-section/users-groups/users-groups';
import { OrganizationalUnits } from './pages/security-section/organizational-units/organizational-units';
import { SharedDrives } from './pages/security-section/shared-drives/shared-drives';
import { MobileDevices } from './pages/security-section/mobile-devices/mobile-devices';
import { AppAccess } from './pages/security-section/app-access/app-access';
import { AppPasswords } from './pages/security-section/app-passwords/app-passwords';
import { DomainDns } from './pages/security-section/domain-dns/domain-dns';
import { Licenses } from './pages/control-section/licenses/licenses';
import { ReportsReactions } from './pages/control-section/reports-reactions/reports-reactions';
import { authGuard } from './auth/auth.guard';
import { Forbidden } from './pages/forbidden/forbidden';
import { TeamleaderTest } from './pages/teamleader-test/teamleader-test';
import { TeamleaderAccessDenied } from './pages/teamleader-access-denied/teamleader-access-denied';
import { ServerError } from './pages/server-error/server-error';

export const routes: Routes = [
  {
    path: 'login',
    component: Login,
  },
  {
    path: 'forbidden',
    component: Forbidden,
  },
  {
    path: 'callback',
    component: Callback,
  },
  {
    path: 'access-denied',
    component: TeamleaderAccessDenied,
  },
  {
    path: 'server-error',
    component: ServerError,
  },
  {
    path: 'teamleader',
    component: TeamleaderTest,
  },
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },
  {
    path: 'home',
    component: Home,
    canActivate: [authGuard],
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
    path: 'licenses',
    component: Licenses,
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
  { path: '**', redirectTo: 'home' },
];
