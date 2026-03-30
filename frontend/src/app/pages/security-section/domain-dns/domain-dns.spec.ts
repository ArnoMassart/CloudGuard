import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { DnsRecord } from '../../../models/dns/DnsRecord';
import { Domain } from '../../../models/domain/Domain';
import { DnsService } from '../../../services/dns-service';
import { DomainService } from '../../../services/domain-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { DomainDns } from './domain-dns';

/** Minimal keys for domain-dns template + control helpers (avoid missing-translation noise). */
const DOMAIN_DNS_I18N: Record<string, string> = {
  'domain-dns': 'Domain & DNS',
  'domain.description': 'Desc',
  'domain.total': 'Total',
  'domain.valid-dns': 'Valid DNS',
  'domain.all': 'All',
  'domain.all-linked': 'Linked',
  refreshing: 'Refreshing',
  'renew-data': 'Refresh',
  'domain.domain-name': 'Domain',
  'domain.verification-status': 'Status',
  'domain.number-users': 'Users',
  verified: 'Verified',
  'not-verified': 'Not verified',
  'domain.none': 'None',
  'domain.loading': 'Loading',
  'domain.security-checks': 'Checks',
  'domain.authentication-state': 'Auth',
  'select-domain': 'Select',
  'domain.security-checks.loading': 'Loading checks',
  'domain.security-checks.critical-problems': 'Critical',
  'domain.security-checks.problems': 'Problems',
  'domain.security-checks.advices': 'Advice',
  'domain.security-checks.none': 'No checks',
  'domain.none-selected': 'None selected',
  'domain.dns-records.important': 'Important',
  'domain.dns-records.loading': 'Loading DNS',
  'name-caps': 'Name',
  'value-caps': 'Value',
  description: 'Description',
  'full-value': 'Full value',
  'domain.dns-records.none': 'No records',
  'domain.dns-records.select-domain': 'Pick domain',
  'to-admin-console': 'Admin',
  'SPF Record': 'SPF Record',
  'MX Records': 'MX Records',
  'site-verification': 'Site verification',
  valid: 'Valid',
  optional: 'Optional',
  'requires-action': 'Action',
  attention: 'Attention',
  error: 'Error',
  'domain.dns-records.description.spf': 'SPF desc',
  'domain.dns-records.description.mx': 'MX desc',
  'domain.dns-records.description.txt': 'TXT desc',
  'domain.control.description.valid.spf': 'OK SPF',
  'domain.control.description.action-required.mx': 'Fix MX',
  'domain.control.description.attention.txt': 'TXT attention',
  'domain.control.tip.mx': 'Tip MX',
  'domain.control.tip.txt': 'Tip TXT',
  'correct-configured': 'OK',
  'requires-attention': 'Attn',
  'domain.control.description.ok.optional-not-configured': 'Optional OK',
};

class DomainDnsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(DOMAIN_DNS_I18N);
  }
}

