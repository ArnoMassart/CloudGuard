import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';

export interface ResolvedNotification {
  id: string;
  severity: string;
  title: string;
  description: string;
  recommendedActions?: string[];
  notificationType: string;
  source: string;
  sourceLabel: string;
  sourceRoute: string;
  resolvedAt: string | null;
}

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

@Injectable({
  providedIn: 'root',
})
export class ResolvedNotificationService {
  readonly #API_URL = RouteService.getBackendUrl('/notifications/resolved');
  readonly #http = inject(HttpClient);

  markAsResolved(request: ResolveNotificationRequest): Observable<void> {
    return this.#http.post<void>(this.#API_URL, request, {
      withCredentials: true,
    });
  }
}
