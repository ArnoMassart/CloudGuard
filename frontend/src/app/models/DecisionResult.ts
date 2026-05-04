import { Role } from './users/User';

export type DecisionResult = {
  userEmail: string;
  isAccepted: boolean;
  isSuperAdmin: boolean;
  organizationId: string;
  roles: Role[];
  denyReason: string;
};
