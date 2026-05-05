import type { SecurityScoreBreakdown } from '../password/PasswordSettings';

export interface AppPasswordOverviewResponse {
    allowed: boolean;
    totalAppPasswords: number;
    usersWithAppPasswords: number;
    securityScore: number | null;
    securityScoreBreakdown?: SecurityScoreBreakdown;
}