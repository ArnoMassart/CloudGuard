import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';

export interface AppPassword {
    userName: string;
    userEmail: string;
    userRole: string;
    codeId: number;
    name: string;
    createdAt: Date;
    lastUsedAt: Date | null;
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
    appPasswords: AppPassword[];
}

@Injectable({
  providedIn: 'root',
})
export class AppPasswordsService {
    readonly #API_URL = RouteService.getBackendUrl('/google/app-passwords');
    readonly #http = inject(HttpClient);

    public getAppPasswords() {
        return this.#http.get<AppPassword[]>(`${this.#API_URL}`, {
            withCredentials: true
        });
    }
}