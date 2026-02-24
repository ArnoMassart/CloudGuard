import { MobileDevice } from './MobileDevice';

export type MobileDevicePageResponse = {
  devices: MobileDevice[];
  nextPageToken: string | null;
};
