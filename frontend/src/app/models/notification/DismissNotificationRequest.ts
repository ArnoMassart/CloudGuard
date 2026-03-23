export interface DismissNotificationRequest {
  source: string;
  notificationType: string;
  sourceLabel: string;
  sourceRoute: string;
  title: string;
  description: string;
  severity: string;
  recommendedActions?: string[];
}
