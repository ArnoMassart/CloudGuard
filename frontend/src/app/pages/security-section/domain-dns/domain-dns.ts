import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { DomainService } from '../../../services/domain-service';
import { Domain } from '../../../models/domain/Domain';
import { AppIcons } from '../../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';
import { DnsRecord } from '../../../models/dns/DnsRecord';
import type { SecurityScoreBreakdown } from '../../../models/password/PasswordSettings';
import { DnsService } from '../../../services/dns-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-domain-dns',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, FormsModule, TranslocoPipe],
  templateUrl: './domain-dns.html',
  styleUrl: './domain-dns.css',
})
export class DomainDns implements OnInit, OnDestroy {
  readonly domains = signal<Domain[]>([]);
  readonly isLoading = signal(true);
  readonly isRefreshing = signal(false);
  readonly #domainService = inject(DomainService);
  readonly Icons = AppIcons;
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly #translocoService = inject(TranslocoService);

  private readonly dnsService = inject(DnsService);
  readonly rows = signal<DnsRecord[]>([]);
  readonly isLoadingDns = signal(false);
  readonly selectedDnsDomain = signal<string | null>(null);
  readonly expandedDnsRow = signal<string | null>(null);

  readonly totalDomains = computed(() => this.domains().length);
  readonly validDnsRecords = computed(() => this.rows().filter((r) => r.status === 'VALID').length);

  securityScore = signal<number>(0);
  securityScoreBreakdown = signal<SecurityScoreBreakdown | null>(null);

  #langSubscription?: Subscription;

  /** Controls sorted by severity */
  readonly securityControlsRows = computed(() => {
    const r = this.rows();

    const getStatusPriority = (status: string): number => {
      switch (status) {
        case 'ACTION_REQUIRED':
        case 'ERROR':
          return 0;
        case 'ATTENTION':
          return 1;
        default:
          return 2;
      }
    };

    return [...r].sort((a, b) => getStatusPriority(a.status) - getStatusPriority(b.status));
  });

  readonly hasCriticalProblems = computed(() =>
    this.rows().some((r) => r.status === 'ACTION_REQUIRED' || r.status === 'ERROR')
  );
  readonly hasWarnings = computed(() => this.rows().some((r) => r.status === 'ATTENTION'));

  readonly showDnsCriticalBanner = computed(() => this.hasCriticalProblems());

  readonly showDnsAttentionBanner = computed(
    () => this.hasWarnings() && !this.showDnsCriticalBanner(),
  );

  ngOnInit() {
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#loadDomains();
      if (this.selectedDnsDomain() !== null) {
        this.#loadDns(this.selectedDnsDomain()!);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.#langSubscription) {
      this.#langSubscription.unsubscribe();
    }
  }

  selectDnsDomain(domain: string) {
    this.selectedDnsDomain.set(domain);
    this.#loadDns(domain);
  }

  toggleExpandDnsRow(key: string) {
    this.expandedDnsRow.update((current) => (current === key ? null : key));
  }

  getDnsStatusLabel(status: string): string {
    if (status === 'VALID') return 'valid';
    if (status === 'OK') return 'optional';
    if (status === 'ACTION_REQUIRED') return 'requires-action';
    if (status === 'ATTENTION') return 'attention';
    if (status === 'ERROR') return 'error';
    return status;
  }

  getDnsStatusClass(status: string, type: string): 'valid' | 'attention' | 'error' | 'neutral' {
    if (status === 'VALID') return 'valid';
    if (status === 'OK') return 'neutral';
    if (status === 'ACTION_REQUIRED') return 'error';
    if (status === 'ATTENTION') return 'attention';
    if (status === 'ERROR') return 'error';
    return 'neutral';
  }

  getDnsRecordDescription(type: string): string {
    const descriptions: Record<string, string> = {
      SPF: 'domain.dns-records.description.spf',
      DKIM: 'domain.dns-records.description.dkim',
      DMARC: 'domain.dns-records.description.dmarc',
      MX: 'domain.dns-records.description.mx',
      DNSSEC: 'domain.dns-records.description.dnssec',
      CAA: 'domain.dns-records.description.caa',
      TXT: 'domain.dns-records.description.txt',
      CNAME: 'domain.dns-records.description.cname',
    };
    return descriptions[type] ?? '';
  }

  getControlTitle(type: string): string {
    const titles: Record<string, string> = {
      SPF: 'SPF Record',
      DKIM: 'DKIM Signing',
      DMARC: 'DMARC Policy',
      MX: 'MX Records',
      DNSSEC: 'DNSSEC',
      CAA: 'CAA Records',
      TXT: 'site-verification',
      CNAME: 'mail-subdomain',
    };
    return titles[type] ?? type;
  }

