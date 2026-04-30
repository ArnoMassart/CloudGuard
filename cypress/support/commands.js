// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

/// <reference types="cypress" />

Cypress.Commands.add('visitApp', (path)=>{
  cy.visit(path, {
    onBeforeLoad(win){
      win.sessionStorage.setItem("has_seen_splash", "true");
    },
  });
});

const E2E_USER = {
  email: 'e2e-user@test.local',
  firstName: 'E2E',
  lastName: 'User',
  pictureUrl: null,
  roles: ['SUPER_ADMIN'],
  createdAt: '2026-01-01T00:00:00',
  roleRequested: false,
  organizationRequested: false,
  organizationId: 1,
  organizationName: 'Test Org',
  isCloudmenStaff: false,
};

const DASHBOARD_PAGE = {
  scores: {
    usersScore: 100,
    groupsScore: 80,
    drivesScore: 90,
    devicesScore: 70,
    appAccessScore: 60,
    appPasswordsScore: 80,
    passwordSettingsScore: 90,
    dnsScore: 50,
  },
  overallScore: 80,
  lastUpdated: '2026-04-04T00:00:00',
};

const DASHBOARD_OVERVIEW = {
  totalNotifications: 10,
  criticalNotifications: 3,
};

Cypress.Commands.add('stubLoggedInSession', ()=>{
  // Path-only regex: host-agnostic and works with IPv6 baseUrl (e.g. http://[::1]:4200) where ** globs mis-match.
  cy.intercept('GET', /\/api\/auth\/check-session(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: E2E_USER,
  });
  cy.intercept('GET', /\/api\/auth\/me(?:\/)?(?=\?|$)/, { body: E2E_USER }).as('authMe');
  cy.intercept('GET', /\/api\/user\/is-cloudmen-staff/, { statusCode: 200, body: false });
  cy.intercept('GET', /\/api\/teamleader\/check/, { statusCode: 200, body: { hasAccess: true } });
  cy.intercept('GET', /\/api\/dashboard\/overview/, DASHBOARD_OVERVIEW);
  cy.intercept('GET', /\/api\/dashboard(?=\?|$)/, DASHBOARD_PAGE);
  cy.intercept('GET', /\/api\/user\/language(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: 'nl',
  });
  cy.intercept('GET', /\/api\/user\/all\/requested-count/, { statusCode: 200, body: 0 });
});

Cypress.Commands.add('stubGuestSession', ()=>{
  cy.intercept('GET', /\/api\/auth\/check-session(?:\/)?(?=\?|$)/, { statusCode: 401, body: {} });
});

const USERS_SECTION_OVERVIEW = {
  totalUsers: 42,
  withoutTwoFactor: 3,
  adminUsers: 2,
  securityScore: 88,
  activeLongNoLoginCount: 0,
  inactiveRecentLoginCount: 0,
  warnings: {
    hasWarnings: false,
    hasMultipleWarnings: false,
    items: {},
  },
};

const USERS_SECTION_PAGE = {
  users: [
    {
      fullName: 'E2E Person',
      email: 'person@example.com',
      role: 'Regular User',
      active: true,
      lastLogin: '2026-04-01',
      twoFactorEnabled: true,
      securityConform: true,
      securityViolationCodes: [],
    },
  ],
  nextPageToken: '',
};

const GROUPS_SECTION_OVERVIEW = {
  totalGroups: 15,
  groupsWithExternal: 1,
  highRiskGroups: 0,
  mediumRiskGroups: 1,
  lowRiskGroups: 14,
  securityScore: 90,
  warnings: {
    hasWarnings: false,
    hasMultipleWarnings: false,
    items: {},
  },
};

const GROUPS_SECTION_PAGE = {
  groups: [
    {
      name: 'E2E Test Group',
      adminId: 'admin@test.local',
      risk: 'LOW',
      tags: [],
      totalMembers: 5,
      externalMembers: 0,
      externalAllowed: false,
      whoCanJoin: 'ANYONE_CAN_JOIN',
      whoCanView: 'ALL_VIEW',
    },
  ],
  nextPageToken: '',
};

