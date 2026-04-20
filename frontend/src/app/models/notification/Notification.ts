export type NotificationSeverity = 'critical' | 'warning' | 'info';

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
  hasReported?: boolean;
  supportsDetails?: boolean;
  /** Present when API merges active + solved lists: open issues vs resolved in projection. */
  instanceStatus?: 'active' | 'solved';
}