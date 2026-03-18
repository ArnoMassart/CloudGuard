import { AdminWithSecurityKey } from './AdminWithSecurityKey';

export interface AdminSecurityKeysResponse {
  admins: AdminWithSecurityKey[];
  totalAdmins: number;
  errorMessage?: string;
}