describe('DomainDns', () => {
  let component: DomainDns;
  let fixture: ComponentFixture<DomainDns>;
  let domainServiceMock: {
    getDomains: ReturnType<typeof vi.fn>;
    refreshCache: ReturnType<typeof vi.fn>;
  };
  let dnsServiceMock: { getDnsRecords: ReturnType<typeof vi.fn> };
  let preferencesMock: {
    loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => import('rxjs').Observable<T>;
  };

  const primaryDomain: Domain = {
    domainName: 'primary.com',
    domainType: 'Primary Domain',
    isVerified: true,
    totalUsers: 10,
  };

  const dnsRows: DnsRecord[] = [
    { type: 'SPF', name: '@', values: ['v=spf1'], status: 'VALID' },
    { type: 'MX', name: '@', values: [], status: 'ACTION_REQUIRED', message: 'mx issue' },
    { type: 'TXT', name: '_dmarc', values: ['v=DMARC1'], status: 'ATTENTION' },
  ];

  const dnsResponse = {
    domain: 'primary.com',
    rows: dnsRows,
    securityScore: 82,
    securityScoreBreakdown: {
      totalScore: 82,
      status: 'good',
      factors: [],
    },
  };

  beforeEach(async () => {
    domainServiceMock = {
      getDomains: vi.fn(() => of([primaryDomain])),
      refreshCache: vi.fn(() => of('')),
    };
    dnsServiceMock = {
      getDnsRecords: vi.fn(() => of(dnsResponse)),
    };
    preferencesMock = {
      loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => data$,
    };

    await TestBed.configureTestingModule({
      imports: [DomainDns],
      providers: [
        { provide: DomainService, useValue: domainServiceMock },
        { provide: DnsService, useValue: dnsServiceMock },
        { provide: SecurityPreferencesFacade, useValue: preferencesMock },
        {
          provide: SecurityScoreDetailService,
          useValue: {
            open: vi.fn(),
            createSimpleBreakdown: vi.fn((score: number, subtitle: string) => ({
              totalScore: score,
              status: 'good',
              factors: [],
            })),
          },
        },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: DomainDnsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DomainDns);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads domains, selects primary, and loads DNS records', () => {
    expect(domainServiceMock.getDomains).toHaveBeenCalled();
    expect(dnsServiceMock.getDnsRecords).toHaveBeenCalledWith('primary.com');
    expect(component.selectedDnsDomain()).toBe('primary.com');
    expect(component.rows()).toEqual(dnsRows);
    expect(component.securityScore()).toBe(82);
    expect(component.isLoading()).toBe(false);
    expect(component.isLoadingDns()).toBe(false);
    expect(component.totalDomains()).toBe(1);
    expect(component.validDnsRecords()).toBe(1);
  });

  it('does not auto-select DNS when no primary domain', async () => {
    domainServiceMock.getDomains.mockReturnValue(
      of([
        { domainName: 'secondary.com', domainType: 'Secondary Domain', isVerified: true },
      ]),
    );
    dnsServiceMock.getDnsRecords.mockClear();
    fixture = TestBed.createComponent(DomainDns);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.selectedDnsDomain()).toBe(null);
    expect(dnsServiceMock.getDnsRecords).not.toHaveBeenCalled();
  });

  it('securityControlsRows sorts by severity', () => {
    const sorted = component.securityControlsRows().map((r) => r.type);
    expect(sorted).toEqual(['MX', 'TXT', 'SPF']);
  });

  it('critical banner when ACTION_REQUIRED or ERROR exists; attention-only when not', () => {
    expect(component.hasCriticalProblems()).toBe(true);
    expect(component.hasWarnings()).toBe(true);
    expect(component.showDnsCriticalBanner()).toBe(true);
    expect(component.showDnsAttentionBanner()).toBe(false);

    component.rows.set([
      { type: 'TXT', name: 't', values: [], status: 'ATTENTION' },
    ]);
    expect(component.showDnsCriticalBanner()).toBe(false);
    expect(component.showDnsAttentionBanner()).toBe(true);
  });

  it('getDnsStatusLabel and getDnsStatusClass', () => {
    expect(component.getDnsStatusLabel('VALID')).toBe('valid');
    expect(component.getDnsStatusLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(component.getDnsStatusClass('VALID', 'SPF')).toBe('valid');
    expect(component.getDnsStatusClass('ACTION_REQUIRED', 'MX')).toBe('error');
    expect(component.getDnsStatusClass('ATTENTION', 'TXT')).toBe('attention');
    expect(component.getDnsStatusClass('OK', 'TXT')).toBe('neutral');
  });

  it('getDnsRecordDescription and getControlTitle', () => {
    expect(component.getDnsRecordDescription('SPF')).toContain('spf');
    expect(component.getDnsRecordDescription('OTHER')).toBe('');
    expect(component.getControlTitle('SPF')).toBe('SPF Record');
    expect(component.getControlTitle('X')).toBe('X');
  });

  it('getControlStatusDescription covers statuses', () => {
    expect(component.getControlStatusDescription(dnsRows[0])).toContain('valid.spf');
    expect(component.getControlStatusDescription(dnsRows[1])).toContain('action-required.mx');
    expect(component.getControlStatusDescription(dnsRows[2])).toContain('attention.txt');
    expect(
      component.getControlStatusDescription({
        type: 'SPF',
        name: '',
        values: [],
        status: 'OK',
      }),
    ).toBe('domain.control.description.ok.optional-not-configured');
    expect(
      component.getControlStatusDescription({
        type: 'CAA',
        name: '',
        values: [],
        status: 'ERROR',
        message: 'fail',
      }),
    ).toBe('fail');
  });

  it('getControlTip only for attention or action required', () => {
    expect(component.getControlTip('MX', 'ACTION_REQUIRED')).toContain('tip.mx');
    expect(component.getControlTip('SPF', 'VALID')).toBe(null);
  });

  it('getControlArticleUrl returns known URLs or null', () => {
    expect(component.getControlArticleUrl('SPF')).toContain('google.com');
    expect(component.getControlArticleUrl('UNKNOWN')).toBe(null);
  });

  it('selectDnsDomain loads DNS for domain', () => {
    dnsServiceMock.getDnsRecords.mockClear();
    component.selectDnsDomain('other.com');
    expect(component.selectedDnsDomain()).toBe('other.com');
    expect(dnsServiceMock.getDnsRecords).toHaveBeenCalledWith('other.com');
  });

  it('toggleExpandDnsRow toggles key', () => {
    const key = 'SPF@';
    component.toggleExpandDnsRow(key);
    expect(component.expandedDnsRow()).toBe(key);
    component.toggleExpandDnsRow(key);
    expect(component.expandedDnsRow()).toBe(null);
  });

  it('openSecurityScoreDetail uses breakdown from response', () => {
    const detail = TestBed.inject(SecurityScoreDetailService);
    component.openSecurityScoreDetail();
    expect(detail.open).toHaveBeenCalledWith(
      { totalScore: 82, status: 'good', factors: [] },
      'DNS',
    );
  });

  it('refreshData refreshes domain cache then reloads domains', () => {
    domainServiceMock.getDomains.mockClear();
    domainServiceMock.refreshCache.mockClear();
    component.refreshData();
    expect(domainServiceMock.refreshCache).toHaveBeenCalled();
    expect(domainServiceMock.getDomains).toHaveBeenCalled();
    expect(component.isRefreshing()).toBe(false);
  });

  it('refreshData stops spinners when refresh fails', () => {
    const errSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    domainServiceMock.refreshCache.mockReturnValueOnce(throwError(() => new Error('cache')));
    component.refreshData();
    expect(component.isRefreshing()).toBe(false);
    errSpy.mockRestore();
  });
});