  getControlStatusDescription(row: DnsRecord): string {
    const status = row.status;
    const type = row.type;
    if (status === 'VALID') {
      const validDesc: Record<string, string> = {
        SPF: 'domain.control.description.valid.spf',
        DKIM: 'domain.control.description.valid.dkim',
        DMARC: 'domain.control.description.valid.dmarc',
        MX: 'domain.control.description.valid.mx',
        DNSSEC: 'domain.control.description.valid.dnssec',
        CAA: 'domain.control.description.valid.caa',
        TXT: 'domain.control.description.valid.txt',
        CNAME: 'domain.control.description.valid.cname',
      };
      return validDesc[type] ?? row.message ?? 'correct-configured';
    }
    if (status === 'ACTION_REQUIRED') {
      const actionDesc: Record<string, string> = {
        SPF: 'domain.control.description.action-required.spf',
        DKIM: 'domain.control.description.action-required.dkim',
        DMARC: 'domain.control.description.action-required.dmarc',
        MX: 'domain.control.description.action-required.mx',
        DNSSEC: 'domain.control.description.action-required.dnssec',
        CAA: 'domain.control.description.action-required.caa',
        TXT: 'domain.control.description.action-required.txt',
        CNAME: 'domain.control.description.action-required.cname',
      };
      return actionDesc[type] ?? row.message ?? 'requires-action';
    }
    if (status === 'ATTENTION') {
      const attentionDesc: Record<string, string> = {
        SPF: 'domain.control.description.attention.spf',
        DKIM: 'domain.control.description.attention.dkim',
        DMARC: 'domain.control.description.attention.dmarc',
        MX: 'domain.control.description.attention.mx',
        DNSSEC: 'domain.control.description.attention.dnssec',
        CAA: 'domain.control.description.attention.caa',
        TXT: 'domain.control.description.attention.txt',
        CNAME: 'domain.control.description.attention.cname',
      };
      return attentionDesc[type] ?? row.message ?? 'requires-attention';
    }
    if (status === 'OK') {
      return 'domain.control.description.ok.optional-not-configured';
    }
    if (status === 'ERROR') {
      return row.message ?? 'domain.control.description.error';
    }
    return row.message ?? '';
  }

  getControlTip(type: string, status: string): string | null {
    if (status !== 'ATTENTION' && status !== 'ACTION_REQUIRED') return null;
    const tips: Record<string, string> = {
      SPF: 'domain.control.tip.spf',
      DKIM: 'domain.control.tip.dkim',
      DMARC: 'domain.control.tip.dmarc',
      MX: 'domain.control.tip.mx',
      DNSSEC: 'domain.control.tip.dnssec',
      CAA: 'domain.control.tip.caa',
      TXT: 'domain.control.tip.txt',
      CNAME: 'domain.control.tip.cname',
    };
    return tips[type] ?? null;
  }

  openSecurityScoreDetail() {
    const breakdown =
      this.securityScoreBreakdown() ??
      this.#securityScoreDetail.createSimpleBreakdown(this.securityScore(), 'DNS');
    this.#securityScoreDetail.open(breakdown, 'DNS');
  }

  getControlArticleUrl(type: string): string | null {
    const urls: Record<string, string> = {
      SPF: 'https://support.google.com/a/answer/33786',
      DKIM: 'https://support.google.com/a/answer/174124',
      DMARC: 'https://support.google.com/a/answer/2466580',
      MX: 'https://support.google.com/a/answer/140034',
      TXT: 'https://support.google.com/a/answer/183895',
      DNSSEC: 'https://docs.cloud.google.com/dns/docs/dnssec',
      CAA: 'https://developers.cloudflare.com/ssl/edge-certificates/caa-records',
      CNAME: 'https://support.google.com/a/answer/112037',
    };
    return urls[type] ?? null;
  }

  refreshData() {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#domainService.refreshCache().subscribe({
      next: () => {
        this.#domainService.getDomains().subscribe({
          next: (domains) => {
            this.domains.set(domains);
            this.isRefreshing.set(false);
          },
          error: (err) => {
            console.error('Failed to load domains', err);
            this.isRefreshing.set(false);
          },
        });
      },
      error: (err) => {
        console.error('Failed to refresh cache', err);
        this.isRefreshing.set(false);
      },
    });
  }

  #loadDomains() {
    this.#preferencesFacade.loadWithPrefs$(this.#domainService.getDomains()).subscribe({
      next: (domains) => {
        this.domains.set(domains);
        this.isLoading.set(false);
        const primary = domains.find((d) => d.domainType === 'Primary Domain');
        if (primary && !this.selectedDnsDomain()) {
          this.selectDnsDomain(primary.domainName);
        }
      },
      error: (err) => {
        console.error('Failed to load domains', err);
        this.isLoading.set(false);
      },
    });
  }

  #loadDns(domain: string) {
    this.isLoadingDns.set(true);

    this.dnsService.getDnsRecords(domain).subscribe({
      next: (res) => {
        this.rows.set(res.rows);
        this.securityScore.set(res.securityScore);
        this.securityScoreBreakdown.set(res.securityScoreBreakdown ?? null);
        this.isLoadingDns.set(false);
      },
      error: (err) => {
        console.error('DNS load failed', err);
        this.securityScoreBreakdown.set(null);
        this.isLoadingDns.set(false);
      },
    });
  }
}
