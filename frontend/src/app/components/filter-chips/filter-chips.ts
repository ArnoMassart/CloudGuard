import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { FilterOption } from '../../models/FilterOption';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-filter-chips',
  imports: [LucideAngularModule, TranslocoPipe],
  templateUrl: './filter-chips.html',
  styleUrl: './filter-chips.css',
})
export class FilterChips {
  readonly Icons = AppIcons;

  @Input() options: FilterOption[] = [];
  @Input() selectedValue: string = 'all';
  @Input() label: string = 'Filter:';
  @Input() variant: 'pilled' | 'default' = 'pilled';
  @Input() showRefresh: boolean = false;
  @Input() isRefreshing: boolean = false;
  @Input() refreshLabel: string = 'refresh';

  private readonly pilledInactiveClass = 'bg-gray-100 text-gray-700 hover:bg-gray-200';

  @Output() filterChange = new EventEmitter<string>();
  @Output() refresh = new EventEmitter<void>();

  selectFilter(value: string): void {
    this.filterChange.emit(value);
  }

  getButtonClass(opt: FilterOption): string {
    const base = 'px-4 py-2 rounded-full text-sm font-medium transition-colors';
    const isActive = this.selectedValue === opt.value;
    const inactiveClass = this.variant === 'pilled' ? this.pilledInactiveClass : opt.inactiveClass;
    return `${base} ${isActive ? opt.activeClass : inactiveClass}`;
  }

  onRefresh(): void {
    this.refresh.emit();
  }
}
