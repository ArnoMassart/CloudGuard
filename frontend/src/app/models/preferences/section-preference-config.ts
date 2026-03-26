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
      { key: '2fa', label: 'Gebruikers zonder tweefactorauthenticatie (2FA)' },
      { key: 'activity', label: 'Accountinactiviteit en signalering rond aanmeldgedrag' },
      {
        key: 'groupExternal',
        label: 'Groepen met externe leden of verhoogd risico (open toegang of extern beheer)',
      },
    ],
  },
  {
    section: 'shared-drives',
    label: 'Gedeelde Drives',
    route: '/shared-drives',
    preferences: [
      { key: 'orphan', label: 'Gedeelde drives zonder eigenaar' },
      { key: 'external', label: 'Gedeelde drives met externe deelnemers' },
      {
        key: 'outsideDomain',
        label: 'Delen met gebruikers buiten het organisatiedomein toegestaan',
      },
      {
        key: 'nonMemberAccess',
        label: 'Toegang voor niet-leden van de drive toegestaan',
      },
    ],
  },
  {
    section: 'mobile-devices',
    label: 'Apparaten',
    route: '/devices',
    preferences: [
      { key: 'lockscreen', label: 'Apparaten zonder schermvergrendeling' },
      { key: 'encryption', label: 'Apparaten zonder versleuteling van opslag' },
      { key: 'osVersion', label: 'Niet-ondersteunde of verouderde besturingssysteemversie' },
      { key: 'integrity', label: 'Gecompromitteerde integriteit (root of jailbreak)' },
    ],
  },
  {
    section: 'app-access',
    label: 'App Toegang',
    route: '/app-access',
    preferences: [{ key: 'highRisk', label: 'Applicaties met een hoog risicoprofiel' }],
  },
  {
    section: 'app-passwords',
    label: 'App-wachtwoorden',
    route: '/app-passwords',
    preferences: [{ key: 'appPassword', label: 'Actieve app-specifieke wachtwoorden' }],
  },
  {
    section: 'password-settings',
    label: 'Wachtwoordinstellingen',
    route: '/password-settings',
    preferences: [
      { key: '2sv', label: 'Tweestapsverificatie niet verplicht gesteld' },
      { key: 'length', label: 'Minimale wachtwoordlengte onder twaalf tekens' },
      { key: 'strongPassword', label: 'Beleid voor sterke wachtwoorden niet afgedwongen' },
      { key: 'expiration', label: 'Geen periodieke wachtwoordvernieuwing vereist' },
      { key: 'adminsSecurityKeys', label: 'Beheerdersaccounts zonder beveiligingssleutel' },
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
