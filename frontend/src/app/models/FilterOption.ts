export interface FilterOption<T extends string = string> {
  value: T;
  label: string;
  count: number;
  /** Tailwind classes when active (e.g. 'bg-primary text-white') */
  activeClass: string;
  /** Tailwind classes when inactive (e.g. 'bg-gray-100 text-gray-700') */
  inactiveClass: string;
}