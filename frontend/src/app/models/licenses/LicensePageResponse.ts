import { InactiveUser } from './InactiveUser';
import { LicenseType } from './LicenseType';
import { MfaStats } from './MfaStats';

export type LicensePageResponse = {
  licenseTypes: LicenseType[];
  inactiveUsers: InactiveUser[];
  mfaStats: MfaStats;
  maxLicenseAmount: number;
  chartStepSize: number;
};
