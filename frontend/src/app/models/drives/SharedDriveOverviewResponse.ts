import type { SecurityScoreBreakdown } from '../password/PasswordSettings';

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
};
