import { Device } from './Device';

export type DevicePageResponse = {
  devices: Device[];
  nextPageToken: string | null;
};
