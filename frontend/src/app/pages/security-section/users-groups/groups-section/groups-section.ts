import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { GroupOverviewResponse, GroupService } from '../../../../services/group-service';
import { SecurityScoreDetailService } from '../../../../services/security-score-detail.service';
import { GroupOrgDetail } from '../../../../models/groups/GroupOrgDetail';
import { SectionTopCard } from '../../../../components/section-top-card/section-top-card';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { AppIcons } from '../../../../shared/AppIcons';
import { PageWarnings } from '../../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { SecurityPreferencesFacade } from '../../../../services/security-preferences-facade';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';

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
  imports: [
    CommonModule,
    LucideAngularModule,
    SectionTopCard,
    SearchBar,
    FormsModule,
    PageWarnings,
    PageWarningsItem,
    TranslocoPipe,
  ],
  templateUrl: './groups-section.html',
  styleUrl: './groups-section.css',
})
export class GroupsSection implements OnInit, OnDestroy {
  readonly Icons = AppIcons;

  readonly #translocoService = inject(TranslocoService);

  readonly #groupService = inject(GroupService);
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly groups = signal<GroupSummary[]>([]);
  readonly loading = signal(true);
  readonly isRefreshing = signal<boolean>(false);
  readonly pageOverview = signal<GroupOverviewResponse | null>(null);
  readonly searchQuery = signal('');

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

  readonly showExternalKpiAlert = computed(() => {
    const o = this.pageOverview();
    const n = o?.groupsWithExternal ?? 0;
    return n > 0 && !this.#preferencesFacade.isDisabled('users-groups', 'groupExternal');
  });

  readonly showHighRiskKpiAlert = computed(() => {
    const o = this.pageOverview();
    const n = o?.highRiskGroups ?? 0;
    return n > 0 && !this.#preferencesFacade.isDisabled('users-groups', 'groupExternal');
  });

  #langSubscription?: Subscription;

  ngOnInit(): void {
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.loadGroupsOverview();
      this.loadGroups(null);
    });
  }

  ngOnDestroy(): void {
    if (this.#langSubscription) {
      this.#langSubscription.unsubscribe();
    }
  }

  private loadGroupsOverview(): void {
    forkJoin({
      overview: this.#groupService.getGroupsOverview(),
      _: this.#preferencesFacade.loadDisabled$(),
    }).subscribe({
      next: ({ overview }) => {
        this.pageOverview.set(overview);
        this.loadWarnings();
      },
      error: (err) => console.error('Failed to load groups overview', err),
    });
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
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

  openSecurityScoreDetail() {
    const overview = this.pageOverview();
    const breakdown =
      overview?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(overview?.securityScore ?? 0, 'groups');
    this.#securityScoreDetail.open(breakdown, 'groups');
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
    const o = this.pageOverview();
    const external =
      !!o &&
      (o.groupsWithExternal ?? 0) > 0 &&
      !this.#preferencesFacade.isDisabled('users-groups', 'groupExternal');
    const highRisk =
      !!o &&
      (o.highRiskGroups ?? 0) > 0 &&
      !this.#preferencesFacade.isDisabled('users-groups', 'groupExternal');

    this.groupPageWarnings.set({
      externalMember: external,
      highRisk,
    });
    this.hasWarnings.set(external || highRisk);
  }
}
