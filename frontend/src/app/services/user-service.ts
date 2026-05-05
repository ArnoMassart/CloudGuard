import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { RouteService } from './route-service';
import { Observable, tap } from 'rxjs';
import { UserPageResponse } from '../models/users/UserPageResponse';
import { UserOverviewResponse } from '../models/users/UserOverviewResponse';
import { UsersWithoutTwoFactorResponse } from '../models/users/UsersWithoutTwoFactorResponse';
import { Role, RoleLabels, RolePriority, User } from '../models/users/User';
import { DatabaseUsersResponse } from '../models/users/DatabaseUsersResponse';
import { DecisionResult } from '../models/DecisionResult';
import { UserDenyRequest } from '../models/users/UserDenyRequest';
import { DeniedUser } from '../models/users/DeniedUser';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  readonly #API_URL = RouteService.getBackendUrl('/google/users');
  readonly #http = inject(HttpClient);

  readonly requestedCount = signal<number>(0);
  readonly deniedCount = signal<number>(0);

  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user?.firstName && user?.lastName)
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    if (user?.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user?.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
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
      {},
      { responseType: 'text', withCredentials: true }
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

  requestAccess(urlPath: string): Observable<string> {
    const url = RouteService.getBackendUrl('/user' + urlPath);
    return this.#http.post(
      url,
      {},
      {
        responseType: 'text',
        withCredentials: true,
      }
    );
  }

  getRequestSent(urlPath: string): Observable<boolean> {
    const url = RouteService.getBackendUrl('/user' + urlPath);
    return this.#http.get<boolean>(url, {
      withCredentials: true,
    });
  }

  hasValidField(urlPath: string): Observable<boolean> {
    const url = RouteService.getBackendUrl('/user' + urlPath);

    return this.#http.get<boolean>(url, {
      withCredentials: true,
    });
  }

  getAllDatabaseUsers(
    size: number,
    pageToken?: string,
    query?: string,
    org?: string,
    status?: string
  ): Observable<DatabaseUsersResponse<User>> {
    const url = RouteService.getBackendUrl('/user/all');
    let params = new HttpParams().set('size', size.toString());

    console.log('Status', status);

    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);
    if (org) params = params.set('orgFilter', org);
    if (status && status !== 'all') {
      params = params.set('status', status);
    }

    return this.#http.get<DatabaseUsersResponse<User>>(url, {
      params: params,
    });
  }

  getAllRequestedDatabaseUsers(
    size: number,
    pageToken?: string,
    query?: string
  ): Observable<DatabaseUsersResponse<User>> {
    const url = RouteService.getBackendUrl('/user/all/requested-access');
    let params = new HttpParams().set('size', size.toString());

    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);

    return this.#http.get<DatabaseUsersResponse<User>>(url, {
      params: params,
    });
  }

  getAllDeniedDatabaseUsers(
    size: number,
    pageToken?: string,
    query?: string
  ): Observable<DatabaseUsersResponse<DeniedUser>> {
    const url = RouteService.getBackendUrl('/user/all/denied');
    let params = new HttpParams().set('size', size.toString());

    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);

    return this.#http.get<DatabaseUsersResponse<DeniedUser>>(url, {
      params: params,
    });
  }

  updateRolesForUser(userEmail: string, roles: Role[]) {
    const url = RouteService.getBackendUrl('/user/roles');
    const body = {
      userEmail,
      roles,
    };

    return this.#http.post<DatabaseUsersResponse<User>>(url, body, {
      withCredentials: true,
    });
  }

  updateRolesForUserWithoutAny(userEmail: string, roles: Role[]) {
    const url = RouteService.getBackendUrl('/user/roles-without');
    const body = {
      userEmail,
      roles,
    };

    return this.#http.post(url, body, {
      withCredentials: true,
    });
  }

  refreshRequestedCount(): Observable<number> {
    const url = RouteService.getBackendUrl('/user/access-requests/count');
    return this.#http.get<number>(url, { withCredentials: true }).pipe(
      tap((count) => {
        this.requestedCount.set(count);
      })
    );
  }

  refreshDeniedCount(): Observable<number> {
    const url = RouteService.getBackendUrl('/user/denied/count');
    return this.#http.get<number>(url, { withCredentials: true }).pipe(
      tap((count) => {
        this.deniedCount.set(count);
      })
    );
  }

  getRole(roles: Role[]): string {
    if (!roles || roles.length === 0) return 'User';

    const sortedRoles = [...roles].sort((a, b) => {
      const priorityA = RolePriority[a] ?? 99;
      const priorityB = RolePriority[b] ?? 99;
      return priorityA - priorityB;
    });

    const highestPriorityRole = sortedRoles[0];

    return this.getRoleLabel(highestPriorityRole);
  }

  getRoleLabel(role: string | Role): string {
    const label = RoleLabels[role as Role];

    return label ? label : role.toString();
  }

  updateUserOrg(userEmail: string, orgId: number) {
    const url = RouteService.getBackendUrl('/user/org-change');
    const body = {
      userEmail,
      orgId,
    };

    return this.#http
      .post(url, body, {
        withCredentials: true,
      })
      .pipe(
        tap(() => {
          this.refreshRequestedCount().subscribe();
        })
      );
  }

  isOrganizationSetup(): Observable<boolean> {
    const url = RouteService.getBackendUrl('/user/org-setup-status');

    return this.#http.get<boolean>(url, {
      withCredentials: true,
    });
  }

  isCloudmenStaff(): Observable<boolean> {
    const url = RouteService.getBackendUrl('/user/is-cloudmen-staff');

    return this.#http.get<boolean>(url, {
      withCredentials: true,
    });
  }

  getUsersFullName(user: User): string {
    return user.firstName + ' ' + user.lastName;
  }

  userAccepted(result: DecisionResult): Observable<string> {
    const url = RouteService.getBackendUrl('/user/accepted');

    return this.#http.post(url, result, {
      responseType: 'text',
      withCredentials: true,
    });
  }

  userDenied(result: UserDenyRequest): Observable<string> {
    const url = RouteService.getBackendUrl('/user/denied');

    return this.#http.post(url, result, {
      responseType: 'text',
      withCredentials: true,
    });
  }

  userReaccepted(email: string): Observable<string> {
    const url = RouteService.getBackendUrl('/user/reaccept');

    return this.#http.post(url, email, {
      responseType: 'text',
      withCredentials: true,
    });
  }

  switchUserStatus(email: string): Observable<string> {
    const url = RouteService.getBackendUrl('/user/status/change');

    return this.#http.post(url, email, {
      responseType: 'text',
      withCredentials: true,
    });
  }
}
