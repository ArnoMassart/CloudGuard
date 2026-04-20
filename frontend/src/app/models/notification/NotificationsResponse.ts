import { Notification } from './Notification';

export interface NotificationsResponse {
  active: Notification[];
  solved: Notification[];
}
