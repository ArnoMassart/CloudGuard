import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ChevronLeft,
  ChevronRight,
  CircleCheck,
  CircleX,
  Clock,
  LucideAngularModule,
  Search,
  TriangleAlert,
  ShieldAlert,
  Users,
  ExternalLink,
} from 'lucide-angular';
import { UsersSectionTopCard } from '../users-section/users-section-top-card/users-section-top-card';
import { GroupOrgDetail, GroupService } from '../../../../services/group-service';

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
  imports: [CommonModule, LucideAngularModule, UsersSectionTopCard, FormsModule],
  templateUrl: './groups-section.html',
  styleUrl: './groups-section.css',
})
export class GroupsSection implements OnInit{
  readonly triangleAlertIcon = TriangleAlert;
  readonly searchIcon = Search;
  readonly checkCircle = CircleCheck;
  readonly xCircle = CircleX;
  readonly clock = Clock;
  readonly triangleAlert = TriangleAlert;
  readonly chevronLeft = ChevronLeft;
  readonly chevronRight = ChevronRight;
  readonly shieldAlertIcon = ShieldAlert;
  readonly usersIcon = Users;
  readonly externalLinkIcon = ExternalLink;

  readonly #groupService = inject(GroupService);
  readonly groups = signal<GroupSummary[]>([]);
  readonly searchTerm = signal('');
  
  readonly filteredGroups = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    if (!term) return this.groups();

    return this.groups().filter((g) => g.name.toLowerCase().includes(term));
  });

  readonly totalGroups = computed(() => this.groups().length);

  readonly groupsWithExternal = computed(
    () =>
      this.groups().filter((g) => g.externalMembers > 0 || g.externalAllowed).length
  );

  readonly highRiskGroups = computed(
    () => this.groups().filter((g) => g.risk === 'HIGH').length
  );

  readonly mediumRiskGroups = computed(
    () => this.groups().filter((g) => g.risk === 'MEDIUM').length
  );

  readonly lowRiskGroups = computed(
    () => this.groups().filter((g) => g.risk === 'LOW').length
  );

  readonly securityScore = computed(() => {
    const total = this.totalGroups();
    if (!total) return 0;

    const high = this.highRiskGroups();
    const medium = this.mediumRiskGroups();
    const low = this.lowRiskGroups();

    // Weighted score: LOW = 100, MEDIUM = 60, HIGH = 20
    const weightedTotal = low * 100 + medium * 60 + high * 20;
    return Math.round(weightedTotal / total);
  });

  ngOnInit():void {
    this.#groupService.getOrgGroups().subscribe({
      next: (groups) => {
        this.groups.set(this.mapToGroupSummary(groups));
      },
      error: (error) => {
        console.error('Error fetching groups:', error);
        this.groups.set([]);
      }
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
}