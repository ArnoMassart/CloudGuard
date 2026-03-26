import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';
import { UserPageResponse } from '../models/users/UserPageResponse';
import { UserOverviewResponse } from '../models/users/UserOverviewResponse';
import { UsersWithoutTwoFactorResponse } from '../models/users/UsersWithoutTwoFactorResponse';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  readonly #API_URL = RouteService.getBackendUrl('/google/users');
  readonly #http = inject(HttpClient);

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user?.firstName && user?.lastName)
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    if (user?.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user?.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  getRole(user: { roles: string[] }): string {
    const roles = user.roles;

    if (!roles) return '';

    return roles.at(0) ?? 'Admin';
  }

  getOrgUsers(size: number, pageToken?: string, query?: string): Observable<UserPageResponse> {
    let params = new HttpParams().set('size', size.toString());

    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);

    return this.#http.get<UserPageResponse>(`${this.#API_URL}`, {
      withCredentials: true,
      params: params,
    });
  }

  getUsersPageOverview(): Observable<UserOverviewResponse> {
    return this.#http.get<UserOverviewResponse>(`${this.#API_URL}/overview`, {
      withCredentials: true,
    });
  }

  getUsersWithoutTwoFactor(): Observable<UsersWithoutTwoFactorResponse> {
    return this.#http.get<UsersWithoutTwoFactorResponse>(`${this.#API_URL}/without-two-factor`, {
      withCredentials: true,
    });
  }

  refreshUsersCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {
        withCredentials: true,
      },
      { responseType: 'text' }
    );
  }

  updateLanguage(language: string) {
    const url = RouteService.getBackendUrl('/user/language');
    return this.#http.post(url, { language: language });
  }

  getLanguage(): Observable<string> {
    const url = RouteService.getBackendUrl('/user/language');
    return this.#http.get(url, {
      responseType: 'text',
      withCredentials: true,
    });
  }
}
