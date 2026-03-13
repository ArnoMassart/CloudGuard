export interface ResolveNotificationRequest {
  source: string;
  notificationType: string;
  sourceLabel: string;
  sourceRoute: string;
  title: string;
  description: string;
  severity: string;
  recommendedActions?: string[];
}