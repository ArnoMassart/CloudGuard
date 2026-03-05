import { InactiveUser } from './InactiveUser';
import { LicenseType } from './LicenseType';

export type LicensePageResponse = {
  licenseTypes: LicenseType[];
  inactiveUsers: InactiveUser[];
  maxLicenseAmount: number;
  chartStepSize: number;
};
