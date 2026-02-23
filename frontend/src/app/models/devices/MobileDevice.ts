export type MobileDevice = {
  resourceId: string;
  userName: string;
  userEmail: string;
  deviceName: string;
  model: string;
  os: string;
  lastSync: string;
  status: string;
  complianceScore: number;
  isScreenLockSecure: boolean;
  screenLockText: string;
  isEncryptionSecure: boolean;
  encryptionText: string;
  isOsSecure: boolean;
  osText: string;
  isIntegritySecure: boolean;
  integrityText: string;
};
