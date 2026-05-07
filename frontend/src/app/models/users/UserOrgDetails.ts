/** Backend `UserSecurityEvaluation` violation codes */
export const USER_SECURITY_VIOLATION = {
  NO_2FA: 'NO_2FA',
  ACTIVITY_STALE: 'ACTIVITY_STALE',
  ACTIVITY_INACTIVE_RECENT: 'ACTIVITY_INACTIVE_RECENT',
} as const;

export type UserOrgDetail = {
  fullName: string;
  email: string;
  pictureUrl?: string | null;
  role: string;
  isActive: boolean;
  lastLogin: string;
  isTwoFactorEnabled: boolean;
  isSecurityConform: boolean;
  securityViolationCodes?: string[];
};
