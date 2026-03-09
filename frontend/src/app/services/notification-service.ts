import { inject, Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { map, switchMap, catchError } from 'rxjs/operators';
import { DnsService, DnsRecordResponse } from './dns-service';
import { DomainService } from './domain-service';
import { UserService } from './user-service';
import { DriveService } from './drive-service';
import { MobileDeviceService } from './mobile-device-service';
import { AppPasswordsService } from './app-password-service';
import { GroupService } from './group-service';
import { OAuthService } from './o-auth-service';

export type NotificationSeverity = 'critical' | 'warning' | 'info';

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
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  readonly #domainService = inject(DomainService);
  readonly #dnsService = inject(DnsService);
  readonly #userService = inject(UserService);
  readonly #driveService = inject(DriveService);
  readonly #mobileDeviceService = inject(MobileDeviceService);
  readonly #appPasswordsService = inject(AppPasswordsService);
  readonly #groupService = inject(GroupService);
  readonly #oAuthService = inject(OAuthService);

  getNotifications(): Observable<Notification[]> {
    return this.#domainService.getDomains().pipe(
      switchMap((domains) => {
        const primary = domains.find((d) => d.domainType === 'Primary Domain');
        const primaryDomain = primary?.domainName;

        const dns$ = primaryDomain
          ? this.#dnsService.getDnsRecords(primaryDomain).pipe(
              catchError(() => of<DnsRecordResponse>({ domain: primaryDomain, rows: [] }))
            )
          : of<DnsRecordResponse>({ domain: '', rows: [] });

        return forkJoin({
          dns: dns$,
          users: this.#userService.getUsersPageOverview().pipe(catchError(() => of(null))),
          drives: this.#driveService.getDrivesPageOverview().pipe(catchError(() => of(null))),
          devices: this.#mobileDeviceService.getMobileDevicesPageOverview().pipe(catchError(() => of(null))),
          appPasswords: this.#appPasswordsService.getOverview().pipe(catchError(() => of(null))),
          groups: this.#groupService.getGroupsOverview().pipe(catchError(() => of(null))),
          oAuth: this.#oAuthService.getOAuthPageOverview().pipe(catchError(() => of(null))),
        });
      }),
      map((data) => this.#aggregateNotifications(data))
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
    predicate: (d: { resourceId: string; deviceName: string; userName: string; userEmail: string; isScreenLockSecure: boolean; isEncryptionSecure: boolean; isOsSecure: boolean; isIntegritySecure: boolean }) => boolean
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

  #aggregateNotifications(data: {
    dns: DnsRecordResponse;
    users: { withoutTwoFactor: number; activeLongNoLoginCount: number; inactiveRecentLoginCount: number } | null;
    drives: {
      orphanDrives: number;
      notOnlyDomainUsersAllowedCount: number;
      notOnlyMembersCanAccessCount: number;
      externalMembersDriveCount: number;
    } | null;
    devices: {
      lockScreenCount: number;
      encryptionCount: number;
      osVersionCount: number;
      integrityCount: number;
    } | null;
    appPasswords: { totalAppPasswords: number; allowed: boolean } | null;
    groups: { groupsWithExternal: number } | null;
    oAuth: { totalHighRiskApps: number } | null;
  }): Notification[] {
    const notifications: Notification[] = [];
    let id = 0;

    const add = (n: Omit<Notification, 'id'>) => {
      notifications.push({ ...n, id: `n-${++id}` });
    };

    const dnsTitles: Record<string, string> = {
      SPF: 'SPF Record',
      DKIM: 'DKIM',
      DMARC: 'DMARC',
      MX: 'MX Records',
      DNSSEC: 'DNSSEC',
      CAA: 'CAA Records',
    };
    const criticalDnsTypes = new Set<string>();
    const attentionDnsTypes = new Set<string>();
    for (const row of data.dns.rows) {
      if (row.status === 'ACTION_REQUIRED' || row.status === 'ERROR') {
        criticalDnsTypes.add(dnsTitles[row.type] ?? row.type);
      } else if (
        row.status === 'ATTENTION' &&
        (row.importance === 'REQUIRED' || row.importance === 'RECOMMENDED')
      ) {
        attentionDnsTypes.add(dnsTitles[row.type] ?? row.type);
      }
    }
    if (criticalDnsTypes.size > 0) {
      const typesList = Array.from(criticalDnsTypes).join(', ');
      add({
        severity: 'critical',
        title: 'DNS records ontbreken of niet correct',
        description: `${typesList} ${criticalDnsTypes.size === 1 ? 'ontbreekt' : 'ontbreken'} of ${criticalDnsTypes.size === 1 ? 'is niet' : 'zijn niet'} correct geconfigureerd.`,
        recommendedActions: ['Controleer en configureer alle DNS records via je DNS provider'],
        notificationType: 'dns-critical',
        source: 'domain-dns',
        sourceLabel: 'Domein & DNS',
        sourceRoute: '/domain-dns',
      });
    }
    if (attentionDnsTypes.size > 0) {
      const typesList = Array.from(attentionDnsTypes).join(', ');
      add({
        severity: 'warning',
        title: 'DNS records vereisen aandacht',
        description: `${typesList} ${attentionDnsTypes.size === 1 ? 'kan' : 'kunnen'} worden verbeterd.`,
        recommendedActions: ['Controleer de DNS configuratie via je DNS provider'],
        notificationType: 'dns-attention',
        source: 'domain-dns',
        sourceLabel: 'Domein & DNS',
        sourceRoute: '/domain-dns',
      });
    }

    if (data.users) {
      if (data.users.withoutTwoFactor > 0) {
        add({
          severity: 'critical',
          title: 'Gebruikers zonder tweestapsverificatie',
          description: `${data.users.withoutTwoFactor} gebruiker(s) hebben geen 2FA ingeschakeld.`,
          recommendedActions: [
            'Schakel 2FA in voor deze gebruikers',
            'Verwijder admin rechten totdat 2FA is ingeschakeld',
          ],
          notificationType: 'user-control',
          source: 'users-groups',
          sourceLabel: 'Gebruikers & Groepen',
          sourceRoute: '/users-groups',
        });
      }
      if (data.users.activeLongNoLoginCount > 0) {
        add({
          severity: 'warning',
          title: 'Actieve gebruikers met lange inactiviteit',
          description: `${data.users.activeLongNoLoginCount} actieve gebruiker(s) hebben lang niet ingelogd.`,
          recommendedActions: ['Controleer of deze accounts nog actief moeten zijn'],
          notificationType: 'user-activity',
          source: 'users-groups',
          sourceLabel: 'Gebruikers & Groepen',
          sourceRoute: '/users-groups',
        });
      }
      if (data.users.inactiveRecentLoginCount > 0) {
        add({
          severity: 'warning',
          title: 'Inactieve gebruikers met recente login',
          description: `${data.users.inactiveRecentLoginCount} inactieve gebruiker(s) hebben recent ingelogd.`,
          recommendedActions: ['Controleer of deze accounts opnieuw geactiveerd moeten worden'],
          notificationType: 'user-activity',
          source: 'users-groups',
          sourceLabel: 'Gebruikers & Groepen',
          sourceRoute: '/users-groups',
        });
      }
    }

    if (data.groups && data.groups.groupsWithExternal > 0) {
      add({
        severity: 'warning',
        title: 'Groepen met externe leden',
        description: `${data.groups.groupsWithExternal} groep(en) hebben externe leden.`,
        recommendedActions: ['Controleer toegangsrechten van externe leden'],
        notificationType: 'group-external',
        source: 'users-groups',
        sourceLabel: 'Gebruikers & Groepen',
        sourceRoute: '/users-groups',
      });
    }

    if (data.drives) {
      if (data.drives.orphanDrives > 0) {
        add({
          severity: 'warning',
          title: 'Gedeelde drives zonder eigenaar',
          description: `${data.drives.orphanDrives} drive(s) hebben geen actieve eigenaar.`,
          recommendedActions: ['Wijs een nieuwe eigenaar toe aan deze drives'],
          notificationType: 'drive-orphan',
          source: 'shared-drives',
          sourceLabel: 'Gedeelde Drives',
          sourceRoute: '/shared-drives',
        });
      }
      if (data.drives.externalMembersDriveCount > 0) {
        add({
          severity: 'warning',
          title: 'Drives met externe leden',
          description: `${data.drives.externalMembersDriveCount} drive(s) hebben externe leden.`,
          recommendedActions: ['Controleer externe toegang tot gedeelde drives'],
          notificationType: 'drive-external',
          source: 'shared-drives',
          sourceLabel: 'Gedeelde Drives',
          sourceRoute: '/shared-drives',
        });
      }
    }

    if (data.devices) {
      if (data.devices.lockScreenCount > 0) {
        add({
          severity: 'warning',
          title: 'Apparaten zonder vergrendelscherm beveiliging',
          description: `${data.devices.lockScreenCount} apparaat(en) hebben geen vergrendelscherm beveiliging.`,
          recommendedActions: ['Vereis lockscreen voor alle apparaten'],
          notificationType: 'device-lockscreen',
          source: 'mobile-devices',
          sourceLabel: 'Mobiele Apparaten',
          sourceRoute: '/mobile-devices',
        });
      }
      if (data.devices.encryptionCount > 0) {
        add({
          severity: 'warning',
          title: 'Apparaten zonder encryptie',
          description: `${data.devices.encryptionCount} apparaat(en) zijn niet versleuteld.`,
          recommendedActions: ['Schakel apparaatencryptie in'],
          notificationType: 'device-encryption',
          source: 'mobile-devices',
          sourceLabel: 'Mobiele Apparaten',
          sourceRoute: '/mobile-devices',
        });
      }
      if (data.devices.osVersionCount > 0) {
        add({
          severity: 'warning',
          title: 'Apparaten met verouderde OS versie',
          description: `${data.devices.osVersionCount} apparaat(en) hebben een verouderde besturingssysteemversie.`,
          recommendedActions: ['Vereis minimale OS-versie voor alle apparaten'],
          notificationType: 'device-os',
          source: 'mobile-devices',
          sourceLabel: 'Mobiele Apparaten',
          sourceRoute: '/mobile-devices',
        });
      }
      if (data.devices.integrityCount > 0) {
        add({
          severity: 'warning',
          title: 'Apparaten met integriteitsproblemen',
          description: `${data.devices.integrityCount} apparaat(en) hebben integriteitsproblemen (root/jailbreak).`,
          recommendedActions: ['Blokkeer geroote of gejailbroken apparaten'],
          notificationType: 'device-integrity',
          source: 'mobile-devices',
          sourceLabel: 'Mobiele Apparaten',
          sourceRoute: '/mobile-devices',
        });
      }
    }

    if (data.oAuth && data.oAuth.totalHighRiskApps > 0) {
      add({
        severity: 'critical',
        title: 'Third-party applicaties met te veel toegang',
        description: `${data.oAuth.totalHighRiskApps} applicatie(s) hebben toegang tot gevoelige gegevens of admin-functies.`,
        recommendedActions: [
          'Controleer de toegangsrechten voor deze applicaties',
          'Herroep toegang voor apps die niet meer nodig zijn',
        ],
        notificationType: 'oauth-high-risk',
        source: 'app-access',
        sourceLabel: 'App Toegang',
        sourceRoute: '/app-access',
      });
    }

    if (data.appPasswords) {
      if (data.appPasswords.allowed && data.appPasswords.totalAppPasswords > 0) {
        add({
          severity: 'critical',
          title: 'App-wachtwoorden actief',
          description: `${data.appPasswords.totalAppPasswords} app-wachtwoord(en) actief. App-wachtwoorden omzeilen 2FA.`,
          recommendedActions: ['Overweeg OAuth-gebaseerde authenticatie waar mogelijk'],
          notificationType: 'app-password',
          source: 'app-passwords',
          sourceLabel: 'App-wachtwoorden',
          sourceRoute: '/app-passwords',
        });
      }
    }

    const seen = new Set<string>();
    return notifications.filter((n) => {
      const key = `${n.source}-${n.notificationType}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }
}
