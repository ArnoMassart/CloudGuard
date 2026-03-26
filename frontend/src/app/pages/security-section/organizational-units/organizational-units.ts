import { Component, OnInit, effect, signal, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';
import { OrgUnitService } from '../../../services/org-unit-service';
import { OrgUnitNodeDto } from '../../../models/org-unit/OrgUnitNodeDto';
import { OrgUnitPolicyDto } from '../../../models/org-unit/OrgUnitPolicyDto';
import { AppIcons } from '../../../shared/AppIcons';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';

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
  imports: [CommonModule, LucideAngularModule, PageHeader, TranslocoPipe],
  templateUrl: './organizational-units.html',
  styleUrl: './organizational-units.css',
})
export class OrganizationalUnits implements OnInit, OnDestroy {
  readonly Icons = AppIcons;

  readonly #translocoService = inject(TranslocoService);

  readonly expandedOuIds = signal<Set<string>>(new Set());
  readonly expandedPolicies = signal<Set<string>>(new Set());

  readonly #orgUnitService = inject(OrgUnitService);
  readonly tree = signal<OrgUnitNode | null>(null);
  readonly loading = signal(true);
  readonly isRefreshing = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly selectedOrgUnit = signal<OrgUnitNodeDto | null>(null);

  readonly policies = signal<OrgUnitPolicyDto[]>([]);
  readonly policiesLoading = signal(false);
  readonly policiesError = signal<string | null>(null);

  #langSubscription?: Subscription;

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
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.loadUnitTree();
    });
  }

  ngOnDestroy(): void {
    if (this.#langSubscription) {
      this.#langSubscription.unsubscribe();
    }
  }

  loadUnitTree(): void {
    this.#orgUnitService.getOrgUnitTree().subscribe({
      next: (data) => {
        this.tree.set(data);
        this.selectedOrgUnit.set(data);
        if (data?.id) this.expandedOuIds.set(new Set([data.id]));
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err.message || 'Er is een fout opgetreden bij het laden van de organisatie-eenheden.',
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

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#orgUnitService.refreshOrgUnitsCache().subscribe({
      next: () => {
        this.loadUnitTree();
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
