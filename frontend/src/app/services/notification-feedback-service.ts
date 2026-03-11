import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class NotificationFeedbackService {
    readonly #API_URL = RouteService.getBackendUrl('/notifications/feedback');
    readonly #http = inject(HttpClient);

    submitFeedback(source: string, notificationType: string, feedbackText: string): Observable<void>{
        return this.#http.post<void>(this.#API_URL,{
            source,
            notificationType,
            feedbackText
        },{
            withCredentials: true,
        });
    }

}