/** Stubs Users & Groups API (use after stubLoggedInSession). */
Cypress.Commands.add('stubUsersGroupsApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/users\/overview/, { body: USERS_SECTION_OVERVIEW });
  cy.intercept('GET', /\/api\/google\/users\?/, { body: USERS_SECTION_PAGE });
  cy.intercept('GET', /\/api\/google\/groups\/overview/, { body: GROUPS_SECTION_OVERVIEW });
  cy.intercept('GET', /\/api\/google\/groups\?/, { body: GROUPS_SECTION_PAGE });
});

const ORG_UNITS_TREE_STUB = {
  id: 'root-e2e',
  name: 'E2E Root OU',
  orgUnitPath: '/root-e2e',
  userCount: 100,
  isRoot: true,
  children: [
    {
      id: 'child-e2e',
      name: 'E2E Child OU',
      orgUnitPath: '/child-e2e',
      userCount: 12,
      children: [],
    },
  ],
};

const ORG_UNIT_POLICIES_STUB = [
  {
    key: 'e2e-policy',
    title: 'E2E Mobile Policy',
    description: 'Stubbed policy row for Cypress',
    status: 'Conform',
    statusClass: 'green',
    inherited: false,
    source: 'ORG_UNIT',
  },
];

/** Stubs org unit tree + policies (after stubLoggedInSession). Policies route must stay distinct from the tree GET. */
Cypress.Commands.add('stubOrganizationalUnitsApis', () => {
  cy.intercept('GET', /\/api\/google\/org-units\/policies/, {
    statusCode: 200,
    body: ORG_UNIT_POLICIES_STUB,
  });
  cy.intercept('GET', /^.*\/api\/google\/org-units(?:\/)?$/, (req) => {
    req.reply({ statusCode: 200, body: ORG_UNITS_TREE_STUB });
  });
});

const SHARED_DRIVES_OVERVIEW_STUB = {
  totalDrives: 7,
  orphanDrives: 0,
  totalHighRisk: 1,
  totalExternalMembers: 4,
  securityScore: 82,
  notOnlyDomainUsersAllowedCount: 0,
  notOnlyMembersCanAccessCount: 0,
  externalMembersDriveCount: 1,
  warnings: {
    hasWarnings: false,
    hasMultipleWarnings: false,
    items: {},
  },
};

const SHARED_DRIVES_PAGE_STUB = {
  drives: [
    {
      id: 'e2e-drive-1',
      name: 'E2E Marketing Drive',
      totalMembers: 8,
      externalMembers: 2,
      totalOrganizers: 1,
      createdTime: '2026-01-15T10:00:00Z',
      parsedTime: '15-01-2026',
      onlyDomainUsersAllowed: true,
      onlyMembersCanAccess: true,
      risk: 'low',
    },
    {
      id: 'e2e-drive-2',
      name: 'E2E Finance Drive',
      totalMembers: 3,
      externalMembers: 0,
      totalOrganizers: 0,
      createdTime: '2026-02-01T12:00:00Z',
      parsedTime: '01-02-2026',
      onlyDomainUsersAllowed: false,
      onlyMembersCanAccess: false,
      risk: 'high',
    },
  ],
  nextPageToken: null,
};

/** Stubs shared drives list, overview, preferences/disabled, and refresh (after stubLoggedInSession). */
Cypress.Commands.add('stubSharedDrivesApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/drives\/overview/, {
    statusCode: 200,
    body: SHARED_DRIVES_OVERVIEW_STUB,
  });
  cy.intercept('POST', /\/api\/google\/drives\/refresh/, {
    statusCode: 200,
    body: '',
  });
  cy.intercept('GET', /\/api\/google\/drives\?/, (req) => {
    const url = new URL(req.url);
    const q = url.searchParams.get('query') ?? '';
    if (q === '__E2E_EMPTY__') {
      req.reply({ statusCode: 200, body: { drives: [], nextPageToken: null } });
    } else {
      req.reply({ statusCode: 200, body: SHARED_DRIVES_PAGE_STUB });
    }
  });
});

