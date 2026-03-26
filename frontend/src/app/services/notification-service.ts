import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { RouteService } from './route-service';
import { UserService } from './user-service';
import { DriveService } from './drive-service';
import { DeviceService } from './device-service';
import { GroupService } from './group-service';
import { OAuthService } from './o-auth-service';
import { PasswordSettingsService } from './password-settings-service';
import { Notification } from '../models/notification/Notification';
import { NotificationsResponse } from '../models/notification/NotificationsResponse';
import { Device } from '../models/devices/Device';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  readonly #http = inject(HttpClient);
  readonly #API_URL = RouteService.getBackendUrl('/notifications');
  readonly #userService = inject(UserService);
  readonly #driveService = inject(DriveService);
  readonly #deviceService = inject(DeviceService);
  readonly #groupService = inject(GroupService);
  readonly #oAuthService = inject(OAuthService);
  readonly #passwordSettingsService = inject(PasswordSettingsService);

  getNotificationsAndDismissed(): Observable<{ active: Notification[]; dismissed: Notification[] }> {
    return this.#http
      .get<NotificationsResponse>(this.#API_URL, { withCredentials: true })
      .pipe(catchError(() => of({ active: [], dismissed: [] })));
  }

  getNotificationDetails(notification: Notification): Observable<string[]> {
    switch (notification.notificationType) {
      case 'user-control':
        return this.#userService.getUsersWithoutTwoFactor().pipe(
          map((r) => r.users.map((u) => `${u.fullName} (${u.email})`)),
          catchError(() => of([])),
        );
      case 'group-external':
        return this.#groupService.getOrgGroups(undefined, undefined, 200).pipe(
          map((r) =>
            r.groups
              .filter((g) => g.externalMembers > 0)
              .map((g) => `${g.name} (${g.externalMembers} externe leden)`),
          ),
          catchError(() => of([])),
        );
      case 'oauth-high-risk':
        return this.#oAuthService.getApps(50, 'high').pipe(
          map((r) => r.apps.map((a) => `${a.name} (${a.totalUsers} gebruikers)`)),
          catchError(() => of([])),
        );
      case 'drive-orphan':
      case 'drive-external':
        return this.#driveService.getDrives(100).pipe(
          map((r) => {
            if (notification.notificationType === 'drive-orphan') {
              return r.drives.filter((d) => d.totalOrganizers <= 0).map((d) => d.name);
            }
            return r.drives
              .filter((d) => d.externalMembers > 0)
              .map((d) => `${d.name} (${d.externalMembers} externe leden)`);
          }),
          catchError(() => of([])),
        );
      case 'device-lockscreen':
        return this.#getDevicesByFilter((d) => !d.lockSecure);
      case 'device-encryption':
        return this.#getDevicesByFilter((d) => !d.encSecure);
      case 'device-os':
        return this.#getDevicesByFilter((d) => !d.osSecure);
      case 'device-integrity':
        return this.#getDevicesByFilter((d) => !d.intSecure);
      case 'password-2sv-not-enforced':
      case 'password-weak-length':
      case 'password-strong-not-required':
      case 'password-never-expires':
      case 'password-admins-no-security-keys':
        return this.#getPasswordNotificationDetails(notification.notificationType);
      default:
        return of([]);
    }
  }

  #getDevicesByFilter(predicate: (d: Device) => boolean): Observable<string[]> {
    return this.#deviceService.getDevices(undefined, undefined, undefined, 100).pipe(
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
      catchError(() => of([])),
    );
  }

  #getPasswordNotificationDetails(notificationType: string): Observable<string[]> {
    return this.#passwordSettingsService.getPasswordSettings().pipe(
      map((data) => {
        switch (notificationType) {
          case 'password-2sv-not-enforced':
            return data.twoStepVerification.byOrgUnit
              .filter((ou) => !ou.enforced)
              .map((ou) => `${ou.orgUnitName} (${ou.orgUnitPath}) – ${ou.totalCount} gebruikers`);
          case 'password-weak-length':
            return data.passwordPoliciesByOu
              .filter((p) => p.minLength != null && p.minLength < 12)
              .map((p) => `${p.orgUnitName} (${p.orgUnitPath}) – min. ${p.minLength} tekens`);
          case 'password-strong-not-required':
            return data.passwordPoliciesByOu
              .filter((p) => p.strongPasswordRequired === false)
              .map((p) => `${p.orgUnitName} (${p.orgUnitPath})`);
          case 'password-never-expires':
            return data.passwordPoliciesByOu
              .filter((p) => p.expirationDays == null || p.expirationDays === 0)
              .map((p) => `${p.orgUnitName} (${p.orgUnitPath})`);
          case 'password-admins-no-security-keys':
            return (data.adminsWithoutSecurityKeys ?? []).map(
              (a) => `${a.name} (${a.email})`
            );
          default:
            return [];
        }
      }),
      catchError(() => of([])),
    );
  }
}
