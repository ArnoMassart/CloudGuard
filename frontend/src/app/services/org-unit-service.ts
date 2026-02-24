import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';
import { OrgUnitNode } from '../pages/security-section/organizational-units/organizational-units';

export interface OrgUnitNodeDto {
    id: string;
    name: string;
    orgUnitPath?: string;
    userCount: number;
    children?: OrgUnitNodeDto[];
    root?: boolean;
    isRoot?: boolean;
}

export interface OrgUnitPolicyDto {
    key: string;
    title: string;
    description: string;
    status: string;
    statusClass: string;
    baseExplanation?: string;
    inheritanceExplanation?: string;
    inherited: boolean;
    source: string;
    settingsLinkText?: string;
    adminLink?: string;
}

@Injectable({
  providedIn: 'root',
})
export class OrgUnitService {
    readonly #API_URL = RouteService.getBackendUrl('/google');
    readonly #http = inject(HttpClient);

    getOrgUnitTree(): Observable<OrgUnitNode> {
        return this.#http.get<OrgUnitNode>(`${this.#API_URL}/org-units`, {
            withCredentials: true,
        });
    }

    getPoliciesForOrgUnit(orgUnitPath: string): Observable<OrgUnitPolicyDto[]> {
        return this.#http.get<OrgUnitPolicyDto[]>(`${this.#API_URL}/org-units/policies`, {
            params: { orgUnitPath },
            withCredentials: true,
        });
    }
}