const DEVICES_OVERVIEW_STUB = {
  totalDevices: 42,
  totalNonCompliant: 2,
  totalApprovedDevices: 36,
  securityScore: 88,
  lockScreenCount: 0,
  encryptionCount: 0,
  osVersionCount: 0,
  integrityCount: 0,
  warnings: {
    hasWarnings: false,
    hasMultipleWarnings: false,
    items: {},
  },
};

const DEVICES_TYPES_STUB = ['ANDROID'];

const DEVICES_PAGE_STUB = {
  devices: [
    {
      resourceId: 'e2e-device-1',
      deviceType: 'ANDROID',
      userName: 'E2E Person',
      userEmail: 'e2e.person@example.com',
      deviceName: 'E2E Pixel 8',
      model: 'Pixel 8',
      os: 'Android 15',
      lastSync: '2026-04-01 10:00',
      status: 'Approved',
      complianceScore: 85,
      lockSecure: true,
      screenLockText: 'Schermvernberging actief',
      encSecure: true,
      encryptionText: 'Encryptie actief',
      osSecure: true,
      osText: 'OS up-to-date',
      intSecure: true,
      integrityText: 'Geen jailbreak/root',
    },
  ],
  nextPageToken: null,
};

/** Stubs devices overview, types list, paging GET, prefs/disabled, refresh (after stubLoggedInSession). */
Cypress.Commands.add('stubDevicesApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/devices\/overview/, {
    statusCode: 200,
    body: DEVICES_OVERVIEW_STUB,
  });
  cy.intercept('GET', /\/api\/google\/devices\/types/, {
    statusCode: 200,
    body: DEVICES_TYPES_STUB,
  });
  cy.intercept('POST', /\/api\/google\/devices\/refresh/, {
    statusCode: 200,
    body: '',
  });
  cy.intercept('GET', /\/api\/google\/devices\?/, (req) => {
    const url = new URL(req.url);
    const status = url.searchParams.get('status') ?? '';
    if (status === 'Blocked') {
      req.reply({ statusCode: 200, body: { devices: [], nextPageToken: null } });
      return;
    }
    req.reply({ statusCode: 200, body: DEVICES_PAGE_STUB });
  });
});

const APP_ACCESS_OVERVIEW_STUB = {
  totalThirdPartyApps: 12,
  totalHighRiskApps: 1,
  totalPermissionsGranted: 48,
  securityScore: 77,
};

const APP_ACCESS_PAGE_STUB = {
  apps: [
    {
      id: 'e2e-app-oauth-1',
      name: 'E2E Connected App',
      appType: 'MARKETPLACE',
      appSource: 'Google Workspace',
      isThirdParty: true,
      isAnonymous: false,
      isHighRisk: false,
      totalUsers: 5,
      exposurePercentage: 12,
      scopeCount: 3,
      dataAccess: [
        { name: 'Gmail API', rights: 'app-access.rights.email-viewing', risk: false },
      ],
      highRiskCount: 0,
    },
  ],
  nextPageToken: '',
  allFilteredApps: 1,
  allHighRiskApps: 0,
  allNotHighRiskApps: 1,
};

