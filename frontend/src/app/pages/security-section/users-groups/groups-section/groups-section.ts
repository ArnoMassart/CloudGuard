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

  // Static example value – mirrors UsersSection "Security Score"
  readonly securityScore = computed(() => 65);

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
      risk: g.risk as GroupRisk,
      tags: g.tags,
      totalMembers: g.totalMembers,
      externalMembers: g.externalMembers,
      externalAllowed: g.externalAllowed,
      whoCanJoin: g.whoCanJoin,
      whoCanView: g.whoCanView,
    }));
  }
}