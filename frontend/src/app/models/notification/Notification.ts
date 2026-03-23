export type NotificationSeverity = 'critical' | 'warning' | 'info';

export type NotificationStatus = 'new' | 'in_behandeling' | 'dismissed';

export interface Notification {
  id: string;
  severity: NotificationSeverity;
  title: string;
  description: string;
  recommendedActions?: string[];
  notificationType: string;
  source: string;
  sourceLabel: string;
  sourceRoute: string;
  status?: NotificationStatus;
  supportsDetails?: boolean;
}