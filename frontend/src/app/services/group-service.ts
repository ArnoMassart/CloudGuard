import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { UserOrgDetail } from '../models/UserOrgDetails';
import { Observable } from 'rxjs';

export interface GroupOrgDetail {
    name: string;
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

    getOrgGroups(): Observable<GroupOrgDetail[]> {
        return this.#http.get<GroupOrgDetail[]>(`${this.#API_URL}/groups`,{
            withCredentials: true,
        })
    };
}
