import { DataAccess } from './DataAccess';

export type AggregatedAppDto = {
  id: string;
  name: string;
  appType: string;
  appSource: string;
  isThirdParty: boolean;
  isAnonymous: boolean;
  isHighRisk: boolean;
  totalUsers: number;
  exposurePercentage: number;
  scopeCount: number;
  dataAccess: DataAccess[];
  highRiskCount: number;
};
