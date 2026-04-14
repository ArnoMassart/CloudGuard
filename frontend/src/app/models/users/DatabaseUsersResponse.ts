import { User } from './User';

export type DatabaseUsersResponse = {
  users: User[];
  nextPageToken: string;
};
