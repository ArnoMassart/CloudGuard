export type User = {
  email: string;
  firstName: string;
  lastName: string;
  pictureUrl?: string | null;
  roles: Role[];
  createdAt: Date;
  roleRequested: boolean;
};

export enum Role {
  UNASSIGNED = 'UNASSIGNED',
  SUPER_ADMIN = 'SUPER_ADMIN',
  DASHBOARD_ADMIN = 'DASHBOARD_ADMIN',
  USERS_GROUPS_ADMIN = 'USERS_GROUPS_ADMIN',
  ORG_UNITS_ADMIN = 'ORG_UNITS_ADMIN',
  SHARED_DRIVES_ADMIN = 'SHARED_DRIVES_ADMIN',
  DEVICES_ADMIN = 'DEVICES_ADMIN',
  APP_ACCESS_ADMIN = 'APP_ACCESS_ADMIN',
  APP_PASSWORDS_ADMIN = 'APP_PASSWORDS_ADMIN',
  PASSWORD_SETTINGS_ADMIN = 'PASSWORD_SETTINGS_ADMIN',
  DOMAIN_DNS_ADMIN = 'DOMAIN_DNS_ADMIN',
  LICENSES_ADMIN = 'LICENSES_ADMIN',
  NOTIFICATIONS_FEEDBACK_ADMIN = 'NOTIFICATIONS_FEEDBACK_ADMIN',
  SECURITY_PREFERENCES_ADMIN = 'SECURITY_PREFERENCES_ADMIN',
}

export const RoleLabels: Record<Role, string> = {
  [Role.UNASSIGNED]: 'Niet Toegewezen',
  [Role.SUPER_ADMIN]: 'Super Admin',
  [Role.DASHBOARD_ADMIN]: 'Dashboard Admin',
  [Role.USERS_GROUPS_ADMIN]: 'Gebruikers & Groepen Admin',
  [Role.ORG_UNITS_ADMIN]: 'Organisatie-eenheden Admin',
  [Role.SHARED_DRIVES_ADMIN]: 'Gedeelde Drives Admin',
  [Role.DEVICES_ADMIN]: 'Apparaten Admin',
  [Role.APP_ACCESS_ADMIN]: 'App Toegang Admin',
  [Role.APP_PASSWORDS_ADMIN]: 'App Wachtwoorden Admin',
  [Role.PASSWORD_SETTINGS_ADMIN]: 'Wachtwoordinstellingen Admin',
  [Role.DOMAIN_DNS_ADMIN]: 'Domein & DNS Admin',
  [Role.LICENSES_ADMIN]: 'Licenties Admin',
  [Role.NOTIFICATIONS_FEEDBACK_ADMIN]: 'Notificaties & Feedback Admin',
  [Role.SECURITY_PREFERENCES_ADMIN]: 'Beveiligingsvoorkeuren Admin',
};
