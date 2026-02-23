import { MobileDevice } from './MobileDevice';

export type MobileDevicePageResponse = {
  devices: MobileDevice[];
  pageToken: string | null;
};
