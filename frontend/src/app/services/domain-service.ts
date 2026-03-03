import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';

export interface Domain {
    domainName: string;
    kind: string;
    isPrimary: boolean;
    isVerified: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class DomainService {
    readonly #API_URL = RouteService.getBackendUrl('/google/domains');
    readonly #http = inject(HttpClient);

    public getDomains(){
        return this.#http.get<Domain[]>(`${this.#API_URL}`, {
            withCredentials: true,
        });
    }
}