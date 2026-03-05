import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { Domain, DomainService } from '../../../services/domain-service';
import { AppIcons } from '../../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';
import { DnsRecord, DnsService } from '../../../services/dns-service';

@Component({
  selector: 'app-domain-dns',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, FormsModule],
  templateUrl: './domain-dns.html',
  styleUrl: './domain-dns.css',
})
export class DomainDns implements OnInit {
  readonly domains = signal<Domain[]>([]);
  readonly isLoading = signal(true);
  readonly isRefreshing = signal(false);
  readonly #domainService = inject(DomainService);
  readonly Icons = AppIcons;

  private readonly dnsService = inject(DnsService);
  readonly rows = signal<DnsRecord[]>([]);
  readonly isLoadingDns = signal(false);
  readonly selectedDnsDomain = signal<string | null>(null);
  readonly expandedDnsRow = signal<string | null>(null);

  readonly totalDomains = computed(() => this.domains().length);
  readonly validDnsRecords = computed(() => this.rows().filter((r) => r.status === 'VALID').length);

  /** Security score */
  readonly securityScore = computed(() => {
    const rows = this.rows();
    if (rows.length === 0) return 0;

    const importanceWeight: Record<string, number> = {
      REQUIRED: 15,
      RECOMMENDED: 10,
      OPTIONAL: 5,
    };

    const statusMultiplier: Record<string, number> = {
      VALID: 1,
      OK: 0.5,
      ATTENTION: 0.5,
      ACTION_REQUIRED: 0,
      ERROR: 0,
    };

    let score = 0;
    for (const row of rows) {
      const weight = importanceWeight[row.importance ?? 'OPTIONAL'] ?? 5;
      const mult = statusMultiplier[row.status] ?? 0;
      score += weight * mult;
    }

    return Math.round(Math.min(100, score));
  });

  /** Controls sorted by severity */
  readonly securityControlsRows = computed(() => {
    const r = this.rows();
    const order = (s: string) =>
      s === 'ACTION_REQUIRED' || s === 'ERROR' ? 0 : s === 'ATTENTION' ? 1 : 2;
    return [...r].sort((a, b) => order(a.status) - order(b.status));
  });

  readonly hasCriticalProblems = computed(() =>
    this.rows().some((r) => r.status === 'ACTION_REQUIRED' || r.status === 'ERROR')
  );
  readonly hasWarnings = computed(() => this.rows().some((r) => r.status === 'ATTENTION'));

  ngOnInit() {
    this.#loadDomains();
  }

  selectDnsDomain(domain: string) {
    this.selectedDnsDomain.set(domain);
    this.#loadDns(domain);
  }

  toggleExpandDnsRow(key: string) {
    this.expandedDnsRow.update((current) => (current === key ? null : key));
  }

  getDnsStatusLabel(status: string): string {
    if (status === 'VALID') return 'Geldig';
    if (status === 'OK') return 'Optioneel';
    if (status === 'ACTION_REQUIRED') return 'Actie vereist';
    if (status === 'ATTENTION') return 'Aandacht';
    if (status === 'ERROR') return 'Fout';
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
      SPF: 'SPF record authoriseert Google om emails te verzenden namens uw domein',
      DKIM: 'DKIM record voor digitale ondertekening van uitgaande emails',
      DMARC: 'DMARC record voor beleid rond email authenticatie en bescherming tegen spoofing',
      MX: 'MX record voor routing van inkomende emails naar Google servers',
      DNSSEC: 'DNSKEY records voor DNSSEC – cryptografische verificatie van DNS antwoorden',
      CAA: 'CAA record beperkt welke certificaatautoriteiten SSL/TLS certificaten mogen uitgeven',
      TXT: 'TXT record voor Google site verificatie (optioneel)',
      CNAME: 'CNAME record voor mail subdomein (optioneel)',
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
      TXT: 'Site verificatie',
      CNAME: 'Mail subdomein',
    };
    return titles[type] ?? type;
  }

  getControlStatusDescription(row: DnsRecord): string {
    const status = row.status;
    const type = row.type;
    if (status === 'VALID') {
      const validDesc: Record<string, string> = {
        SPF: 'SPF record is correct geconfigureerd',
        DKIM: 'DKIM is actief voor uitgaande mail',
        DMARC: 'DMARC policy is correct ingesteld',
        MX: 'MX records verwijzen correct naar Google',
        DNSSEC: 'DNSSEC is ingeschakeld',
        CAA: 'CAA records zijn ingesteld',
        TXT: 'Google site verificatie is aanwezig',
        CNAME: 'CNAME record is geconfigureerd',
      };
      return validDesc[type] ?? row.message ?? 'Correct geconfigureerd';
    }
    if (status === 'ACTION_REQUIRED') {
      const actionDesc: Record<string, string> = {
        SPF: 'SPF record ontbreekt of niet correct geconfigureerd',
        DKIM: 'DKIM is niet ingesteld',
        DMARC: 'DMARC is niet ingesteld',
        MX: 'Domein heeft geen mailserver of MX records ontbreken',
        DNSSEC: 'DNSSEC is niet ingeschakeld',
        CAA: 'CAA records niet ingesteld',
        TXT: 'Site verificatie ontbreekt',
        CNAME: 'CNAME record ontbreekt',
      };
      return actionDesc[type] ?? row.message ?? 'Actie vereist';
    }
    if (status === 'ATTENTION') {
      const attentionDesc: Record<string, string> = {
        SPF: 'SPF record bevat geen include:_spf.google.com',
        DKIM: 'DKIM record is niet correct geconfigureerd',
        DMARC: 'DMARC policy kan worden aangescherpt naar reject',
        MX: 'Geen Google mail exchangers gevonden – controleer relayhost configuratie',
        DNSSEC: 'DNSSEC configuratie vereist aandacht',
        CAA: 'CAA records niet ingesteld',
        TXT: 'Site verificatie vereist aandacht',
        CNAME: 'CNAME configuratie vereist aandacht',
      };
      return attentionDesc[type] ?? row.message ?? 'Aandacht vereist';
    }
    if (status === 'OK') {
      return 'Optioneel – niet geconfigureerd';
    }
    if (status === 'ERROR') {
      return row.message ?? 'Fout bij ophalen DNS gegevens';
    }
    return row.message ?? '';
  }

  getControlTip(type: string, status: string): string | null {
    if (status !== 'ATTENTION' && status !== 'ACTION_REQUIRED') return null;
    const tips: Record<string, string> = {
      SPF: 'Als geen andere servers mail voor dit domein verzenden, stel dan in: v=spf1 include:_spf.google.com ~all',
      DKIM: 'Configureer DKIM in Google Admin voor uw domein',
      DMARC: "Overweeg policy te verhogen naar 'reject' voor maximale beveiliging",
      MX: 'Wijzig MX records naar Google mail servers (bijv. ASPMX.L.GOOGLE.COM)',
      DNSSEC: 'Schakel DNSSEC in bij je domeinregistrar voor extra beveiliging',
      CAA: 'Voeg CAA records toe om te bepalen welke certificaatautoriteiten certificaten voor uw domein mogen uitgeven',
      TXT: 'Voeg google-site-verification TXT record toe voor verificatie',
      CNAME: 'Configureer CNAME voor mail subdomein indien gewenst',
    };
    return tips[type] ?? null;
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
    this.#domainService.getDomains().subscribe({
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
        this.isLoadingDns.set(false);
      },
      error: (err) => {
        console.error('DNS load failed', err);
        this.isLoadingDns.set(false);
      },
    });
  }
}
