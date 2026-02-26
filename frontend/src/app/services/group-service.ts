import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';

export interface GroupOrgDetail {
  name: string;
  adminId: string;
  risk: string;
  tags: string[];
  totalMembers: number;
  externalMembers: number;
  externalAllowed: boolean;
  whoCanJoin: string;
  whoCanView: string;
}

@Injectable({
  providedIn: 'root',
})
export class GroupService {
  readonly #API_URL = RouteService.getBackendUrl('/google/groups');
  readonly #http = inject(HttpClient);

  getGroupsOverview(): Observable<GroupOverviewResponse> {
    return this.#http.get<GroupOverviewResponse>(`${this.#API_URL}/overview`, {
      withCredentials: true,
    });
  }

  getOrgGroups(
    query?: string,
    pageToken?: string,
    size: number = 5
  ): Observable<GroupPageResponse> {
    let params = new HttpParams().set('size', size.toString());
    if (query?.trim()) {
      params = params.set('query', query.trim());
    }
    if (pageToken) {
      params = params.set('pageToken', pageToken);
    }
    return this.#http.get<GroupPageResponse>(`${this.#API_URL}`, {
      withCredentials: true,
      params,
    });
  }

  getGroupSettings(groupEmail: string): Observable<GroupSettingsDto> {
    return this.#http.get<GroupSettingsDto>(`${this.#API_URL}/settings`, {
      withCredentials: true,
      params: { groupEmail },
    });
  }

  refreshGroupCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {
        withCredentials: true,
      },
      { responseType: 'text' }
    );
  }
}

export interface GroupPageResponse {
  groups: GroupOrgDetail[];
  nextPageToken: string | null;
}

export interface GroupOverviewResponse {
  totalGroups: number;
  groupsWithExternal: number;
  highRiskGroups: number;
  mediumRiskGroups: number;
  lowRiskGroups: number;
  securityScore: number;
}

export interface GroupSettingsDto {
  whoCanJoin: string;
  whoCanView: string;
}
