import { Notification } from "./Notification";

export interface NotificationsResponse {
  active: Notification[];
  dismissed: Notification[];
}