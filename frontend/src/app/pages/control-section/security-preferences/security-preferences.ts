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
import {
  SECTION_PREFERENCE_CONFIGS,
  type SectionPreferenceConfig,
} from '../../../models/preferences/section-preference-config';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MatDialog } from '@angular/material/dialog';
import { MessageDialog } from '../../../components/message-dialog/message-dialog';

@Component({
  selector: 'app-security-preferences',
  standalone: true,
  imports: [CommonModule, PageHeader, PageWarnings, PageWarningsItem, LucideAngularModule, TranslocoPipe],
  templateUrl: './security-preferences.html',
  styleUrl: './security-preferences.css',
})
export class SecurityPreferences implements OnInit {
  readonly sections = SECTION_PREFERENCE_CONFIGS;
  readonly Icons = AppIcons;

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
  readonly #dialog = inject(MatDialog);
  readonly preferencesFacade = inject(SecurityPreferencesFacade);

  readonly preferences = signal<Record<string, boolean>>({});
  readonly dnsImportance = signal<Record<string, string>>({});
  readonly dnsOverrideTypes = signal<ReadonlySet<string>>(new Set());
  readonly loading = signal(true);
  readonly saving = signal<string | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);

  /** Per-section accordion; omitted or true means expanded */
  readonly expandedSections = signal<Record<string, boolean>>({});

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

  /** HTTP 409: preference writes blocked until workspace organization is linked. */
  #handlePreferenceSaveError(err: unknown): void {
    const detail = this.#httpErrorDetail(err);
    if (err instanceof HttpErrorResponse && err.status === 409) {
      this.#dialog.open(MessageDialog, {
        width: 'min(420px, calc(100vw - 2rem))',
        data: {
          titleKey: 'preferences.dialog.organization_required_title',
          message: detail || this.#transloco.translate('preferences.error.save'),
        },
      });
      return;
    }
    this.saveError.set(detail || this.#transloco.translate('preferences.error.save'));
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

  sectionExpanded(sectionId: string): boolean {
    return this.expandedSections()[sectionId] ?? false;
  }

  toggleSection(sectionId: string): void {
    this.expandedSections.update((m) => {
      const cur = m[sectionId] ?? false;
      return { ...m, [sectionId]: !cur };
    });
  }

  sectionStats(section: SectionPreferenceConfig): { active: number; total: number } {
    const total = section.preferences.length;
    let active = 0;
    for (const pref of section.preferences) {
      if (this.isEnabled(section.section, pref.key)) active++;
    }
    return { active, total };
  }

  sectionBadgeLabel(section: SectionPreferenceConfig): string {
    const { active, total } = this.sectionStats(section);
    if (active === total) {
      return this.#transloco.translate('preferences.section.badge.all_active');
    }
    return this.#transloco.translate('preferences.section.badge.partial', { active, total });
  }

  sectionBadgeClass(section: SectionPreferenceConfig): string {
    const { active, total } = this.sectionStats(section);
    if (active === total) {
      return 'border border-emerald-100 bg-emerald-50 text-emerald-800';
    }
    return 'border border-amber-100 bg-amber-50 text-amber-900';
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
        this.#handlePreferenceSaveError(err);
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
        this.#handlePreferenceSaveError(err);
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
