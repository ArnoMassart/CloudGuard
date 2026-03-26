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
import { TranslocoPipe } from '@jsverse/transloco';

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
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);

  readonly preferences = signal<Record<string, boolean>>({});
  readonly dnsImportance = signal<Record<string, string>>({});
  readonly dnsOverrideTypes = signal<ReadonlySet<string>>(new Set());
  readonly loading = signal(true);
  readonly saving = signal<string | null>(null);

  ngOnInit(): void {
    this.#loadPreferences();
  }

  #loadPreferences(): void {
    this.loading.set(true);
    this.#preferencesService.getAllPreferences().subscribe({
      next: (res) => this.#applyPreferencesPayload(res),
      error: () => {
        this.preferences.set({});
        this.dnsImportance.set({});
        this.dnsOverrideTypes.set(new Set());
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
    this.#preferencesService.setPreference(section, key, newValue).subscribe({
      next: () => {
        this.preferences.update((p) => ({ ...p, [fullKey]: newValue }));
        this.#preferencesFacade.refresh();
      },
      error: () => this.#loadPreferences(),
      complete: () => this.saving.set(null),
    });
  }

  setDnsImportance(section: string, prefKey: string, dnsType: string, raw: string): void {
    const fullKey = `${section}:${prefKey}`;
    const previous = this.dnsSelectValue(dnsType);
    if (raw === previous) return;

    this.saving.set(fullKey);
    this.#preferencesService.setPreference(section, prefKey, true, raw).subscribe({
      next: () => {
        this.#preferencesFacade.refresh();
        this.#reloadPrefsQuiet();
      },
      error: () => this.#loadPreferences(),
      complete: () => this.saving.set(null),
    });
  }
}
