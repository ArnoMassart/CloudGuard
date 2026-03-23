import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';
import { ResolveNotificationRequest } from '../models/notification/ResolveNotificationRequest';

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

  unDismiss(source: string, notificationType: string): Observable<void> {
    return this.#http.delete<void>(this.#API_URL, {
      params: { source, notificationType },
      withCredentials: true,
    });
  }
}
