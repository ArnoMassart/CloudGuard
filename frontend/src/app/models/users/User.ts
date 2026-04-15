export type User = {
  email: string;
  firstName: string;
  lastName: string;
  pictureUrl?: string | null;
  roles: Role[];
  createdAt: Date;
  roleRequested: boolean;
  organizationRequested: boolean;
  organizationId: number;
  organizationName?: string | null;
};

export enum Role {
  UNASSIGNED = 'UNASSIGNED',
  SUPER_ADMIN = 'SUPER_ADMIN',

  USERS_GROUPS_VIEWER = 'USERS_GROUPS_VIEWER',
  ORG_UNITS_VIEWER = 'ORG_UNITS_VIEWER',
  SHARED_DRIVES_VIEWER = 'SHARED_DRIVES_VIEWER',
  DEVICES_VIEWER = 'DEVICES_VIEWER',
  APP_ACCESS_VIEWER = 'APP_ACCESS_VIEWER',
  APP_PASSWORDS_VIEWER = 'APP_PASSWORDS_VIEWER',
  PASSWORD_SETTINGS_VIEWER = 'PASSWORD_SETTINGS_VIEWER',
  DOMAIN_DNS_VIEWER = 'DOMAIN_DNS_VIEWER',
  LICENSES_VIEWER = 'LICENSES_VIEWER',
  NOTIFICATIONS_FEEDBACK_VIEWER = 'NOTIFICATIONS_FEEDBACK_VIEWER',
  SECURITY_PREFERENCES_VIEWER = 'SECURITY_PREFERENCES_VIEWER',
}

export const RoleLabels: Record<Role, string> = {
  [Role.UNASSIGNED]: 'user.role.unassigned',
  [Role.SUPER_ADMIN]: 'user.role.super-admin',
  [Role.USERS_GROUPS_VIEWER]: 'user.role.users-groups',
  [Role.ORG_UNITS_VIEWER]: 'user.role.org-units',
  [Role.SHARED_DRIVES_VIEWER]: 'user.role.shared-drives',
  [Role.DEVICES_VIEWER]: 'user.role.devices',
  [Role.APP_ACCESS_VIEWER]: 'user.role.app-access',
  [Role.APP_PASSWORDS_VIEWER]: 'user.role.app-passwords',
  [Role.PASSWORD_SETTINGS_VIEWER]: 'user.role.password-settings',
  [Role.DOMAIN_DNS_VIEWER]: 'user.role.domain-dns',
  [Role.LICENSES_VIEWER]: 'user.role.licenses',
  [Role.NOTIFICATIONS_FEEDBACK_VIEWER]: 'user.role.notifications-feedback',
  [Role.SECURITY_PREFERENCES_VIEWER]: 'user.role.security-preferences',
};

export const RolePriority: Record<Role, number> = {
  [Role.SUPER_ADMIN]: 1,
  [Role.NOTIFICATIONS_FEEDBACK_VIEWER]: 3,
  [Role.SECURITY_PREFERENCES_VIEWER]: 4,
  [Role.USERS_GROUPS_VIEWER]: 5,
  [Role.ORG_UNITS_VIEWER]: 6,
  [Role.SHARED_DRIVES_VIEWER]: 7,
  [Role.DEVICES_VIEWER]: 8,
  [Role.APP_ACCESS_VIEWER]: 9,
  [Role.APP_PASSWORDS_VIEWER]: 10,
  [Role.PASSWORD_SETTINGS_VIEWER]: 11,
  [Role.DOMAIN_DNS_VIEWER]: 12,
  [Role.LICENSES_VIEWER]: 13,

  [Role.UNASSIGNED]: 99,
};
