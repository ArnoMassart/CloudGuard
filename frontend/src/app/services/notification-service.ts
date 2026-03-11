import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { RouteService } from './route-service';
import { UserService } from './user-service';
import { DriveService } from './drive-service';
import { MobileDeviceService } from './mobile-device-service';
import { GroupService } from './group-service';
import { OAuthService } from './o-auth-service';

export type NotificationSeverity = 'critical' | 'warning' | 'info';

export type NotificationStatus = 'new' | 'in_behandeling' | 'resolved';

export interface Notification {
  id: string;
  severity: NotificationSeverity;
  title: string;
  description: string;
  recommendedActions?: string[];
  notificationType: string;
  source: string;
  sourceLabel: string;
  sourceRoute: string;
  status?: NotificationStatus;
  supportsDetails?: boolean;
}

interface NotificationsResponse {
  active: Notification[];
  resolved: Notification[];
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  readonly #http = inject(HttpClient);
  readonly #API_URL = RouteService.getBackendUrl('/notifications');
  readonly #userService = inject(UserService);
  readonly #driveService = inject(DriveService);
  readonly #mobileDeviceService = inject(MobileDeviceService);
  readonly #groupService = inject(GroupService);
  readonly #oAuthService = inject(OAuthService);

  getNotificationsAndResolved(): Observable<{ active: Notification[]; resolved: Notification[] }> {
    return this.#http.get<NotificationsResponse>(this.#API_URL, { withCredentials: true }).pipe(
      catchError(() => of({ active: [], resolved: [] }))
    );
  }

  getNotificationDetails(notification: Notification): Observable<string[]> {
    switch (notification.notificationType) {
      case 'user-control':
        return this.#userService.getUsersWithoutTwoFactor().pipe(
          map((r) => r.users.map((u) => `${u.fullName} (${u.email})`)),
          catchError(() => of([]))
        );
      case 'group-external':
        return this.#groupService.getOrgGroups(undefined, undefined, 200).pipe(
          map((r) =>
            r.groups
              .filter((g) => g.externalMembers > 0)
              .map((g) => `${g.name} (${g.externalMembers} externe leden)`)
          ),
          catchError(() => of([]))
        );
      case 'oauth-high-risk':
        return this.#oAuthService.getApps(50, 'high').pipe(
          map((r) => r.apps.map((a) => `${a.name} (${a.totalUsers} gebruikers)`)),
          catchError(() => of([]))
        );
      case 'drive-orphan':
      case 'drive-external':
        return this.#driveService.getDrives(100).pipe(
          map((r) => {
            if (notification.notificationType === 'drive-orphan') {
              return r.drives
                .filter((d) => d.totalOrganizers <= 0)
                .map((d) => d.name);
            }
            return r.drives
              .filter((d) => d.externalMembers > 0)
              .map((d) => `${d.name} (${d.externalMembers} externe leden)`);
          }),
          catchError(() => of([]))
        );
      case 'device-lockscreen':
        return this.#getDevicesByFilter((d) => !d.isScreenLockSecure);
      case 'device-encryption':
        return this.#getDevicesByFilter((d) => !d.isEncryptionSecure);
      case 'device-os':
        return this.#getDevicesByFilter((d) => !d.isOsSecure);
      case 'device-integrity':
        return this.#getDevicesByFilter((d) => !d.isIntegritySecure);
      default:
        return of([]);
    }
  }

  #getDevicesByFilter(
    predicate: (d: {
      resourceId: string;
      deviceName: string;
      userName: string;
      userEmail: string;
      isScreenLockSecure: boolean;
      isEncryptionSecure: boolean;
      isOsSecure: boolean;
      isIntegritySecure: boolean;
    }) => boolean
  ): Observable<string[]> {
    return this.#mobileDeviceService.getDevices(undefined, undefined, undefined, 100).pipe(
      map((r) => {
        const seen = new Set<string>();
        return r.devices
          .filter((d) => {
            if (!predicate(d)) return false;
            if (seen.has(d.resourceId)) return false;
            seen.add(d.resourceId);
            return true;
          })
          .map((d) => `${d.deviceName} – ${d.userName} (${d.userEmail})`);
      }),
      catchError(() => of([]))
    );
  }
}
