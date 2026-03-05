import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';

export interface DnsRecord {
  type: string;
  name: string;
  values: string[];
  status: string;
  importance?: string;
  message?: string;
}

export interface DnsRecordResponse {
  domain: string;
  rows: DnsRecord[];
}

@Injectable({
  providedIn: 'root',
})
export class DnsService {
    readonly #API_URL = RouteService.getBackendUrl('/google/dns-records');
    readonly #http = inject(HttpClient);

    public getDnsRecords(domain: string) {
        return this.#http.get<DnsRecordResponse>(`${this.#API_URL}/records`, {
            params: { domain },
            withCredentials: true,
        });
    }
}