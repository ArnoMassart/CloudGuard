import { Organization } from './Organization';

export type DatabaseOrgResponse = {
  organizations: Organization[];
  nextPageToken: string;
};
