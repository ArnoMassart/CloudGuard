export type SharedDriveOverviewResponse = {
  totalDrives: number;
  orphanDrives: number;
  totalHighRisk: number;
  totalExternalMembers: number;
  securityScore: number;
  notOnlyDomainUsersAllowedCount: number;
  notOnlyMembersCanAccessCount: number;
  externalMembersCount: number;
  orphanDrivesCount: number;
};
