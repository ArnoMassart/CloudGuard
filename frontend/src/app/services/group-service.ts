import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';
import { GroupOrgDetail } from '../models/groups/GroupOrgDetail';
import type { SecurityScoreBreakdown } from '../models/password/PasswordSettings';
import type { SectionWarnings } from '../models/SectionWarnings';

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
  securityScoreBreakdown?: SecurityScoreBreakdown;
  warnings?: SectionWarnings;
}

export interface GroupSettingsDto {
  whoCanJoin: string;
  whoCanView: string;
}
