import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { PageHeader } from '../../../components/page-header/page-header';
import { PasswordSettingsService } from '../../../services/password-settings-service';
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

  readonly data = signal<PasswordSettingsData | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly expandedOu = signal<string | null>(null);
  readonly securityKeysExpanded = signal(true);
  readonly forcedChangeExpanded = signal(true);
  readonly isRefreshing = signal(false);

  toggleSecurityKeys(): void {
    this.securityKeysExpanded.update((v) => !v);
  }

  toggleForcedChange(): void {
    this.forcedChangeExpanded.update((v) => !v);
  }

  ngOnInit(): void {
    this.#load();
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

  retry(): void {
    this.#load();
  }

  refreshData(): void {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#passwordSettingsService.refreshCache().subscribe({
      next: () => this.#load(),
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
      complete: () => this.isRefreshing.set(false),
    });
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

  getUserUrl(email: string): string {
    if (!email?.trim()) return 'https://admin.google.com/u/1/ac/users';
    return `https://admin.google.com/u/1/ac/users/${encodeURIComponent(email.trim())}`;
  }

  formatForcedChangeReason(reason: string): string {
    if (reason === 'changePasswordAtNextLogin') return 'Verplicht bij volgende login';
    return reason || 'Onbekend';
  }
}
