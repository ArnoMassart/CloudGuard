import { UserOrgDetail } from './UserOrgDetails';

export type UserPageResponse = {
  users: UserOrgDetail[];
  nextPageToken: string;
};
