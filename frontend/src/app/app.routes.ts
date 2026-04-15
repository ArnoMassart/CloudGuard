import { Routes } from '@angular/router';
import { ApiTest } from './pages/api-test/api-test';
import { Callback } from './auth/callback/callback';
import { Home } from './pages/home/home';
import { UsersGroups } from './pages/security-section/users-groups/users-groups';
import { OrganizationalUnits } from './pages/security-section/organizational-units/organizational-units';
import { SharedDrives } from './pages/security-section/shared-drives/shared-drives';
import { Devices } from './pages/security-section/devices/devices';
import { AppAccess } from './pages/security-section/app-access/app-access';
import { AppPasswords } from './pages/security-section/app-passwords/app-passwords';
import { PasswordSettings } from './pages/security-section/password-settings/password-settings';
import { DomainDns } from './pages/security-section/domain-dns/domain-dns';
import { Licenses } from './pages/control-section/licenses/licenses';
import { ReportsReactions } from './pages/control-section/reports-reactions/reports-reactions';
import { SecurityPreferences } from './pages/control-section/security-preferences/security-preferences';
import { authGuard } from './auth/auth.guard';
import { Forbidden } from './pages/forbidden/forbidden';
import { TeamleaderAccessDenied } from './pages/teamleader-access-denied/teamleader-access-denied';
import { ServerError } from './pages/server-error/server-error';
import { accessGuard } from './access/access.guard';
import { Login } from './pages/login/login';
import { RequestAccess } from './pages/request-access/request-access';
import { AccountsManager } from './pages/control-section/accounts-manager/accounts-manager';
import { roleGuard } from './guards/role.guard';
import { Role } from './models/users/User';
import { AccessDenied } from './pages/access-denied/access-denied';
import { NoOrganization } from './pages/no-organization/no-organization';
import { CLOUDMEN_ADMIN_EMAIL } from '../env';

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
    path: 'no-access',
    component: AccessDenied,
  },
  {
    path: 'request-access',
    component: RequestAccess,
  },
  {
    path: 'no-organization',
    component: NoOrganization,
  },
  {
    path: 'server-error',
    component: ServerError,
  },
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },
  {
    path: 'home',
    component: Home,
    canActivate: [authGuard, accessGuard],
  },
  {
    path: 'users-groups',
    component: UsersGroups,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.USERS_GROUPS_VIEWER],
    },
  },
  {
    path: 'organizational-units',
    component: OrganizationalUnits,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.ORG_UNITS_VIEWER],
    },
  },
  {
    path: 'shared-drives',
    component: SharedDrives,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.SHARED_DRIVES_VIEWER],
    },
  },
  {
    path: 'devices',
    component: Devices,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.DEVICES_VIEWER],
    },
  },
  {
    path: 'app-access',
    component: AppAccess,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.APP_ACCESS_VIEWER],
    },
  },
  {
    path: 'app-passwords',
    component: AppPasswords,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.APP_PASSWORDS_VIEWER],
    },
  },
  {
    path: 'password-settings',
    component: PasswordSettings,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.PASSWORD_SETTINGS_VIEWER],
    },
  },
  {
    path: 'domain-dns',
    component: DomainDns,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.DOMAIN_DNS_VIEWER],
    },
  },
  {
    path: 'licenses',
    component: Licenses,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.LICENSES_VIEWER],
    },
  },
  {
    path: 'reports-reactions',
    component: ReportsReactions,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.NOTIFICATIONS_FEEDBACK_VIEWER],
    },
  },
  {
    path: 'security-preferences',
    component: SecurityPreferences,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.SECURITY_PREFERENCES_VIEWER],
    },
  },
  {
    path: 'accounts-manager',
    component: AccountsManager,
    canActivate: [authGuard, accessGuard, roleGuard],
    data: {
      requiredRoles: [Role.SUPER_ADMIN],
      allowedEmails: [CLOUDMEN_ADMIN_EMAIL],
    },
  },
  {
    path: 'test',
    component: ApiTest,
  },
  { path: '**', redirectTo: 'home' },
];
