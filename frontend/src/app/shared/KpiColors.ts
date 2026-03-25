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
