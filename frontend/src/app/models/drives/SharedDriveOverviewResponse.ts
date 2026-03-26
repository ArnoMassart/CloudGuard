import type { SecurityScoreBreakdown } from '../password/PasswordSettings';
import type { SectionWarnings } from '../SectionWarnings';

export type SharedDriveOverviewResponse = {
  totalDrives: number;
  orphanDrives: number;
  totalHighRisk: number;
  totalExternalMembers: number;
  securityScore: number;
  notOnlyDomainUsersAllowedCount: number;
  notOnlyMembersCanAccessCount: number;
  externalMembersDriveCount: number;
  securityScoreBreakdown?: SecurityScoreBreakdown;
  warnings?: SectionWarnings;
};
