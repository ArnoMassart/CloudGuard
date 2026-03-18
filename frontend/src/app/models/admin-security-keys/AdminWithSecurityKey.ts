export interface AdminWithSecurityKey {
  id: string;
  name: string;
  email: string;
  role: string;
  orgUnitPath: string;
  twoFactorEnabled: boolean;
  numSecurityKeys: number;
}
