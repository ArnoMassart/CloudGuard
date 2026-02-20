import { SharedDrive } from './SharedDrive';

export type SharedDrivePageResponse = {
  drives: SharedDrive[];
  nextPageToken: string | null;
};
