import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  static getBackendUrl(path?: string): string {
    return '/api' + path;
  }
}
