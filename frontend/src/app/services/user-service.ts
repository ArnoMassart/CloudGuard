import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { UserOrgDetail } from '../models/UserOrgDetails';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  readonly #API_URL = RouteService.getBackendUrl('/google');
  readonly #http = inject(HttpClient);

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user?.firstName && user?.lastName)
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    if (user?.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user?.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  getRole(user: { roles: string[] }): string {
    return user.roles.length > 0 ? user.roles[0] : 'Admin';
  }

  getOrgUsers(): Observable<UserOrgDetail[]> {
    return this.#http.get<UserOrgDetail[]>(`${this.#API_URL}/users`, {
      withCredentials: true,
    });
  }
}
