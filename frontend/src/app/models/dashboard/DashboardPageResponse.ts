import { DashboardScores } from './DashboardScores';

export type DashboardPageResponse = {
  scores: DashboardScores;
  overallScore: number | null;
  lastUpdated: string;
};
