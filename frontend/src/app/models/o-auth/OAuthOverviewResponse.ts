import type { SecurityScoreBreakdown } from '../password/PasswordSettings';

export type OAuthOverviewResponse = {
  totalThirdPartyApps: number;
  totalHighRiskApps: number;
  totalPermissionsGranted: number;
  securityScore: number;
  securityScoreBreakdown?: SecurityScoreBreakdown;
};
