import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { PageHeader } from '../../../components/page-header/page-header';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { PasswordSettingsService } from '../../../services/password-settings-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import {
  OrgUnit2Sv,
  OuPasswordPolicy,
  PasswordSettings as PasswordSettingsData,
} from '../../../models/password/PasswordSettings';
import { AdminWithSecurityKey } from '../../../models/admin-security-keys/AdminWithSecurityKey';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { LucideAngularModule } from 'lucide-angular';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { KPI_COLORS } from '../../../shared/KpiColors';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-password-settings',
  imports: [
    CommonModule,
    PageHeader,
    SectionTopCard,
    PageWarnings,
    PageWarningsItem,
    LucideAngularModule,
    TranslocoPipe,
  ],
  templateUrl: './password-settings.html',
})
export class PasswordSettings implements OnInit, OnDestroy {
  readonly Icons = AppIcons;
  readonly #passwordSettingsService = inject(PasswordSettingsService);
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly #translocoService = inject(TranslocoService);

  readonly data = signal<PasswordSettingsData | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly expandedOu = signal<string | null>(null);
  readonly securityKeysExpanded = signal(true);
  readonly forcedChangeExpanded = signal(true);
  readonly isRefreshing = signal(false);
  readonly warningsExpanded = signal(true);
  readonly criticalWarningsExpanded = signal(true);

  readonly hasAdminsWithoutSecurityKeys = computed(() =>
    (this.data()?.adminsWithoutSecurityKeys.length ?? 0) > 0 &&
    !this.#preferencesFacade.isDisabled('password-settings', 'adminsSecurityKeys')
  );

  readonly kpiAdminsSecurityKeysColors = computed(() => {
    const n = this.data()?.adminsWithoutSecurityKeys.length ?? 0;
    if (n === 0) return KPI_COLORS.okGreenDark;
    if (this.#preferencesFacade.isDisabled('password-settings', 'adminsSecurityKeys')) return KPI_COLORS.muted;
    return KPI_COLORS.alertOrange;
  });
  readonly hasPasswordLengthWeak = computed(() =>
    !this.#preferencesFacade.isDisabled('password-settings', 'length') &&
    (this.data()?.passwordPoliciesByOu ?? []).some(
      (p) => p.minLength != null && p.minLength < 12
    )
  );
  readonly hasStrongPasswordNotRequired = computed(() =>
    !this.#preferencesFacade.isDisabled('password-settings', 'strongPassword') &&
    (this.data()?.passwordPoliciesByOu ?? []).some(
      (p) => p.strongPasswordRequired === false
    )
  );
  readonly hasPasswordNeverExpires = computed(() =>
    !this.#preferencesFacade.isDisabled('password-settings', 'expiration') &&
    (this.data()?.passwordPoliciesByOu ?? []).some(
      (p) => p.expirationDays == null || p.expirationDays === 0
    )
  );
  readonly has2SvNotEnforced = computed(() =>
    !this.#preferencesFacade.isDisabled('password-settings', '2sv') &&
    (this.data()?.twoStepVerification.byOrgUnit ?? []).some((ou) => !ou.enforced)
  );

  readonly hasWarnings = computed(
    () =>
      this.hasAdminsWithoutSecurityKeys() ||
      this.hasPasswordLengthWeak() ||
      this.hasStrongPasswordNotRequired() ||
      this.hasPasswordNeverExpires()
  );
  readonly hasCriticalWarnings = computed(() => this.has2SvNotEnforced());
  readonly hasMultipleWarnings = computed(
    () =>
      [
        this.hasAdminsWithoutSecurityKeys(),
        this.hasPasswordLengthWeak(),
        this.hasStrongPasswordNotRequired(),
        this.hasPasswordNeverExpires(),
      ].filter(Boolean).length > 1
  );

  #langSubscription?: Subscription;

  isPasswordPrefDisabled(
    key: '2sv' | 'length' | 'strongPassword' | 'expiration' | 'adminsSecurityKeys',
  ): boolean {
    return this.#preferencesFacade.isDisabled('password-settings', key);
  }

  /**
   * Problem count per OU for list styling: same dimensions as warnings (length &lt; 12, no expiration, weak not required, reuse)
   * minus issues the user muted. Reuse has no preference and is always counted.
   */
  effectivePolicyProblemCount(policy: OuPasswordPolicy): number {
    const f = this.#preferencesFacade;
    let p = 0;
    if (!f.isDisabled('password-settings', 'strongPassword') && policy.strongPasswordRequired === false) {
      p++;
    }
    if (!f.isDisabled('password-settings', 'expiration') && (policy.expirationDays == null || policy.expirationDays === 0)) {
      p++;
    }
    if (policy.reusePreventionCount != null && policy.reusePreventionCount === 0) {
      p++;
    }
    if (!f.isDisabled('password-settings', 'length') && policy.minLength != null && policy.minLength < 12) {
      p++;
    }
    return p;
  }

  toggleWarnings(): void {
    this.warningsExpanded.update((v) => !v);
  }

  toggleCriticalWarnings(): void {
    this.criticalWarningsExpanded.update((v) => !v);
  }

  toggleSecurityKeys(): void {
    this.securityKeysExpanded.update((v) => !v);
  }

  toggleForcedChange(): void {
    this.forcedChangeExpanded.update((v) => !v);
  }

  ngOnInit(): void {
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#load();
    });
  }

  ngOnDestroy(): void {
    if (this.#langSubscription) {
      this.#langSubscription.unsubscribe();
    }
  }

  #load(showLoadingScreen = true, onComplete?: () => void): void {
    if (showLoadingScreen) {
      this.loading.set(true);
    }
    this.error.set(null);
    forkJoin({
      settings: this.#passwordSettingsService.getPasswordSettings(),
      _: this.#preferencesFacade.loadDisabled$(),
    }).subscribe({
      next: ({ settings }) => {
        this.data.set(settings);
        this.loading.set(false);
        onComplete?.();
      },
      error: (err) => {
        this.error.set(err?.message || 'Kon gegevens niet laden.');
        this.loading.set(false);
        onComplete?.();
      },
    });
  }

  retry(): void {
    this.#load(true);
  }

  refreshData(): void {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#passwordSettingsService.refreshCache().subscribe({
      next: () => this.#load(false, () => this.isRefreshing.set(false)),
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
    });
  }

  toggleOu(policy: string): void {
    this.expandedOu.update((v) => (v === policy ? null : policy));
  }

  get2SvForOu(orgUnitPath: string): OrgUnit2Sv | undefined {
    return this.data()?.twoStepVerification.byOrgUnit.find((ou) => ou.orgUnitPath === orgUnitPath);
  }

  hasPolicyData(policy: OuPasswordPolicy): boolean {
    return (
      policy.minLength != null ||
      policy.expirationDays != null ||
      policy.strongPasswordRequired != null ||
      policy.reusePreventionCount != null
    );
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
    if (!path || path === '/') return 'password-settings.head-organisation';
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

  openSecurityScoreDetail(): void {
    const settings = this.data();
    const breakdown =
      settings?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(
        settings?.securityScore ?? 0,
        'security-score.password-settings'
      );
    this.#securityScoreDetail.open(breakdown, 'security-score.password-settings');
  }
}
