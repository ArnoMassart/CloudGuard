import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { OrgUnitService } from '../../../services/org-unit-service';
import { OrganizationalUnits, OrgUnitNode } from './organizational-units';

const ORG_UNITS_I18N_STUB: Record<string, string> = {
  'organisational-units': 'Organizational units',
  'organisational-units.description': 'Description',
  'organisational-units.loading': 'Loading',
  structure: 'Structure',
  refreshing: 'Refreshing',
  'renew-data': 'Refresh',
  users: 'Users',
  'sub-units': 'Sub-units',
  'security-rules': 'Security rules',
  'security-rules.loading': 'Loading rules',
  'org-units.change-settings': 'Change',
  'to-admin-console': 'Admin',
  'organisational-units.error.load-failed': 'Load failed',
  'organisational-units.error.refresh-failed': 'Refresh failed',
  'organisational-units.error.policies-failed': 'Policies failed',
};

class OrgUnitsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(ORG_UNITS_I18N_STUB);
  }
}

describe('OrganizationalUnits', () => {
  let component: OrganizationalUnits;
  let fixture: ComponentFixture<OrganizationalUnits>;
  let orgUnitServiceMock: {
    getOrgUnitTree: ReturnType<typeof vi.fn>;
    getPoliciesForOrgUnit: ReturnType<typeof vi.fn>;
    refreshOrgUnitsCache: ReturnType<typeof vi.fn>;
  };

  const rootTree: OrgUnitNode = {
    id: 'root-id',
    name: 'Company',
    orgUnitPath: '/root/path',
    userCount: 10,
    children: [{ id: 'child-id', name: 'Dept', userCount: 3 }],
  };

  beforeEach(async () => {
    orgUnitServiceMock = {
      getOrgUnitTree: vi.fn(() => of(rootTree)),
      getPoliciesForOrgUnit: vi.fn(() => of([])),
      refreshOrgUnitsCache: vi.fn(() => of('')),
    };

    await TestBed.configureTestingModule({
      imports: [OrganizationalUnits],
      providers: [
        { provide: OrgUnitService, useValue: orgUnitServiceMock },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: OrgUnitsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrganizationalUnits);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads tree on init and fetches policies for selected unit path', () => {
    expect(orgUnitServiceMock.getOrgUnitTree).toHaveBeenCalled();
    expect(orgUnitServiceMock.getPoliciesForOrgUnit).toHaveBeenCalledWith('/root/path');
    expect(component.tree()).toEqual(rootTree);
    expect(component.selectedOrgUnit()?.id).toBe('root-id');
    expect(component.loading()).toBe(false);
    expect(component.expandedOuIds().has('root-id')).toBe(true);
  });

  it('sets error when tree load fails', async () => {
    orgUnitServiceMock.getOrgUnitTree.mockReturnValueOnce(throwError(() => new Error('network')));
    fixture = TestBed.createComponent(OrganizationalUnits);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.error()).toContain('network');
    expect(component.loading()).toBe(false);
  });

  it('selectUnit changes selection and loads policies for new path', async () => {
    orgUnitServiceMock.getPoliciesForOrgUnit.mockClear();
    const child = rootTree.children![0];
    component.selectUnit(child);
    await fixture.whenStable();

    expect(component.selectedOrgUnit()?.id).toBe('child-id');
    expect(orgUnitServiceMock.getPoliciesForOrgUnit).toHaveBeenCalledWith('child-id');
  });

  it('toggleExpanded and isExpanded update OU expansion state', () => {
    expect(component.isExpanded('root-id')).toBe(true);
    component.toggleExpanded('root-id');
    expect(component.isExpanded('root-id')).toBe(false);
    component.toggleExpanded('root-id');
    expect(component.isExpanded('root-id')).toBe(true);
  });

  it('isSelected reflects selectedOrgUnit', () => {
    expect(component.isSelected(rootTree)).toBe(true);
    expect(component.isSelected(rootTree.children![0])).toBe(false);
  });

  it('getSubUnitCount returns children length', () => {
    expect(component.getSubUnitCount(rootTree)).toBe(1);
    expect(component.getSubUnitCount(rootTree.children![0])).toBe(0);
  });

  it('togglePolicyExpanded toggles policy rows', () => {
    expect(component.isPolicyExpanded('p1')).toBe(false);
    component.togglePolicyExpanded('p1');
    expect(component.isPolicyExpanded('p1')).toBe(true);
  });

  it('status helpers classify policy statusClass', () => {
    expect(component.isStatusGreen('bg-green-100')).toBe(true);
    expect(component.isStatusAmber('text-AMBER')).toBe(true);
    expect(component.isStatusSlate('other')).toBe(true);
    expect(component.getStatusExplanation('policy-status-green')).toContain('conform');
    expect(component.getStatusExplanation('policy-status-amber')).toContain('aandachtspunten');
    expect(component.getStatusExplanation('unknown')).toContain('niet worden vastgesteld');
    expect(component.getStatusExplanation(undefined)).toBe('');
  });

  it('openPolicyAdmin opens absolute URLs unchanged and prefixes relative links', () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    component.openPolicyAdmin('https://example.com/x');
    expect(openSpy).toHaveBeenCalledWith('https://example.com/x', '_blank', 'noopener');

    component.openPolicyAdmin('settings/foo');
    expect(openSpy).toHaveBeenCalledWith(
      'https://admin.google.com/u/1/ac/settings/foo',
      '_blank',
      'noopener',
    );

    openSpy.mockRestore();
  });

  it('openPolicyAdmin no-ops when link missing', () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);
    component.openPolicyAdmin(undefined);
    expect(openSpy).not.toHaveBeenCalled();
    openSpy.mockRestore();
  });

  it('sets policiesError when policy request fails', async () => {
    orgUnitServiceMock.getPoliciesForOrgUnit.mockReturnValueOnce(
      throwError(() => new Error('Policy HTTP 500')),
    );
    fixture = TestBed.createComponent(OrganizationalUnits);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.policiesError()).toBe('Policy HTTP 500');
    expect(component.policies()).toEqual([]);
    expect(component.policiesLoading()).toBe(false);
  });

  it('refreshData calls refresh then reloads tree', () => {
    orgUnitServiceMock.getOrgUnitTree.mockClear();
    orgUnitServiceMock.refreshOrgUnitsCache.mockClear();
    component.refreshData();
    expect(orgUnitServiceMock.refreshOrgUnitsCache).toHaveBeenCalled();
    expect(orgUnitServiceMock.getOrgUnitTree).toHaveBeenCalled();
    expect(component.isRefreshing()).toBe(false);
  });
});
