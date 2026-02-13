import { Injectable } from '@angular/core';
import { BACKEND_URL } from '../../../env';

@Injectable({
  providedIn: 'root',
})
export class RouteService {
  static getBackendUrl(path?: string): string {
    return (BACKEND_URL ? `${BACKEND_URL}/api` : ' /api') + path;
  }
}
