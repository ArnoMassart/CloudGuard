export interface AppPasswordOverviewResponse {
    allowed: boolean;
    totalAppPasswords: number;
    totalHighRiskAppPasswords: number;
    securityScore: number;
}