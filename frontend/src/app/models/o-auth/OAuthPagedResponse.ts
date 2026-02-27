import { AggregatedAppDto } from './AggregatedAppDto';

export type OAuthPagedResponse = {
  apps: AggregatedAppDto[];
  nextPageToken: string;
};
