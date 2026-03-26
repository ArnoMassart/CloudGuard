export type SharedDrive = {
  id: string;
  name: string;
  totalMembers: number;
  externalMembers: number;
  totalOrganizers: number;
  createdTime: string;
  parsedTime: string;
  onlyDomainUsersAllowed: boolean;
  onlyMembersCanAccess: boolean;
  risk: Risk;
};

type Risk = 'low' | 'middle' | 'high';
