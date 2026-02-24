import { Component, OnInit, effect, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';
import {
  OrgUnitNodeDto,
  OrgUnitPolicyDto,
  OrgUnitService,
} from '../../../services/org-unit-service';
import { AppIcons } from '../../../shared/AppIcons';

export interface OrgUnitNode {
  id: string;
  name: string;
  orgUnitPath?: string;
  userCount: number;
  children?: OrgUnitNode[];
  isRoot?: boolean;
}

@Component({
  selector: 'app-organizational-units',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, PageHeader],
  templateUrl: './organizational-units.html',
  styleUrl: './organizational-units.css',
})
export class OrganizationalUnits implements OnInit {
  readonly Icons = AppIcons;

  readonly expandedOuIds = signal<Set<string>>(new Set());
  readonly expandedPolicies = signal<Set<string>>(new Set());

  readonly #orgUnitService = inject(OrgUnitService);
  readonly tree = signal<OrgUnitNode | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly selectedOrgUnit = signal<OrgUnitNodeDto | null>(null);

  readonly policies = signal<OrgUnitPolicyDto[]>([]);
  readonly policiesLoading = signal(false);
  readonly policiesError = signal<string | null>(null);

  constructor() {
    effect(() => {
      const unit = this.selectedOrgUnit();
      if (!unit) {
        this.policies.set([]);
        return;
      }
      const path = unit.orgUnitPath ?? unit.id ?? '/';
      this.policiesLoading.set(true);
      this.policiesError.set(null);
      this.#orgUnitService.getPoliciesForOrgUnit(path).subscribe({
        next: (list) => {
          this.policies.set(list);
          this.policiesLoading.set(false);
        },
        error: (err) => {
          this.policiesError.set(err?.message ?? 'Beleidsregels laden mislukt');
          this.policies.set([]);
          this.policiesLoading.set(false);
        },
      });
    });
  }

  ngOnInit(): void {
    this.#orgUnitService.getOrgUnitTree().subscribe({
      next: (data) => {
        this.tree.set(data);
        this.selectedOrgUnit.set(data);
        if (data?.id) this.expandedOuIds.set(new Set([data.id]));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err.message || 'Er is een fout opgetreden bij het laden van de organisatie-eenheden.'
        );
        this.loading.set(false);
      },
    });
  }

  selectUnit(node: OrgUnitNode | OrgUnitNodeDto): void {
    this.selectedOrgUnit.set(node as OrgUnitNodeDto);
  }

  toggleExpanded(nodeId: string): void {
    this.expandedOuIds.update((set) => {
      const next = new Set(set);
      if (next.has(nodeId)) next.delete(nodeId);
      else next.add(nodeId);
      return next;
    });
  }

  isExpanded(nodeId: string): boolean {
    return this.expandedOuIds().has(nodeId);
  }

  isSelected(node: OrgUnitNode): boolean {
    return this.selectedOrgUnit()?.id === node.id;
  }

  getSubUnitCount(node: OrgUnitNode): number {
    return node.children?.length ?? 0;
  }

  togglePolicyExpanded(policyKey: string): void {
    this.expandedPolicies.update((set) => {
      const next = new Set(set);
      if (next.has(policyKey)) next.delete(policyKey);
      else next.add(policyKey);
      return next;
    });
  }

  isPolicyExpanded(policyKey: string): boolean {
    return this.expandedPolicies().has(policyKey);
  }

  openPolicyAdmin(adminLink: string | undefined): void {
    if (!adminLink) return;
    const url = adminLink.startsWith('http')
      ? adminLink
      : `https://admin.google.com/u/1/ac/${adminLink}`;
    window.open(url, '_blank', 'noopener');
  }

  readonly isStatusGreen = (statusClass: string | undefined): boolean =>
    !!statusClass?.toLowerCase().includes('green');
  readonly isStatusAmber = (statusClass: string | undefined): boolean =>
    !!statusClass?.toLowerCase().includes('amber');
  readonly isStatusSlate = (statusClass: string | undefined): boolean =>
    !this.isStatusGreen(statusClass) && !this.isStatusAmber(statusClass);

  readonly getStatusExplanation = (statusClass: string | undefined): string => {
    if (!statusClass) return '';
    if (statusClass.includes('green')) return 'Deze beleidsregel is conform.';
    if (statusClass.includes('amber'))
      return 'Let op: er zijn aandachtspunten bij deze beleidsregel.';
    return 'De status van deze beleidsregel kon niet worden vastgesteld.';
  };
}