/** Stubs OAuth / app-access overview + paged apps + prefs/disabled + refresh (after stubLoggedInSession). Backend path segment is `/google/oAuth`. */
Cypress.Commands.add('stubAppAccessApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/oAuth\/overview/, {
    statusCode: 200,
    body: APP_ACCESS_OVERVIEW_STUB,
  });
  cy.intercept('POST', /\/api\/google\/oAuth\/refresh/, {
    statusCode: 200,
    body: '',
  });
  cy.intercept('GET', /\/api\/google\/oAuth\?/, (req) => {
    const url = new URL(req.url);
    const query = url.searchParams.get('query') ?? '';
    if (query === '__E2E_EMPTY__') {
      req.reply({
        statusCode: 200,
        body: {
          apps: [],
          nextPageToken: '',
          allFilteredApps: 0,
          allHighRiskApps: 0,
          allNotHighRiskApps: 0,
        },
      });
      return;
    }
    req.reply({ statusCode: 200, body: APP_ACCESS_PAGE_STUB });
  });
});

const APP_PASSWORDS_OVERVIEW_STUB = {
  allowed: true,
  totalAppPasswords: 7,
  securityScore: 92,
};

const APP_PASSWORDS_PAGE_STUB = {
  users: [
    {
      id: 'user-e2e-1',
      name: 'E2E Worker',
      email: 'e2e.worker@test.local',
      role: 'Regular',
      tsv: true,
      passwords: [
        {
          codeId: 9001,
          name: 'Legacy Mail Client',
          creationTime: '2026-01-15',
          lastTimeUsed: null,
        },
      ],
    },
  ],
  nextPageToken: null,
};

/** Stubs app-passwords overview + paged users + prefs/disabled + refresh (after stubLoggedInSession). */
Cypress.Commands.add('stubAppPasswordsApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/app-passwords\/overview/, {
    statusCode: 200,
    body: APP_PASSWORDS_OVERVIEW_STUB,
  });
  cy.intercept('POST', /\/api\/google\/app-passwords\/refresh/, {
    statusCode: 200,
    body: '',
  });
  cy.intercept('GET', /\/api\/google\/app-passwords\?/, (req) => {
    req.reply({ statusCode: 200, body: APP_PASSWORDS_PAGE_STUB });
  });
});

const PASSWORD_SETTINGS_STUB = {
  passwordPoliciesByOu: [
    {
      orgUnitPath: '/',
      orgUnitName: 'E2E Root',
      userCount: 100,
      score: 88,
      problemCount: 0,
      minLength: 14,
      expirationDays: 90,
      strongPasswordRequired: true,
      reusePreventionCount: 5,
      inherited: false,
    },
  ],
  twoStepVerification: {
    byOrgUnit: [
      {
        orgUnitPath: '/',
        orgUnitName: 'E2E Root',
        enforced: true,
        enrolledCount: 80,
        totalCount: 100,
      },
    ],
    totalEnrolled: 80,
    totalEnforced: 100,
    totalUsers: 100,
  },
  usersWithForcedChange: [],
  summary: {
    usersWithForcedChange: 0,
    usersWith2SvEnrolled: 80,
    usersWith2SvEnforced: 100,
    totalUsers: 100,
  },
  adminsWithoutSecurityKeys: [],
  securityScore: 91,
};

/** Stubs password-settings payload + prefs/disabled + refresh (after stubLoggedInSession). */
Cypress.Commands.add('stubPasswordSettingsApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/password-settings(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: PASSWORD_SETTINGS_STUB,
  });
  cy.intercept('POST', /\/api\/google\/password-settings\/refresh/, {
    statusCode: 200,
    body: '',
  });
});

const DOMAIN_DNS_LIST_STUB = [
  {
    domainName: 'e2e-example.com',
    domainType: 'Primary Domain',
    isVerified: true,
    totalUsers: 42,
  },
];

const DOMAIN_DNS_RECORDS_RESPONSE_STUB = {
  domain: 'e2e-example.com',
  rows: [
    {
      type: 'SPF',
      name: '@',
      values: ['v=spf1 include:_spf.google.com ~all'],
      status: 'VALID',
    },
    {
      type: 'MX',
      name: '@',
      values: ['1 ASPMX.L.GOOGLE.COM.'],
      status: 'VALID',
    },
  ],
  securityScore: 95,
};

