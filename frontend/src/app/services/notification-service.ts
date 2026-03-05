import { Injectable } from "@angular/core";

export interface Notification {
    id: string;
    domainId: string;
    title: string;
    message: string;
    recommendedAction: string;
    severity: 'CRITICAL' | 'WARNING' | 'INFO';
    status: 'OPEN' | 'IN_PROGRESS' | 'RESPOLVED';
    notificationType: string;
    ticketId: string;
    createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {

}