export type InactiveUser = {
  email: string;
  lastLogin: string;
  licenseType: string;
  isTwoFactorEnabled: boolean;
  daysInactive: number;
};
