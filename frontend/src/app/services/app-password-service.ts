import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';

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

    public getAppPasswords() {
        return this.#http.get<Array<{
            name: string;
            email: string;
            role: string;
            tsv: boolean;
            passwords: AppPassword[];
        }>>(`${this.#API_URL}`, {
            withCredentials: true
        });
    }

    public getOverview() {
        return this.#http.get<AppPasswordOverviewResponse>(`${this.#API_URL}/overview`, {
            withCredentials: true
        });
    }
}