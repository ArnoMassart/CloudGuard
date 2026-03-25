export interface KpiColorSet {
  readonly bg: string;
  readonly icon: string;
  readonly text: string;
}

export const KPI_COLORS = {
  muted: { bg: '#f3f4f6', icon: '#6b7280', text: '#6b7280' } as KpiColorSet,
  okBlue: { bg: '#dbeafe', icon: '#155dfc', text: 'black' } as KpiColorSet,
  okGreen: { bg: '#dbfce7', icon: '#17b04f', text: '#17b04f' } as KpiColorSet,
  okGreenDark: { bg: '#dbfce7', icon: '#17b04f', text: '#166534' } as KpiColorSet,
  alertOrange: { bg: '#ffedd4', icon: '#f54a00', text: '#f54a00' } as KpiColorSet,
  alertRed: { bg: '#ffe2e2', icon: '#e7000b', text: '#e7000b' } as KpiColorSet,
  alertRedDark: { bg: '#fee2e2', icon: '#dc2626', text: '#dc2626' } as KpiColorSet,
  alertPurple: { bg: '#f3e8ff', icon: '#9810fa', text: '#9810fa' } as KpiColorSet,
} as const;

/** Returns ok/muted/alert color set based on count and preference state. */
export function kpiColors(count: number, prefDisabled: boolean, ok: KpiColorSet, alert: KpiColorSet): KpiColorSet {
  if (count === 0) return ok;
  if (prefDisabled) return KPI_COLORS.muted;
  return alert;
}

export interface WarningCheck<K extends string> {
  key: K;
  count: number;
  section: string;
  prefKey: string;
}

/**
 * Evaluates a list of warning checks against the preference facade,
 * returning a warnings record, hasWarnings, and hasMultipleWarnings.
 */
export function evaluateWarnings<K extends string>(
  checks: WarningCheck<K>[],
  isDisabled: (section: string, prefKey: string) => boolean,
): { warnings: Record<K, boolean>; hasWarnings: boolean; hasMultipleWarnings: boolean } {
  const warnings = {} as Record<K, boolean>;
  let activeCount = 0;
  for (const c of checks) {
    const active = c.count > 0 && !isDisabled(c.section, c.prefKey);
    warnings[c.key] = active;
    if (active) activeCount++;
  }
  return { warnings, hasWarnings: activeCount > 0, hasMultipleWarnings: activeCount > 1 };
}
