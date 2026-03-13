export interface OrgUnitNodeDto {
  id: string;
  name: string;
  orgUnitPath?: string;
  userCount: number;
  children?: OrgUnitNodeDto[];
  root?: boolean;
  isRoot?: boolean;
}