/** Stubs domains list, DNS records for selected domain, prefs/disabled, domains refresh (after stubLoggedInSession). */
Cypress.Commands.add('stubDomainDnsApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled/, { body: [] });
  cy.intercept('GET', /\/api\/google\/domains(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: DOMAIN_DNS_LIST_STUB,
  });
  cy.intercept('POST', /\/api\/google\/domains\/refresh/, {
    statusCode: 200,
    body: '',
  });
  cy.intercept('GET', /\/api\/google\/dns-records\/records/, (req) => {
    req.reply({ statusCode: 200, body: DOMAIN_DNS_RECORDS_RESPONSE_STUB });
  });
});

const LICENSES_OVERVIEW_STUB = {
  totalAssigned: 120,
  riskyAccounts: 0,
  unusedLicenses: 5,
};

const LICENSES_PAGE_STUB = {
  licenseTypes: [
    {
      skuId: 'sku-e2e-enterprise',
      skuName: 'E2E Google Workspace Enterprise',
      totalAssigned: 80,
    },
    {
      skuId: 'sku-e2e-starter',
      skuName: 'E2E Business Starter',
      totalAssigned: 40,
    },
  ],
  inactiveUsers: [
    {
      email: 'inactive.e2e@example.com',
      lastLogin: '2025-06-01',
      licenseType: 'Enterprise Plus',
      isTwoFactorEnabled: false,
      daysInactive: 120,
    },
  ],
  maxLicenseAmount: 200,
  chartStepSize: 20,
};

/** Stubs licenses overview + main license payload (after stubLoggedInSession). API path segment is `/google/license`. */
Cypress.Commands.add('stubLicensesApis', () => {
  cy.intercept('GET', /\/api\/google\/license\/overview/, {
    statusCode: 200,
    body: LICENSES_OVERVIEW_STUB,
  });
  cy.intercept('GET', /\/api\/google\/license(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: LICENSES_PAGE_STUB,
  });
});

/** Stubs GET /notifications, POST /notifications/sync, POST /notifications/feedback (after stubLoggedInSession). */
const NOTIFICATIONS_STUB_BODY = {
  active: [
    {
      id: 'e2e-notif-critical',
      severity: 'critical',
      title: 'E2E kritieke melding titel',
      description: 'Stub melding voor Cypress — kritiek.',
      recommendedActions: ['Controleer instellingen'],
      notificationType: 'user-control',
      source: 'e2e-google-workspace',
      sourceLabel: 'Google Workspace',
      sourceRoute: '/users-groups',
      hasReported: false,
      supportsDetails: false,
      createdAt: '2026-01-15T10:00:00.000Z',
    },
    {
      id: 'e2e-notif-info-processing',
      severity: 'info',
      title: 'E2E info in behandeling',
      description: 'Al gerapporteerd aan support.',
      notificationType: 'drive-external',
      source: 'e2e-google-workspace',
      sourceLabel: 'Google Workspace',
      sourceRoute: '/shared-drives',
      hasReported: true,
      supportsDetails: false,
      createdAt: '2026-01-14T09:00:00.000Z',
    },
  ],
  solved: [
    {
      id: 'e2e-notif-solved',
      severity: 'warning',
      title: 'E2E opgeloste waarschuwing',
      description: 'Deze melding is opgelost.',
      notificationType: 'device-lockscreen',
      source: 'e2e-google-workspace',
      sourceLabel: 'Google Workspace',
      sourceRoute: '/devices',
      hasReported: true,
      supportsDetails: false,
      createdAt: '2026-01-10T08:00:00.000Z',
    },
  ],
  lastNotificationSyncAt: '2026-01-20T12:00:00.000Z',
};

Cypress.Commands.add('stubNotificationsApis', () => {
  cy.intercept('GET', /\/api\/notifications(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: NOTIFICATIONS_STUB_BODY,
  }).as('notificationsList');
  cy.intercept('POST', /\/api\/notifications\/sync/, {
    statusCode: 204,
    body: null,
  }).as('notificationsSync');
  cy.intercept('POST', /\/api\/notifications\/feedback/, {
    statusCode: 200,
    body: null,
  }).as('notificationsFeedback');
});

