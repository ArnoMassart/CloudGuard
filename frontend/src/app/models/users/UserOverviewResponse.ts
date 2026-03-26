import type { SecurityScoreBreakdown } from '../password/PasswordSettings';
import type { SectionWarnings } from '../SectionWarnings';

export type UserOverviewResponse = {
  totalUsers: number;
  withoutTwoFactor: number;
  adminUsers: number;
  securityScore: number;
  activeLongNoLoginCount: number;
  inactiveRecentLoginCount: number;
  securityScoreBreakdown?: SecurityScoreBreakdown;
  warnings?: SectionWarnings;
};
