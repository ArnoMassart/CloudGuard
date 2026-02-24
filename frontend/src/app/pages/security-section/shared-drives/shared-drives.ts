import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { DriveService } from '../../../services/drive-service';
import { SharedDrive } from '../../../models/drives/SharedDrive';
import { SharedDrivesPageWarnings } from '../../../models/drives/SharedDrivesPageWarnings';
import { SharedDriveOverviewResponse } from '../../../models/drives/SharedDriveOverviewResponse';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 2;

@Component({
  selector: 'app-shared-drives',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, FormsModule],
  templateUrl: './shared-drives.html',
  styleUrl: './shared-drives.css',
})
export class SharedDrives implements OnInit {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #driveService = inject(DriveService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  drives = signal<SharedDrive[]>([]);
  isLoading = signal(false);
  searchValue = signal('');
  pageOverview = signal<SharedDriveOverviewResponse | null>(null);

  searchQuery = signal('');
  currentPage = signal(1);
  nextPageToken = signal<string | null>(null);

  hasWarnings = signal(false);
  drivePageWarnings = signal<SharedDrivesPageWarnings>({
    notOnlyDomainUsersAllowedWarning: false,
    notOnlyMembersCanAccessWarning: false,
    externalMembersWarning: false,
    orphanDrivesWarning: false,
  });

  hasMultipleWarnings = computed(() => {
    const warnings = this.drivePageWarnings();
    const activeCount = Object.values(warnings).filter((val) => val === true).length;
    return activeCount > 1;
  });

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
    this.#loadDrives();
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
    this.#tokenHistory = [null]; // Reset historie bij nieuwe zoekopdracht
    this.#loadDrives(null);
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token); // Onthoud dit token om terug te kunnen
      this.currentPage.update((p) => p + 1);
      this.#loadDrives(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop(); // Verwijder huidige token
      const prevToken = this.#tokenHistory[this.#tokenHistory.length - 1]; // Pak de vorige
      this.currentPage.update((p) => p - 1);
      this.#loadDrives(prevToken);
    }
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadDrives(token: string | null = null) {
    this.isLoading.set(true);

    this.#driveService.getDrives(ITEMS_PER_PAGE, token || undefined, this.searchQuery()).subscribe({
      next: (res) => {
        const mappedDrives = (res.drives || []).map((d) => ({ ...d, isLoadingDetails: true }));

        this.drives.set(mappedDrives);
        this.nextPageToken.set(res.nextPageToken);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load shared drives', err);
        this.isLoading.set(false);
      },
    });
  }

  #loadPageOverview() {
    this.#driveService.getDrivesPageOverview().subscribe({
      next: (res) => {
        this.pageOverview.set(res);
        this.#loadWarnings();
      },
      error: (err) => {
        console.error('Failed to load page overview', err);
      },
    });
  }

  #loadWarnings() {
    if (this.pageOverview()?.notOnlyDomainUsersAllowedCount! > 0) {
      this.hasWarnings.set(true);
      this.drivePageWarnings().notOnlyDomainUsersAllowedWarning = true;
    }

    if (this.pageOverview()?.notOnlyMembersCanAccessCount! > 0) {
      this.hasWarnings.set(true);
      this.drivePageWarnings().notOnlyMembersCanAccessWarning = true;
    }

    if (this.pageOverview()?.externalMembersDriveCount! > 0) {
      this.hasWarnings.set(true);
      this.drivePageWarnings().externalMembersWarning = true;
    }

    if (this.pageOverview()?.orphanDrives! > 0) {
      this.hasWarnings.set(true);
      this.drivePageWarnings().orphanDrivesWarning = true;
    }
  }
}
