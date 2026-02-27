import { Component, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 3;

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

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  // drives = signal<SharedDrive[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly searchQuery = signal('');
  // readonly pageOverview = signal<SharedDriveOverviewResponse | null>(null);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);

  readonly expandedApp = signal<string | null>(null);

  apps = [
    {
      id: 1,
      name: 'Slack Workspace Integration',
      developer: 'Slack Technologies, Inc.',
      verified: true,
      highRisk: false,
      totalUsers: 24,
      firstDetection: '15-11-2025',
      lastUsed: '17 dagen geleden',
      dataAccess: [
        { name: 'Google Drive', rights: 'Lezen', risk: false },
        { name: 'Gmail', rights: 'Berichten verzenden', risk: false },
        { name: 'Google Agenda', rights: 'Lezen', risk: false },
      ],
    },
    {
      id: 2,
      name: 'ProjectManager Pro',
      developer: 'ProjectManager Solutions Ltd.',
      verified: false,
      highRisk: true,
      totalUsers: 8,
      firstDetection: '20-09-2025',
      lastUsed: '18 dagen geleden',
      dataAccess: [
        { name: 'Google Drive', rights: 'Volledige Toegang', risk: true },
        { name: 'Google Contacten', rights: 'Volledige Toegang', risk: true },
        { name: 'Google Agenda', rights: 'Volledige Toegang', risk: true },
      ],
    },
    {
      id: 3,
      name: 'Email Analytics Tool',
      developer: 'Analytics Inc.',
      verified: false,
      highRisk: true,
      totalUsers: 3,
      firstDetection: '10-06-2024',
      lastUsed: '2 maanden geleden',
      dataAccess: [
        { name: 'Gmail', rights: 'Volledige Toegang', risk: true },
        { name: 'Google Groepen', rights: 'Lezen', risk: false },
      ],
    },
    {
      id: 4,
      name: 'Google Workspace Marketplace - DocuSign',
      developer: 'DocuSign, Inc.',
      verified: true,
      highRisk: false,
      totalUsers: 45,
      firstDetection: '20-03-2024',
      lastUsed: '17 dagen geleden',
      dataAccess: [
        { name: 'Google Drive', rights: 'Bestanden uploaden', risk: false },
        { name: 'Gmail', rights: 'Lezen', risk: false },
      ],
    },
  ];

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

  refreshData() {}

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadApps(token: string | null = null) {
    // this.isLoading.set(true);
  }

  #loadPageOverview() {}
}
