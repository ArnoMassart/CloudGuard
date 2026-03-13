import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Domain } from '../models/domain/Domain';

@Injectable({
  providedIn: 'root',
})
export class DomainService {
    readonly #API_URL = RouteService.getBackendUrl('/google/domains');
    readonly #http = inject(HttpClient);

    public getDomains() {
        return this.#http.get<Domain[]>(`${this.#API_URL}`, {
            withCredentials: true,
        });
    }

    public refreshCache(): Observable<string> {
        return this.#http.post(
            `${this.#API_URL}/refresh`,
            {},
            { withCredentials: true, responseType: 'text' }
        );
    }
}