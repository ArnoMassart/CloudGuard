import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
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
} from 'lucide-angular';
import { UsersSectionTopCard } from '../users-section/users-section-top-card/users-section-top-card';

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
export class GroupsSection {
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

  // Demo data – replace with real backend data later
  private readonly groups = signal<GroupSummary[]>([
    {
      name: 'alle-medewerkers@bedrijf.nl',
      risk: 'LOW',
      tags: ['Laag risico', 'Mailing'],
      totalMembers: 75,
      externalMembers: 0,
      externalAllowed: false,
      whoCanJoin: 'Alleen uitgenodigde gebruikers',
      whoCanView: 'Alle leden',
    },
    {
      name: 'project-alpha@bedrijf.nl',
      risk: 'MEDIUM',
      tags: ['Middel risico', 'Mailing', 'Security'],
      totalMembers: 18,
      externalMembers: 3,
      externalAllowed: true,
      whoCanJoin: 'Iedereen in de organisatie',
      whoCanView: 'Alle leden',
    },
    {
      name: 'extern-partners@bedrijf.nl',
      risk: 'HIGH',
      tags: ['Hoog risico', 'Extern', 'Security'],
      totalMembers: 12,
      externalMembers: 12,
      externalAllowed: true,
      whoCanJoin: 'Externen toegestaan',
      whoCanView: 'Alle leden',
    },
  ]);

  // Search term for the input field
  readonly searchTerm = signal('');

  // Derived values used in the UI
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
}
