import { Notification } from './Notification';

export interface NotificationsResponse {
  active: Notification[];
  solved: Notification[];
  /** ISO-8601 instant from API, or null/undefined if never synced */
  lastNotificationSyncAt?: string | null;
}
