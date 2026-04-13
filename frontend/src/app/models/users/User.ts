export type User = {
  email: string;
  firstName: string;
  lastName: string;
  pictureUrl?: string | null;
  roles: string[];
  createdAt: Date;
  organizationName?: string | null;
};
