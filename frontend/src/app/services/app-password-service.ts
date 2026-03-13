import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppPasswordPageResponse } from '../models/app-password/AppPasswordPageResponse';
import { AppPasswordOverviewResponse } from '../models/app-password/AppPasswordOverviewResponse';

@Injectable({
  providedIn: 'root',
})
export class AppPasswordsService {
    readonly #API_URL = RouteService.getBackendUrl('/google/app-passwords');
    readonly #http = inject(HttpClient);

    public getAppPasswords(size: number, pageToken?: string, query?: string) {
        let params = new HttpParams().set('size', String(size));
        if (pageToken) params = params.set('pageToken', pageToken);
        if (query?.trim()) params = params.set('query', query.trim());
        return this.#http.get<AppPasswordPageResponse>(`${this.#API_URL}`, {
            withCredentials: true,
            params,
        });
    }

    public getOverview() {
        return this.#http.get<AppPasswordOverviewResponse>(`${this.#API_URL}/overview`, {
            withCredentials: true
        });
    }

    public refreshCache(): Observable<string> {
        return this.#http.post(
            `${this.#API_URL}/refresh`,
            {},
            { withCredentials: true, responseType: 'text' }
        );
    }
}