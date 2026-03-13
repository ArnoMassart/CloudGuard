export interface OrgUnitPolicyDto {
    key: string;
    title: string;
    description: string;
    status: string;
    statusClass: string;
    baseExplanation?: string;
    inheritanceExplanation?: string;
    inherited: boolean;
    source: string;
    settingsLinkText?: string;
    adminLink?: string;
    details?: string;
}