import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { PageHeader } from '../../../components/page-header/page-header';
import { PasswordSettingsService } from '../../../services/password-settings-service';
import { AdminSecurityKeysService } from '../../../services/admin-security-keys-service';
import { OrgUnit2Sv, OuPasswordPolicy, PasswordSettings as PasswordSettingsData } from '../../../models/password/PasswordSettings';
import { AdminWithSecurityKey } from '../../../models/admin-security-keys/AdminWithSecurityKey';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-password-settings',
  imports: [
    CommonModule,
    PageHeader,
    SectionTopCard,
    LucideAngularModule,
  ],
  templateUrl: './password-settings.html',
})
export class PasswordSettings implements OnInit {
  readonly Icons = AppIcons;
  readonly #passwordSettingsService = inject(PasswordSettingsService);
  readonly #adminSecurityKeysService = inject(AdminSecurityKeysService);

  readonly data = signal<PasswordSettingsData | null>(null);
  readonly adminsWithSecurityKeys = signal<AdminWithSecurityKey[]>([]);
  readonly adminsLoading = signal(true);
  readonly adminsError = signal<string | null>(null);
  readonly adminsWarning = signal<string | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly expandedOu = signal<string | null>(null);
  readonly isRefreshing = signal(false);

  ngOnInit(): void {
    this.#load();
    this.#loadAdminsWithSecurityKeys();
  }

  #load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.#passwordSettingsService.getPasswordSettings().subscribe({
      next: (settings: PasswordSettingsData) => {
        this.data.set(settings);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message || 'Kon gegevens niet laden.');
        this.loading.set(false);
      },
    });
  }

  #loadAdminsWithSecurityKeys(): void {
    this.adminsLoading.set(true);
    this.adminsError.set(null);
    this.adminsWarning.set(null);
    this.#adminSecurityKeysService.getAdminsWithSecurityKeys().subscribe({
      next: (response) => {
        this.adminsWithSecurityKeys.set(response.admins ?? []);
        this.adminsWarning.set(response.errorMessage ?? null);
        this.adminsLoading.set(false);
      },
      error: (err) => {
        this.adminsError.set(err?.message || 'Kon admins zonder security keys niet laden.');
        this.adminsLoading.set(false);
      },
    });
  }

  retry(): void {
    this.#load();
  }

  refreshData(): void {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#passwordSettingsService.refreshCache().subscribe({
      next: () => {
        let pending = 2;
        const onDone = () => {
          pending--;
          if (pending === 0) this.isRefreshing.set(false);
        };
        this.#passwordSettingsService.getPasswordSettings().subscribe({
          next: (settings) => {
            this.data.set(settings);
            onDone();
          },
          error: () => onDone(),
        });
        this.#adminSecurityKeysService.getAdminsWithSecurityKeys().subscribe({
          next: (response) => {
            this.adminsWithSecurityKeys.set(response.admins ?? []);
            this.adminsWarning.set(response.errorMessage ?? null);
            this.adminsError.set(null);
            onDone();
          },
          error: () => onDone(),
        });
      },
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
    });
  }

  retryAdmins(): void {
    this.#loadAdminsWithSecurityKeys();
  }

  get2SvPercentage(enrolled: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((enrolled / total) * 100);
  }

  toggleOu(policy: string): void {
    this.expandedOu.update((v) => (v === policy ? null : policy));
  }

  get2SvForOu(orgUnitPath: string): OrgUnit2Sv | undefined {
    return this.data()?.twoStepVerification.byOrgUnit.find((ou) => ou.orgUnitPath === orgUnitPath);
  }

  hasPolicyData(policy: OuPasswordPolicy): boolean {
    return policy.minLength != null || policy.expirationDays != null ||
      policy.strongPasswordRequired != null || policy.reusePreventionCount != null;
  }

  openAdminPasswordSettings(): void {
    UtilityMethods.openAdminPage('https://admin.google.com/ac/security/password');
  }

  getAdminUserUrl(admin: AdminWithSecurityKey): string {
    const id = admin.id?.trim();
    if (!id) return 'https://admin.google.com/u/1/ac/users';
    return `https://admin.google.com/u/1/ac/users/${encodeURIComponent(id)}`;
  }

  formatOrgUnit(path: string): string {
    if (!path || path === '/') return 'Hoofdorganisatie';
    return path.startsWith('/') ? path.slice(1) : path;
  }
}
