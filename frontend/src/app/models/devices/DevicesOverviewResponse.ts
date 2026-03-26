import type { SecurityScoreBreakdown } from '../password/PasswordSettings';
import type { SectionWarnings } from '../SectionWarnings';

export type DevicesOverviewResponse = {
  totalDevices: number;
  totalNonCompliant: number;
  totalApprovedDevices: number;
  securityScore: number;
  lockScreenCount: number;
  encryptionCount: number;
  osVersionCount: number;
  integrityCount: number;
  securityScoreBreakdown?: SecurityScoreBreakdown;
  warnings?: SectionWarnings;
};