/** Minimal preferences payload — omitted keys behave as enabled in the UI (after stubbed GET). Use after stubLoggedInSession. */
const SECURITY_PREFERENCES_STUB_BODY = {
  preferences: {},
  dnsImportance: {},
  dnsImportanceOverrideTypes: [],
};

/** Stubs GET/PUT `/api/user/preferences` and GET disabled list (after stubLoggedInSession). */
Cypress.Commands.add('stubSecurityPreferencesApis', () => {
  cy.intercept('GET', /\/api\/user\/preferences\/disabled(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: [],
  }).as('preferencesDisabled');
  cy.intercept('GET', /\/api\/user\/preferences(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: SECURITY_PREFERENCES_STUB_BODY,
  }).as('preferencesGet');
  cy.intercept('PUT', /\/api\/user\/preferences(?:\/)?(?=\?|$)/, {
    statusCode: 204,
    body: null,
  }).as('preferencesPut');
});

/** Overrides default guest staff check so `cloudmenStaffGuard` allows `/accounts-manager`. Call after stubLoggedInSession. */
Cypress.Commands.add('stubCloudmenStaffAccess', () => {
  cy.intercept('GET', /\/api\/user\/is-cloudmen-staff/, { statusCode: 200, body: true }).as('cloudmenStaff');
});

const ACCOUNTS_MANAGER_ORGS = [
  {
    id: 1,
    name: 'E2E Organization',
    customerId: 'cust-e2e-1',
    adminEmail: 'org.admin@e2e.example.com',
  },
];

const ACCOUNTS_DB_USERS_PAGE = {
  users: [
    {
      email: 'acct.e2e@example.com',
      firstName: 'Account',
      lastName: 'E2E',
      pictureUrl: null,
      roles: ['SUPER_ADMIN'],
      createdAt: '2026-02-01T00:00:00.000Z',
      roleRequested: false,
      organizationRequested: false,
      organizationId: 1,
      organizationName: 'E2E Organization',
      isCloudmenStaff: true,
    },
  ],
  nextPageToken: '',
};

const ACCOUNTS_DB_USERS_NO_ROLES_PAGE = {
  users: [
    {
      email: 'pending.roles@example.com',
      firstName: 'Pending',
      lastName: 'Roles',
      pictureUrl: null,
      roles: [],
      createdAt: '2026-02-05T00:00:00.000Z',
      roleRequested: true,
      organizationRequested: false,
      organizationId: 1,
      organizationName: 'E2E Organization',
    },
  ],
  nextPageToken: '',
};

const ACCOUNTS_ORGS_PAGED = {
  organizations: ACCOUNTS_MANAGER_ORGS,
  nextPageToken: '',
};

/** Stubs account-manager user/org list APIs (`/api/user/all`, `/api/user/all/no-roles`, `/api/org/all`, `/api/org/all-paged`). */
Cypress.Commands.add('stubAccountsManagerApis', () => {
  cy.intercept('GET', /\/api\/user\/all\/no-roles/, {
    statusCode: 200,
    body: ACCOUNTS_DB_USERS_NO_ROLES_PAGE,
  }).as('accountsUsersNoRoles');
  cy.intercept('GET', /\/api\/user\/all(?:\/)?(?=\?|$)/, {
    statusCode: 200,
    body: ACCOUNTS_DB_USERS_PAGE,
  }).as('accountsUsersAll');
  cy.intercept('GET', /\/api\/org\/all-paged/, {
    statusCode: 200,
    body: ACCOUNTS_ORGS_PAGED,
  }).as('accountsOrgsPaged');
  cy.intercept('GET', /\/api\/org\/all(?=$|[?#])/, {
    statusCode: 200,
    body: ACCOUNTS_MANAGER_ORGS,
  }).as('accountsOrgsAll');
});