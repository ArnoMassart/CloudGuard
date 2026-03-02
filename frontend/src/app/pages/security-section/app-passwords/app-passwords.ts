import { Component, inject, OnInit, signal } from '@angular/core';
import { AppPasswordsService } from '../../../services/app-password-service';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { PageHeader } from '../../../components/page-header/page-header';
import {AppPassword, AppPasswordOverviewResponse, UserAppPasswords} from "../../../services/app-password-service";
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-app-passwords',
  imports: [SectionTopCard, CommonModule, PageHeader, LucideAngularModule],
  templateUrl: './app-passwords.html',
  styleUrl: './app-passwords.css',
})
export class AppPasswords implements OnInit{
  readonly Icons = AppIcons;
  readonly pageOverview = signal<AppPasswordOverviewResponse|null>(null);
  readonly #appPasswordsService = inject(AppPasswordsService);
  readonly userAppPasswords = signal<UserAppPasswords[]>([]);
  readonly isExpanded = signal(true);
  readonly expandedAppPassword = signal<string|null>(null);

  ngOnInit(): void{
    this.#loadAppPasswords();
  }

  toggleExpanded(appPasswordId: string) {
    this.isExpanded.update((v) => !v);
  }

  toggleExpand(appPasswordId: string) {
    if (this.expandedAppPassword() === appPasswordId) {
      this.expandedAppPassword.set(null);
    } else {
      this.expandedAppPassword.set(appPasswordId);
    }
  }

  #loadAppPasswords() {
    this.#appPasswordsService.getOverview().subscribe({
      next: (overview) => this.pageOverview.set(overview),
      error: () => {},
    });
    this.#appPasswordsService.getAppPasswords().subscribe({
      next: (response) => {
        const data = response.length > 0
          ? response.map((u) => this.#mapToUserAppPasswords(u))
          : this.#getDemoData();
        this.userAppPasswords.set(data);
      },
      error: (_err) => {
        const demoData = this.#getDemoData();
        this.userAppPasswords.set(demoData);
        this.#setDemoOverview(demoData);
      },
    });
  }

  #setDemoOverview(users: UserAppPasswords[]) {
    const totalAppPasswords = users.reduce((sum, u) => sum + u.appPasswords.length, 0);
    const usersWithAppPasswords = users.length;
    this.pageOverview.set({
      allowed: true,
      totalAppPasswords,
      totalHighRiskAppPasswords: totalAppPasswords,
      securityScore: usersWithAppPasswords === 0 ? 100 : Math.max(0, 100 - usersWithAppPasswords * 10),
    });
  }

  #mapToUserAppPasswords(u: { name: string; email: string; role: string; tsv: boolean; passwords: AppPassword[] }): UserAppPasswords {
    return {
      name: u.name,
      email: u.email,
      role: u.role,
      twoFactorEnabled: u.tsv,
      appPasswords: u.passwords,
    };
  }

  #getDemoData(): UserAppPasswords[] {
    const now = new Date();
    const daysAgo = (d: number) => new Date(now.getTime() - d * 24 * 60 * 60 * 1000);
    const ts = (d: Date) => String(d.getTime());
    return [
      { name: 'Pieter de Vries', email: 'pieter.devries@bedrijf.nl', role: 'User', twoFactorEnabled: false, appPasswords: [{ codeId: 101, name: 'Outlook Desktop', creationTime: ts(daysAgo(350)), lastTimeUsed: ts(daysAgo(27)) }] },
      { name: 'Thomas Mulder', email: 'thomas.mulder@bedrijf.nl', role: 'User', twoFactorEnabled: true, appPasswords: [{ codeId: 102, name: 'Thunderbird', creationTime: ts(daysAgo(200)), lastTimeUsed: ts(daysAgo(3)) }] },
      { name: 'Lisa van Berg', email: 'lisa.vanberg@bedrijf.nl', role: 'Admin', twoFactorEnabled: true, appPasswords: [{ codeId: 103, name: 'Apple Mail', creationTime: ts(daysAgo(180)), lastTimeUsed: ts(daysAgo(1)) }, { codeId: 104, name: 'Outlook Desktop', creationTime: ts(daysAgo(90)), lastTimeUsed: null }] },
      { name: 'Jan Bakker', email: 'jan.bakker@bedrijf.nl', role: 'User', twoFactorEnabled: true, appPasswords: [{ codeId: 105, name: 'Google Calendar Sync', creationTime: ts(daysAgo(60)), lastTimeUsed: ts(daysAgo(14)) }] },
      { name: 'Sophie Jansen', email: 'sophie.jansen@bedrijf.nl', role: 'User', twoFactorEnabled: false, appPasswords: [{ codeId: 106, name: 'Thunderbird', creationTime: ts(daysAgo(400)), lastTimeUsed: ts(daysAgo(45)) }, { codeId: 107, name: 'Outlook Mobile', creationTime: ts(daysAgo(120)), lastTimeUsed: ts(daysAgo(0)) }] },
    ];
  }

  formatDate(value: Date | string | null): string {
    if (!value) return '–';
    const d = typeof value === 'string' ? new Date(Number(value) || value) : value;
    if (isNaN(d.getTime())) return '–';
    return d.toLocaleDateString('nl-NL', { day: 'numeric', month: 'numeric', year: 'numeric' });
  }

  formatLastUsed(value: Date | string | null): string {
    if (!value) return 'nooit';
    const d = typeof value === 'string' ? new Date(Number(value) || value) : value;
    if (isNaN(d.getTime())) return 'nooit';
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'vandaag';
    if (diffDays === 1) return 'gisteren';
    if (diffDays < 7) return `${diffDays} dagen geleden`;
    if (diffDays < 31) return `${Math.floor(diffDays / 7)} weken geleden`;
    return `${Math.floor(diffDays / 31)} maanden geleden`;
  }

}
