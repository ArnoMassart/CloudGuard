export type PreferenceControl = 'toggle' | 'dnsImportance';

export interface PreferenceItem {
  key: string;
  label: string;
  /** Default: toggle */
  control?: PreferenceControl;
  /** SPF, DKIM, … when control is dnsImportance */
  dnsType?: string;
}

export interface SectionPreferenceConfig {
  section: string;
  label: string;
  route: string;
  preferences: PreferenceItem[];
}

export const SECTION_PREFERENCE_CONFIGS: SectionPreferenceConfig[] = [
  {
    section: 'users-groups',
    label: 'preferences.users-groups',
    route: '/users-groups',
    preferences: [
      { key: '2fa', label:'preferences.users-groups.without2fa'},
      { key: 'activity', label: 'preferences.users-groups.account-activity' },
      {
        key: 'groupExternal',
        label: 'preferences.users-groups.external-members',
      },
    ],
  },
  {
    section: 'shared-drives',
    label: 'preferences.shared-drives',
    route: '/shared-drives',
    preferences: [
      { key: 'orphan', label: 'preferences.shared-drives.no-admin' },
      { key: 'external', label: 'preferences.shared-drives.external-members' },
      {
        key: 'outsideDomain',
        label: 'preferences.shared-drives.outside-domain'
      },
      {
        key: 'nonMemberAccess',
        label: 'preferences.shared-drives.nonmember-access',
      },
    ],
  },
  {
    section: 'mobile-devices',
    label: 'preferences.devices',
    route: '/devices',
    preferences: [
      { key: 'lockscreen', label: 'preferences.devices.lockscreen' },
      { key: 'encryption', label: 'preferences.devices.encryption' },
      { key: 'osVersion', label: 'preferences.devices.osVersion' },
      { key: 'integrity', label: 'preferences.devices.integrity' },
    ],
  },
  {
    section: 'app-access',
    label: 'preferences.app-access',
    route: '/app-access',
    preferences: [{ key: 'highRisk', label: 'preferences.app-access.highRisk' }],
  },
  {
    section: 'app-passwords',
    label: 'preferences.app-passwords',
    route: '/app-passwords',
    preferences: [{ key: 'appPassword', label: 'preferences.app-passwords.active' }],
  },
  {
    section: 'password-settings',
    label: 'preferences.password-settings',
    route: '/password-settings',
    preferences: [
      { key: '2sv', label: 'preferences.password-settings.2fa' },
      { key: 'length', label: 'preferences.password-settings.passwordLength' },
      { key: 'strongPassword', label: 'preferences.password-settings.strongPasswords' },
      { key: 'expiration', label: 'preferences.password-settings.expiration' },
      { key: 'adminsSecurityKeys', label: 'preferences.password-settings.securityKeys' },
    ],
  },
  {
    section: 'domain-dns',
    label: 'preferences.domain-dns',
    route: '/domain-dns',
    preferences: [
      { key: 'impSpf', label: 'SPF', control: 'dnsImportance', dnsType: 'SPF' },
      { key: 'impDkim', label: 'DKIM', control: 'dnsImportance', dnsType: 'DKIM' },
      { key: 'impDmarc', label: 'DMARC', control: 'dnsImportance', dnsType: 'DMARC' },
      { key: 'impMx', label: 'MX', control: 'dnsImportance', dnsType: 'MX' },
      { key: 'impDnssec', label: 'DNSSEC', control: 'dnsImportance', dnsType: 'DNSSEC' },
      { key: 'impCaa', label: 'CAA', control: 'dnsImportance', dnsType: 'CAA' },
      { key: 'impTxt', label: 'preferences.dns.site-verification', control: 'dnsImportance', dnsType: 'TXT' },
      { key: 'impCname', label: 'preferences.dns.mail-subdomain', control: 'dnsImportance', dnsType: 'CNAME' },
    ],
  },
];
