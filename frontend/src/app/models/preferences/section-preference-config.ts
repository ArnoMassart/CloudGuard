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
    label: 'Gebruikers & Groepen',
    route: '/users-groups',
    preferences: [
      { key: '2fa', label: 'Waarschuwingen over gebruikers zonder 2FA' },
      { key: 'activity', label: 'Waarschuwingen over gebruikeractiviteit (inactiviteit, recente login)' },
      {
        key: 'groupExternal',
        label:
          'Waarschuwingen over groepen met externe leden en groepen met hoog risico (open of externe instellingen)',
      },
    ],
  },
  {
    section: 'shared-drives',
    label: 'Gedeelde Drives',
    route: '/shared-drives',
    preferences: [
      { key: 'orphan', label: 'Waarschuwingen over drives zonder eigenaar' },
      { key: 'external', label: 'Waarschuwingen over drives met externe leden' },
      {
        key: 'outsideDomain',
        label: 'Waarschuwingen over drives die delen met gebruikers buiten uw domein toestaan',
      },
      {
        key: 'nonMemberAccess',
        label: 'Waarschuwingen over drives die toegang voor niet-leden toestaan',
      },
    ],
  },
  {
    section: 'mobile-devices',
    label: 'Mobiele Apparaten',
    route: '/devices',
    preferences: [
      { key: 'lockscreen', label: 'Waarschuwingen over apparaten zonder vergrendelscherm' },
      { key: 'encryption', label: 'Waarschuwingen over apparaten zonder encryptie' },
      { key: 'osVersion', label: 'Waarschuwingen over verouderde OS-versie' },
      { key: 'integrity', label: 'Waarschuwingen over integriteitsproblemen (root/jailbreak)' },
    ],
  },
  {
    section: 'app-access',
    label: 'App Toegang',
    route: '/app-access',
    preferences: [{ key: 'highRisk', label: 'Waarschuwingen over apps met hoog risico' }],
  },
  {
    section: 'app-passwords',
    label: 'App-wachtwoorden',
    route: '/app-passwords',
    preferences: [{ key: 'appPassword', label: 'Meldingen over actieve app-wachtwoorden' }],
  },
  {
    section: 'password-settings',
    label: 'Wachtwoordinstellingen',
    route: '/password-settings',
    preferences: [
      { key: '2sv', label: '2-Step Verification niet verplicht' },
      { key: 'length', label: 'Zwakke wachtwoordlengte (< 12 tekens)' },
      { key: 'strongPassword', label: 'Sterke wachtwoorden niet verplicht' },
      { key: 'expiration', label: 'Wachtwoorden verlopen nooit' },
      { key: 'adminsSecurityKeys', label: 'Admins zonder security key' },
    ],
  },
  {
    section: 'domain-dns',
    label: 'Domein & DNS',
    route: '/domain-dns',
    preferences: [
      { key: 'impSpf', label: 'SPF', control: 'dnsImportance', dnsType: 'SPF' },
      { key: 'impDkim', label: 'DKIM', control: 'dnsImportance', dnsType: 'DKIM' },
      { key: 'impDmarc', label: 'DMARC', control: 'dnsImportance', dnsType: 'DMARC' },
      { key: 'impMx', label: 'MX', control: 'dnsImportance', dnsType: 'MX' },
      { key: 'impDnssec', label: 'DNSSEC', control: 'dnsImportance', dnsType: 'DNSSEC' },
      { key: 'impCaa', label: 'CAA', control: 'dnsImportance', dnsType: 'CAA' },
      { key: 'impTxt', label: 'Siteverificatie (TXT)', control: 'dnsImportance', dnsType: 'TXT' },
      { key: 'impCname', label: 'Mail subdomein (CNAME)', control: 'dnsImportance', dnsType: 'CNAME' },
    ],
  },
];
