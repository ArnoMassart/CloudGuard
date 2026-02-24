export type MobileDevicesOverviewResponse = {
  totalDevices: number;
  totalNonCompliant: number;
  totalApprovedDevices: number;
  securityScore: number;
  lockScreenCount: number;
  encryptionCount: number;
  osVersionCount: number;
  integrityCount: number;
};
