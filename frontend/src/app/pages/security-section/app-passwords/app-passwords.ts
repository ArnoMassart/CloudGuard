import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AppPasswordsService } from '../../../services/app-password-service';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { PageHeader } from '../../../components/page-header/page-header';
import { AppPassword, AppPasswordOverviewResponse, UserAppPasswords } from '../../../services/app-password-service';
import { LucideAngularModule } from 'lucide-angular';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

const ITEMS_PER_PAGE = 10;

@Component({
  selector: 'app-app-passwords',
  imports: [SectionTopCard, CommonModule, PageHeader, LucideAngularModule],
  templateUrl: './app-passwords.html',
  styleUrl: './app-passwords.css',
})
export class AppPasswords implements OnInit {
  readonly Icons = AppIcons;
  readonly pageOverview = signal<AppPasswordOverviewResponse | null>(null);
  readonly #appPasswordsService = inject(AppPasswordsService);
  readonly userAppPasswords = signal<UserAppPasswords[]>([]);
  readonly expandedAppPassword = signal<string | null>(null);
  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal(false);
  readonly loadError = signal(false);
  readonly searchQuery = signal('');
  readonly filteredUserAppPasswords = computed(() => {
    const users = this.userAppPasswords();
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return users;
    return users.filter(
      (u) =>
        (u.name?.toLowerCase().includes(q)) ||
        (u.email?.toLowerCase().includes(q))
    );
  });
  #tokenHistory: (string | null)[] = [null];
  #searchSubject = new Subject<string>();

  ngOnInit(): void {
    this.#searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((value) => {
      this.onSearch(value);
    });
    this.#loadOverview();
    this.#loadAppPasswords(null);
  }

  onKeyup(value: string) {
    this.#searchSubject.next(value);
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.#tokenHistory = [null];
    this.#loadAppPasswords(null);
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadAppPasswords(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory[this.#tokenHistory.length - 1];
      this.currentPage.update((p) => p - 1);
      this.#loadAppPasswords(prevToken);
    }
  }

  toggleExpand(email: string) {
    if (this.expandedAppPassword() === email) {
      this.expandedAppPassword.set(null);
    } else {
      this.expandedAppPassword.set(email);
    }
  }

  getAdminUserUrl(user: UserAppPasswords): string {
    const id = user.id?.trim();
    if (!id) return 'https://admin.google.com/u/1/ac/users';
    return `https://admin.google.com/u/1/ac/users/${encodeURIComponent(id)}`;
  }

  retryLoad() {
    this.#tokenHistory = [null];
    this.currentPage.set(1);
    this.#loadAppPasswords(null);
  }

  refreshData() {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#appPasswordsService.refreshCache().subscribe({
      next: () => {
        this.#loadOverview();
        this.#tokenHistory = [null];
        this.currentPage.set(1);
        this.#loadAppPasswords(null);
      },
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
      complete: () => {
        this.isRefreshing.set(false);
      },
    });
  }

  #loadOverview() {
    this.#appPasswordsService.getOverview().subscribe({
      next: (overview) => this.pageOverview.set(overview),
      error: () => {},
    });
  }

  #loadAppPasswords(pageToken: string | null) {
    this.isLoading.set(true);
    this.loadError.set(false);
    this.expandedAppPassword.set(null);
    this.#appPasswordsService.getAppPasswords(ITEMS_PER_PAGE, pageToken ?? undefined, this.searchQuery()).subscribe({
      next: (response) => {
        const data = response.users.length > 0
          ? response.users.map((u) => this.#mapToUserAppPasswords(u))
          : this.#getDemoData();
        this.userAppPasswords.set(data);
        this.nextPageToken.set(response.users.length > 0 ? (response.nextPageToken ?? null) : null);
        this.isLoading.set(false);
      },
      error: () => {
        const demoData = this.#getDemoData();
        this.userAppPasswords.set(demoData);
        this.nextPageToken.set(null);
        this.loadError.set(false);
        this.isLoading.set(false);
        if (this.pageOverview() === null) {
          this.#setDemoOverview(demoData);
        }
      },
    });
  }

  #setDemoOverview(users: UserAppPasswords[]) {
    const totalAppPasswords = users.reduce((sum, u) => sum + u.appPasswords.length, 0);
    const usersWithAppPasswords = users.filter((u) => u.appPasswords.length > 0).length;
    this.pageOverview.set({
      allowed: true,
      totalAppPasswords,
      totalHighRiskAppPasswords: totalAppPasswords,
      securityScore: usersWithAppPasswords === 0 ? 100 : Math.max(0, 100 - usersWithAppPasswords * 10),
    });
  }

  #getDemoData(): UserAppPasswords[] {
    const now = new Date();
    const daysAgo = (d: number) => new Date(now.getTime() - d * 24 * 60 * 60 * 1000);
    const ts = (d: Date) => String(d.getTime());
    return [
      { id: 'demo-1', name: 'Pieter de Vries', email: 'pieter.devries@bedrijf.nl', role: 'User', twoFactorEnabled: false, appPasswords: [{ codeId: 101, name: 'Outlook Desktop', creationTime: ts(daysAgo(350)), lastTimeUsed: ts(daysAgo(27)) }] },
      { id: 'demo-2', name: 'Thomas Mulder', email: 'thomas.mulder@bedrijf.nl', role: 'User', twoFactorEnabled: true, appPasswords: [{ codeId: 102, name: 'Thunderbird', creationTime: ts(daysAgo(200)), lastTimeUsed: ts(daysAgo(3)) }] },
      { id: 'demo-3', name: 'Lisa van Berg', email: 'lisa.vanberg@bedrijf.nl', role: 'Admin', twoFactorEnabled: true, appPasswords: [{ codeId: 103, name: 'Apple Mail', creationTime: ts(daysAgo(180)), lastTimeUsed: ts(daysAgo(1)) }, { codeId: 104, name: 'Outlook Desktop', creationTime: ts(daysAgo(90)), lastTimeUsed: null }] },
      { id: 'demo-4', name: 'Jan Bakker', email: 'jan.bakker@bedrijf.nl', role: 'User', twoFactorEnabled: true, appPasswords: [{ codeId: 105, name: 'Google Calendar Sync', creationTime: ts(daysAgo(60)), lastTimeUsed: ts(daysAgo(14)) }] },
      { id: 'demo-5', name: 'Sophie Jansen', email: 'sophie.jansen@bedrijf.nl', role: 'User', twoFactorEnabled: false, appPasswords: [{ codeId: 106, name: 'Thunderbird', creationTime: ts(daysAgo(400)), lastTimeUsed: ts(daysAgo(45)) }, { codeId: 107, name: 'Outlook Mobile', creationTime: ts(daysAgo(120)), lastTimeUsed: ts(daysAgo(0)) }] },
    ];
  }

  #mapToUserAppPasswords(u: { id: string; name: string; email: string; role: string; tsv: boolean; passwords: AppPassword[] }): UserAppPasswords {
    return {
      id: u.id ?? u.email,
      name: u.name,
      email: u.email,
      role: u.role,
      twoFactorEnabled: u.tsv,
      appPasswords: u.passwords,
    };
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
