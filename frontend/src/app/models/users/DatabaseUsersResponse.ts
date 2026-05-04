export type DatabaseUsersResponse<T> = {
  users: T[];
  nextPageToken: string;
};
