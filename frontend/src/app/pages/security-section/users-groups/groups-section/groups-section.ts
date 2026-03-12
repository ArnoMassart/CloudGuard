import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { LucideAngularModule } from 'lucide-angular';
import {
  GroupOrgDetail,
  GroupOverviewResponse,
  GroupService,
} from '../../../../services/group-service';
import { SectionTopCard } from '../../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../../shared/AppIcons';

type GroupRisk = 'LOW' | 'MEDIUM' | 'HIGH';

interface GroupSummary {
  name: string;
  adminId: string;
  risk: GroupRisk;
  tags: string[];
  totalMembers: number;
  externalMembers: number;
  externalAllowed: boolean;
  whoCanJoin: string;
  whoCanView: string;
}

@Component({
  selector: 'app-groups-section',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, SectionTopCard, FormsModule],
  templateUrl: './groups-section.html',
  styleUrl: './groups-section.css',
})
export class GroupsSection implements OnInit {
  readonly Icons = AppIcons;

  readonly #groupService = inject(GroupService);
  readonly groups = signal<GroupSummary[]>([]);
  readonly loading = signal(true);
  readonly isRefreshing = signal<boolean>(false);
  readonly pageOverview = signal<GroupOverviewResponse | null>(null);
  readonly searchQuery = signal('');
  private readonly searchSubject = new Subject<string>();

  readonly nextPageToken = signal<string | null>(null);
  readonly currentPage = signal(1);
  private tokenHistory: (string | null)[] = [null];
  private readonly pageSize = 2;

  readonly isExpanded = signal(true);

  hasWarnings = signal(false);

  groupPageWarnings = signal({
    externalMember: false,
    highRisk: false,
  });

  hasMultipleWarnings = computed(() => {
    const warnings = this.groupPageWarnings();
    const activeCount = Object.values(warnings).filter((val) => val === true).length;
    return activeCount > 1;
  });

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((value) => this.onSearch(value));

    this.loadGroupsOverview();
    this.loadGroups(null);
  }

  private loadGroupsOverview(): void {
    this.#groupService.getGroupsOverview().subscribe({
      next: (res) => {
        this.pageOverview.set(res);
        this.loadWarnings();
      },
      error: (err) => console.error('Failed to load groups overview', err),
    });
  }

  onKeyup(value: string): void {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  onSearch(value: string): void {
    this.currentPage.set(1);
    this.tokenHistory = [null];
    this.loadGroups(null);
  }

  nextPage(): void {
    const token = this.nextPageToken();
    if (token) {
      this.tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.loadGroups(token);
    }
  }

  prevPage(): void {
    if (this.currentPage() > 1) {
      this.tokenHistory.pop();
      const prevToken = this.tokenHistory.at(-1) ?? null;
      this.currentPage.update((p) => p - 1);
      this.loadGroups(prevToken);
    }
  }

  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#groupService.refreshGroupCache().subscribe({
      next: () => {
        this.currentPage.set(1);
        this.tokenHistory = [null];

        this.loadGroups(null);
        this.loadGroupsOverview();
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

  private loadGroups(pageToken: string | null): void {
    this.loading.set(true);
    this.#groupService
      .getOrgGroups(this.searchQuery() || undefined, pageToken ?? undefined, this.pageSize)
      .subscribe({
        next: (res) => {
          this.groups.set(this.mapToGroupSummary(res.groups));
          this.nextPageToken.set(res.nextPageToken ?? null);
          this.loading.set(false);
          console.log(this.groups());
        },
        error: (error) => {
          console.error('Error fetching groups:', error);
          this.groups.set([]);
          this.loading.set(false);
        },
      });
  }

  private mapToGroupSummary(groups: GroupOrgDetail[]): GroupSummary[] {
    return groups.map((g) => ({
      name: g.name,
      adminId: g.adminId ?? '',
      risk: g.risk as GroupRisk,
      tags: g.tags,
      totalMembers: g.totalMembers,
      externalMembers: g.externalMembers,
      externalAllowed: g.externalAllowed,
      whoCanJoin: g.whoCanJoin,
      whoCanView: g.whoCanView,
    }));
  }

  getGroupAdminUrl(group: GroupSummary): string {
    const id = group.adminId?.trim();
    if (!id) {
      return 'https://admin.google.com/u/1/ac/groups';
    }
    return `https://admin.google.com/u/1/ac/groups/${encodeURIComponent(id)}/settings`;
  }

  private loadWarnings() {
    if (this.pageOverview()?.groupsWithExternal! > 0) {
      this.groupPageWarnings().externalMember = true;
      this.hasWarnings.set(true);
    }

    if (this.pageOverview()?.highRiskGroups! > 0) {
      this.groupPageWarnings().highRisk = true;
      this.hasWarnings.set(true);
    }
  }
}
