import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface AppPassword {
    codeId: number;
    name: string;
    creationTime: string | null;
    lastTimeUsed: string | null;
}

export interface AppPasswordOverviewResponse {
    allowed: boolean;
    totalAppPasswords: number;
    totalHighRiskAppPasswords: number;
    securityScore: number;
}

export interface AppPasswordPageResponse {
    users: Array<{ name: string; email: string; role: string; tsv: boolean; passwords: AppPassword[] }>;
    nextPageToken: string | null;
}

export interface UserAppPasswords {
    name: string;
    email: string;
    role: string;
    twoFactorEnabled: boolean;
    appPasswords: AppPassword[];
}

@Injectable({
  providedIn: 'root',
})
export class AppPasswordsService {
    readonly #API_URL = RouteService.getBackendUrl('/google/app-passwords');
    readonly #http = inject(HttpClient);

    public getAppPasswords(size: number, pageToken?: string) {
        let params = new HttpParams().set('size', String(size));
        if (pageToken) params = params.set('pageToken', pageToken);
        return this.#http.get<AppPasswordPageResponse>(`${this.#API_URL}`, {
            withCredentials: true,
            params,
        });
    }

    public getOverview() {
        return this.#http.get<AppPasswordOverviewResponse>(`${this.#API_URL}/overview`, {
            withCredentials: true
        });
    }
}