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

    getOrgGroups(query?: string): Observable<GroupOrgDetail[]> {
        let params = new HttpParams();
        if (query?.trim()) {
            params = params.set('query', query.trim());
        }
        return this.#http.get<GroupOrgDetail[]>(`${this.#API_URL}/groups`, {
            withCredentials: true,
            params,
        });
    }
}
