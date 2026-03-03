import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OAuthPagedResponse } from '../models/o-auth/OAuthPagedResponse';
import { OAuthOverviewResponse } from '../models/o-auth/OAuthOverviewResponse';
import { Risk } from '../models/o-auth/Risk';

@Injectable({
  providedIn: 'root',
})
export class OAuthService {
  readonly #API_URL = RouteService.getBackendUrl('/google/oAuth');
  readonly #http = inject(HttpClient);

  getApps(
    size: number,
    risk: Risk,
    pageToken?: string,
    query?: string
  ): Observable<OAuthPagedResponse> {
    let params = new HttpParams().set('size', size.toString());
    params = params.set('risk', risk);
    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);

    return this.#http.get<OAuthPagedResponse>(this.#API_URL, {
      params,
      withCredentials: true,
    });
  }

  getOAuthPageOverview(): Observable<OAuthOverviewResponse> {
    return this.#http.get<OAuthOverviewResponse>(`${this.#API_URL}/overview`, {
      withCredentials: true,
    });
  }

  refreshOAuthCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {
        withCredentials: true,
      },
      { responseType: 'text' }
    );
  }
}
