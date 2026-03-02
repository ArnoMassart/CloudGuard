import { AggregatedAppDto } from './AggregatedAppDto';

export type OAuthPagedResponse = {
  apps: AggregatedAppDto[];
  nextPageToken: string;
  allFilteredApps: number;
  allHighRiskApps: number;
  allNotHighRiskApps: number;
};
