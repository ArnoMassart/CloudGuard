export type UserOverviewResponse = {
  totalUsers: number;
  withoutTwoFactor: number;
  adminUsers: number;
  securityScore: number;
  activeLongNoLoginCount: number;
  inactiveRecentLoginCount: number;
};
