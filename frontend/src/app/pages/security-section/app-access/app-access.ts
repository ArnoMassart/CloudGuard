import { Component, inject, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { OAuthService } from '../../../services/o-auth-service';
import { AggregatedAppDto } from '../../../models/o-auth/AggregatedAppDto';
import { OAuthOverviewResponse } from '../../../models/o-auth/OAuthOverviewResponse';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-app-access',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, FormsModule],
  templateUrl: './app-access.html',
  styleUrl: './app-access.css',
})
export class AppAccess {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #oAuthService = inject(OAuthService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  apps = signal<AggregatedAppDto[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly searchQuery = signal('');
  readonly pageOverview = signal<OAuthOverviewResponse | null>(null);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);

  readonly expandedApp = signal<string | null>(null);

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  #tokenHistory: (string | null)[] = [null];
  #searchSubject = new Subject<string>();

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((value) => {
      this.onSearch(value);
    });

    this.#loadPageOverview();
    this.#loadApps();
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  onKeyup(value: string) {
    this.#searchSubject.next(value);
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.#tokenHistory = [null];
    this.#loadApps(null);
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadApps(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory[this.#tokenHistory.length - 1];
      this.currentPage.update((p) => p - 1);
      this.#loadApps(prevToken);
    }
  }

  toggleExpand(deviceId: string) {
    if (this.expandedApp() === deviceId) {
      this.expandedApp.set(null);
    } else {
      this.expandedApp.set(deviceId);
    }
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#oAuthService.refreshOAuthCache().subscribe({
      next: (res) => {
        this.currentPage.set(1);
        this.#tokenHistory = [null];

        this.#loadApps(null);
        this.#loadPageOverview();
      },
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
      complete: () => {
        // Stop de spinner zodra alles klaar is
        this.isRefreshing.set(false);
      },
    });
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadApps(token: string | null = null) {
    this.isLoading.set(true);

    this.#oAuthService.getApps(ITEMS_PER_PAGE, token || undefined, this.searchQuery()).subscribe({
      next: (res) => {
        this.apps.set(res.apps);
        this.nextPageToken.set(res.nextPageToken);
        this.isLoading.set(false);
        console.log(this.apps());
        console.log(this.nextPageToken());
      },
      error: (err) => {
        console.error('Failed to load oAuth apps', err);
        this.isLoading.set(false);
      },
    });
  }

  #loadPageOverview() {
    this.#oAuthService.getOAuthPageOverview().subscribe({
      next: (res) => {
        this.pageOverview.set(res);
      },
      error: (err) => {
        console.error('Failed to load page overview', err);
      },
    });
  }
}
