export interface DismissedNotification {
  id: string;
  severity: string;
  title: string;
  description: string;
  recommendedActions?: string[];
  notificationType: string;
  source: string;
  sourceLabel: string;
  sourceRoute: string;
  dismissedAt: string | null;
}
