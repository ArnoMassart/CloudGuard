import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { PageHeader } from '../../../components/page-header/page-header';
import { PasswordSettingsService } from '../../../services/password-settings-service';
import { OuPasswordPolicy, PasswordSettings as PasswordSettingsData } from '../../../models/password/PasswordSettings';
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

  get2SvPercentage(enrolled: number, total: number): number {
    if (total === 0) return 0;
    return Math.round((enrolled / total) * 100);
  }

  toggleOu(policy: string): void {
    this.expandedOu.update((v) => (v === policy ? null : policy));
  }

  openAdminPasswordSettings(): void {
    UtilityMethods.openAdminPage('https://admin.google.com/ac/security/password');
  }
}
