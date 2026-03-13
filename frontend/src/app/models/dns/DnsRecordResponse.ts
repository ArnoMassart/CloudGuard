import { DnsRecord } from "./DnsRecord";

export interface DnsRecordResponse {
  domain: string;
  rows: DnsRecord[];
  securityScore: number;
}