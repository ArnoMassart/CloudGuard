import { AdminWithSecurityKey } from '../admin-security-keys/AdminWithSecurityKey';

export interface PasswordSettings {
  passwordPoliciesByOu: OuPasswordPolicy[];
  twoStepVerification: TwoStepVerification;
  usersWithForcedChange: PasswordChangeRequirement[];
  summary: PasswordSettingsSummary;
  adminsWithoutSecurityKeys: AdminWithSecurityKey[];
  adminsSecurityKeysErrorMessage?: string;
  securityScore: number;
}

export interface OuPasswordPolicy {
  orgUnitPath: string;
  orgUnitName: string;
  userCount: number;
  score: number;
  problemCount: number;
  minLength: number | null;
  expirationDays: number | null;
  strongPasswordRequired: boolean | null;
  reusePreventionCount: number | null;
  inherited: boolean;
}

export interface TwoStepVerification {
  byOrgUnit: OrgUnit2Sv[];
  totalEnrolled: number;
  totalEnforced: number;
  totalUsers: number;
}

export interface OrgUnit2Sv {
  orgUnitPath: string;
  orgUnitName: string;
  enforced: boolean;
  enrolledCount: number;
  totalCount: number;
}

export interface PasswordChangeRequirement {
  email: string;
  fullName: string;
  orgUnitPath: string;
  reason: string;
}

export interface PasswordSettingsSummary {
  usersWithForcedChange: number;
  usersWith2SvEnrolled: number;
  usersWith2SvEnforced: number;
  totalUsers: number;
}
