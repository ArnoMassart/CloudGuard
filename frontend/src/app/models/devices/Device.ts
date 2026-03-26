export type Device = {
  resourceId: string;
  deviceType: string;
  userName: string;
  userEmail: string;
  deviceName: string;
  model: string;
  os: string;
  lastSync: string;
  status: string;
  complianceScore: number;
  lockSecure: boolean;
  screenLockText: string;
  encSecure: boolean;
  encryptionText: string;
  osSecure: boolean;
  osText: string;
  intSecure: boolean;
  integrityText: string;
};

export type DeviceFactor = {
  key: string;
  label: string;
  icon: any;
  secure: boolean;
  text: string;
  state: 'ok' | 'warn' | 'muted';
};
