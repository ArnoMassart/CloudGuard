export interface GroupOrgDetail {
  name: string;
  adminId: string;
  risk: string;
  tags: string[];
  totalMembers: number;
  externalMembers: number;
  externalAllowed: boolean;
  whoCanJoin: string;
  whoCanView: string;
}