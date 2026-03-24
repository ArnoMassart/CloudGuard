import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeader } from '../../../components/page-header/page-header';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../../shared/AppIcons';
import { SecurityPreferencesService } from '../../../services/security-preferences-service';
import { SECTION_PREFERENCE_CONFIGS } from '../../../models/preferences/section-preference-config';

@Component({
  selector: 'app-security-preferences',
  standalone: true,
  imports: [CommonModule, PageHeader, LucideAngularModule],
  templateUrl: './security-preferences.html',
  styleUrl: './security-preferences.css',
})
export class SecurityPreferences implements OnInit {
  readonly Icons = AppIcons;
  readonly sections = SECTION_PREFERENCE_CONFIGS;

  readonly #preferencesService = inject(SecurityPreferencesService);

  readonly preferences = signal<Record<string, boolean>>({});
  readonly loading = signal(true);
  readonly saving = signal<string | null>(null);

  ngOnInit(): void {
    this.#loadPreferences();
  }

  #loadPreferences(): void {
    this.loading.set(true);
    this.#preferencesService.getAllPreferences().subscribe({
      next: (prefs) => this.preferences.set(prefs),
      error: () => this.preferences.set({}),
      complete: () => this.loading.set(false),
    });
  }

  isEnabled(section: string, key: string): boolean {
    const fullKey = `${section}:${key}`;
    return this.preferences()[fullKey] !== false;
  }

  toggle(section: string, key: string): void {
    const fullKey = `${section}:${key}`;
    const current = this.preferences()[fullKey] !== false;
    const newValue = !current;

    this.saving.set(fullKey);
    this.#preferencesService.setPreference(section, key, newValue).subscribe({
      next: () => {
        this.preferences.update((p) => ({ ...p, [fullKey]: newValue }));
      },
      error: () => this.#loadPreferences(),
      complete: () => this.saving.set(null),
    });
  }
}
