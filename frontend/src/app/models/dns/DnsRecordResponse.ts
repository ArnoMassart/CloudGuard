import { DnsRecord } from "./DnsRecord";
import type { SecurityScoreBreakdown } from '../password/PasswordSettings';

export interface DnsRecordResponse {
  domain: string;
  rows: DnsRecord[];
  securityScore: number;
  securityScoreBreakdown?: SecurityScoreBreakdown;
}