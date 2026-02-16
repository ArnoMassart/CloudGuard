import { Role } from './Role';

export type User = {
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  createdAt: Date;
};
