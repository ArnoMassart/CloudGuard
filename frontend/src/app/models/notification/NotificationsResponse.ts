import { Notification } from "./Notification";

export interface NotificationsResponse {
  active: Notification[];
  resolved: Notification[];
}