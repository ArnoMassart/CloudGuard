import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  Building2,
  Users,
  FolderTree,
  Archive,
  ChevronDown,
  ChevronRight,
  CheckCircle,
  ExternalLink,
} from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';

export interface OrgUnitNode {
  id: string;
  name: string;
  userCount: number;
  children?: OrgUnitNode[];
  isRoot?: boolean;
}

export interface Policy {
  title: string;
  description: string;
  status: string;
  statusClass: string;
  /** Shown when expanded */
  explanation?: string;
  settingsLinkText?: string;
  adminLink?: string;
}

@Component({
  selector: 'app-organizational-units',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PageHeader],
  templateUrl: './organizational-units.html',
  styleUrl: './organizational-units.css',
})
export class OrganizationalUnits {
  readonly folderTreeIcon = FolderTree;
  readonly archiveIcon = Archive;
  readonly buildingIcon = Building2;
  readonly usersIcon = Users;
  readonly checkCircleIcon = CheckCircle;
  readonly chevronDownIcon = ChevronDown;
  readonly chevronRightIcon = ChevronRight;
  readonly externalLinkIcon = ExternalLink;

  readonly rootExpanded = signal(true);
  readonly expandedPolicies = signal<Set<string>>(new Set());

  readonly tree: OrgUnitNode = {
    id: 'root',
    name: 'Bedrijf.com',
    userCount: 247,
    isRoot: true,
    children: [
      { id: 'ou1', name: 'Management', userCount: 12, children: [] },
      { id: 'ou2', name: 'IT', userCount: 35, children: [] },
      { id: 'ou3', name: 'Sales', userCount: 45, children: [] },
      { id: 'ou4', name: 'Marketing', userCount: 28, children: [] },
      { id: 'ou5', name: 'Finance', userCount: 18, children: [] },
      { id: 'ou6', name: 'HR', userCount: 8, children: [] },
    ],
  };

  readonly selectedUnit = signal<OrgUnitNode | null>(this.tree);

  readonly policies: Policy[] = [
    {
      title: 'Tweestapsverificatie (2SV)',
      description: 'Verplicht voor alle gebruikers in deze eenheid',
      status: 'Verplicht',
      statusClass: 'bg-primary/15 text-primary',
      explanation:
        'Het is een goede manier om de veiligheid van je account te garanderen, dat is waarom we verplichten om deze instellingen aan te zetten.',
      settingsLinkText: 'Klik hier om deze instellingen aan te passen',
      adminLink: '2sv',
    },
    {
      title: 'Mobiel apparaatbeheer',
      description: 'Beheerde apparaten vereist voor toegang',
      status: 'Ingeschakeld',
      statusClass: 'bg-blue-100 text-blue-800',
      explanation:
        'Alleen beheerde apparaten kunnen toegang krijgen tot bedrijfsgegevens. Dit verhoogt de beveiliging van mobiele toegang.',
      settingsLinkText: 'Klik hier om deze instellingen aan te passen',
      adminLink: 'devicemanagement',
    },
    {
      title: 'Drive delen',
      description: 'Toegestane deelmogelijkheden voor bestanden',
      status: 'Intern',
      statusClass: 'bg-fuchsia-100 text-fuchsia-800',
      explanation:
        'Bepaal wie bestanden mag delen en met wie. Interne instellingen beperken delen tot binnen de organisatie.',
      settingsLinkText: 'Klik hier om deze instellingen aan te passen',
      adminLink: 'drive',
    },
  ];

  selectUnit(node: OrgUnitNode): void {
    this.selectedUnit.set(node);
  }

  toggleRoot(): void {
    this.rootExpanded.update((v) => !v);
  }

  isSelected(node: OrgUnitNode): boolean {
    return this.selectedUnit()?.id === node.id;
  }

  getSubUnitCount(node: OrgUnitNode): number {
    return node.children?.length ?? 0;
  }

  togglePolicyExpanded(title: string): void {
    this.expandedPolicies.update((set) => {
      const next = new Set(set);
      if (next.has(title)) next.delete(title);
      else next.add(title);
      return next;
    });
  }

  isPolicyExpanded(title: string): boolean {
    return this.expandedPolicies().has(title);
  }

  openPolicyAdmin(adminLink: string | undefined): void {
    if (adminLink) window.open(`https://admin.google.com/ac/${adminLink}`);
  }
}
