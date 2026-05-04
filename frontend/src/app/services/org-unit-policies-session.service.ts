import { Injectable } from '@angular/core';
import { OrgUnitPolicyDto } from '../models/org-unit/OrgUnitPolicyDto';

/**
 * In-memory, app-session cache for GET /google/org-units/policies per normalized OU path.
 * Survives navigating away from the page; cleared on org-unit refresh and on language change.
 */
@Injectable({ providedIn: 'root' })
export class OrgUnitPoliciesSessionService {
  readonly #byPath = new Map<string, OrgUnitPolicyDto[]>();

  get(orgUnitPath: string): OrgUnitPolicyDto[] | undefined {
    return this.#byPath.get(this.#normalizePath(orgUnitPath));
  }

  put(orgUnitPath: string, policies: OrgUnitPolicyDto[]): void {
    this.#byPath.set(this.#normalizePath(orgUnitPath), policies);
  }

  clear(): void {
    this.#byPath.clear();
  }

  #normalizePath(orgUnitPath: string): string {
    const t = orgUnitPath?.trim();
    return !t ? '/' : t;
  }
}
