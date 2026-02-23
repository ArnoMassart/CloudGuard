export type SharedDrive = {
  id: string;
  name: string;
  totalMembers: number;
  externalMembers: number;
  totalOrganizers: number;
  createdTime: string;
  onlyDomainUsersAllowed: boolean;
  onlyMembersCanAccess: boolean;
  risk: Risk;
};

type Risk = 'Laag' | 'Middel' | 'Hoog';
