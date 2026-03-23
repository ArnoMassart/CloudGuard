import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';
import { DismissNotificationRequest } from '../models/notification/DismissNotificationRequest';

@Injectable({
  providedIn: 'root',
})
export class DismissedNotificationService {
  readonly #API_URL = RouteService.getBackendUrl('/notifications/dismissed');
  readonly #http = inject(HttpClient);

  markAsDismissed(request: DismissNotificationRequest): Observable<void> {
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
