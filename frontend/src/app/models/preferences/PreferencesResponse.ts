export type PreferencesResponse = {
  preferences: Record<string, boolean>;
  dnsImportance: Record<string, string>;
  dnsImportanceOverrideTypes: string[];
};
