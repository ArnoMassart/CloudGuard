import { AdminWithSecurityKey } from './AdminWithSecurityKey';

export interface AdminSecurityKeysResponse {
  admins: AdminWithSecurityKey[];
  errorMessage?: string;
}
