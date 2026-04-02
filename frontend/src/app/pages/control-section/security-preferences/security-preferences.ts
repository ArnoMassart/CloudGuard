import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeader } from '../../../components/page-header/page-header';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../../shared/AppIcons';
import { SecurityPreferencesService } from '../../../services/security-preferences-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SECTION_PREFERENCE_CONFIGS } from '../../../models/preferences/section-preference-config';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-security-preferences',
  standalone: true,
  imports: [CommonModule, PageHeader, PageWarnings, PageWarningsItem, LucideAngularModule, TranslocoPipe],
  templateUrl: './security-preferences.html',
  styleUrl: './security-preferences.css',
})
export class SecurityPreferences implements OnInit {
  readonly sections = SECTION_PREFERENCE_CONFIGS;

  /** Same icons as `NavItemsSecurity` in the navbar */
  readonly #navIconByRoute: Record<string, typeof AppIcons.Shield> = {
    '/users-groups': AppIcons.Users,
    '/shared-drives': AppIcons.FolderOpen,
    '/devices': AppIcons.SmartPhone,
    '/app-access': AppIcons.Key,
    '/app-passwords': AppIcons.LayoutGrid,
    '/password-settings': AppIcons.Lock,
    '/domain-dns': AppIcons.Globe,
  };

  readonly #preferencesService = inject(SecurityPreferencesService);
  readonly #transloco = inject(TranslocoService);
  readonly preferencesFacade = inject(SecurityPreferencesFacade);

  readonly preferences = signal<Record<string, boolean>>({});
  readonly dnsImportance = signal<Record<string, string>>({});
  readonly dnsOverrideTypes = signal<ReadonlySet<string>>(new Set());
  readonly loading = signal(true);
  readonly saving = signal<string | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);

  ngOnInit(): void {
    this.#loadPreferences();
  }

  retryLoad(): void {
    this.#loadPreferences();
  }

  #loadPreferences(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.#preferencesService.getAllPreferences().subscribe({
      next: (res) => this.#applyPreferencesPayload(res),
      error: (err: unknown) => {
        this.preferences.set({});
        this.dnsImportance.set({});
        this.dnsOverrideTypes.set(new Set());
        this.loadError.set(
          this.#httpErrorDetail(err) || this.#transloco.translate('preferences.error.load'),
        );
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  #reloadPrefsQuiet(): void {
    this.#preferencesService.getAllPreferences().subscribe({
      next: (res) => this.#applyPreferencesPayload(res),
      error: () => {},
    });
  }

  #httpErrorDetail(err: unknown): string {
    if (err instanceof HttpErrorResponse && typeof err.error === 'string' && err.error.trim()) {
      return err.error.trim();
    }
    return '';
  }

  #applyPreferencesPayload(res: {
    preferences?: Record<string, boolean>;
    dnsImportance?: Record<string, string>;
    dnsImportanceOverrideTypes?: string[];
  }): void {
    this.preferences.set(res.preferences ?? {});
    this.dnsImportance.set(res.dnsImportance ?? {});
    this.dnsOverrideTypes.set(new Set(res.dnsImportanceOverrideTypes ?? []));
  }

  isEnabled(section: string, key: string): boolean {
    const fullKey = `${section}:${key}`;
    return this.preferences()[fullKey] !== false;
  }

  sectionNavIcon(route: string): typeof AppIcons.Shield {
    return this.#navIconByRoute[route] ?? AppIcons.Shield;
  }

  dnsSelectValue(dnsType: string | undefined): string {
    if (!dnsType) return '';
    return this.dnsOverrideTypes().has(dnsType) ? (this.dnsImportance()[dnsType] ?? '') : '';
  }

  toggle(section: string, key: string): void {
    const fullKey = `${section}:${key}`;
    const current = this.preferences()[fullKey] !== false;
    const newValue = !current;

    this.saving.set(fullKey);
    this.saveError.set(null);
    this.#preferencesService.setPreference(section, key, newValue).subscribe({
      next: () => {
        this.preferences.update((p) => ({ ...p, [fullKey]: newValue }));
        this.preferencesFacade.refresh();
      },
      error: (err: unknown) => {
        this.saveError.set(
          this.#httpErrorDetail(err) || this.#transloco.translate('preferences.error.save'),
        );
        this.#loadPreferences();
        this.saving.set(null);
      },
      complete: () => this.saving.set(null),
    });
  }

  setDnsImportance(section: string, prefKey: string, dnsType: string, raw: string): void {
    const fullKey = `${section}:${prefKey}`;
    const previous = this.dnsSelectValue(dnsType);
    if (raw === previous) return;

    this.saving.set(fullKey);
    this.saveError.set(null);
    this.#preferencesService.setPreference(section, prefKey, true, raw).subscribe({
      next: () => {
        this.preferencesFacade.refresh();
        this.#reloadPrefsQuiet();
      },
      error: (err: unknown) => {
        this.saveError.set(
          this.#httpErrorDetail(err) || this.#transloco.translate('preferences.error.save'),
        );
        this.#loadPreferences();
        this.saving.set(null);
      },
      complete: () => this.saving.set(null),
    });
  }

  dismissSaveError(): void {
    this.saveError.set(null);
  }

  dismissLoadError(): void {
    this.loadError.set(null);
  }

  dismissSyncWarning(): void {
    this.preferencesFacade.disabledKeysRefreshFailed.set(false);
  }
}
