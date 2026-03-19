import type { SecurityScoreBreakdown } from '../password/PasswordSettings';

export interface AppPasswordOverviewResponse {
    allowed: boolean;
    totalAppPasswords: number;
    totalHighRiskAppPasswords: number;
    securityScore: number;
    securityScoreBreakdown?: SecurityScoreBreakdown;
}