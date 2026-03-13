export interface DnsRecord {
  type: string;
  name: string;
  values: string[];
  status: string;
  importance?: string;
  message?: string;
}