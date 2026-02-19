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
    readonly #API_URL = RouteService.getBackendUrl('/google');
    readonly #http = inject(HttpClient);

    getOrgGroups(query?: string, pageToken?: string, size: number = 5): Observable<GroupPageResponse> {
        let params = new HttpParams().set('size', size.toString());
        if (query?.trim()) {
            params = params.set('query', query.trim());
        }
        if (pageToken) {
            params = params.set('pageToken', pageToken);
        }
        return this.#http.get<GroupPageResponse>(`${this.#API_URL}/groups`, {
            withCredentials: true,
            params,
        });
    }

    getGroupSettings(groupEmail: string): Observable<GroupSettingsDto> {
        return this.#http.get<GroupSettingsDto>(`${this.#API_URL}/groups/settings`, {
            withCredentials: true,
            params: { groupEmail },
        });
    }
}

export interface GroupPageResponse {
    groups: GroupOrgDetail[];
    nextPageToken: string | null;
}

export interface GroupSettingsDto {
    whoCanJoin: string;
    whoCanView: string;
}
