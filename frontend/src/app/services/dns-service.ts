import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';
import { DnsRecordResponse } from '../models/dns/DnsRecordResponse';

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
