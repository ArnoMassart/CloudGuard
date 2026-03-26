/** Backend `UserSecurityEvaluation` violation codes */
export const USER_SECURITY_VIOLATION = {
  NO_2FA: 'NO_2FA',
  ACTIVITY_STALE: 'ACTIVITY_STALE',
  ACTIVITY_INACTIVE_RECENT: 'ACTIVITY_INACTIVE_RECENT',
} as const;

export type UserOrgDetail = {
  fullName: string;
  email: string;
  role: string;
  active: boolean;
  lastLogin: string;
  twoFactorEnabled: boolean;
  securityConform: boolean;
  /** When every listed violation is muted by Beveiligingsvoorkeuren, UI shows Conform. */
  securityViolationCodes?: string[];
};
