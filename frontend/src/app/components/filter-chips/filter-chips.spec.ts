import { FilterChips } from './filter-chips';
import { FilterOption } from '../../models/FilterOption';

describe('FilterChips Unit', () => {
  let component: FilterChips;

  beforeEach(() => {
    component = new FilterChips();
  });

  const mockOption: FilterOption = {
    value: 'test',
    label: 'Test Label',
    count: 5,
    activeClass: 'active-bg',
    inactiveClass: 'inactive-bg',
  };

  it('should emit filterChange when selectFilter is called', () => {
    const spy = vi.spyOn(component.filterChange, 'emit');
    component.selectFilter('test-value');
    expect(spy).toHaveBeenCalledWith('test-value');
  });

  it('should emit refresh when onRefresh is called', () => {
    const spy = vi.spyOn(component.refresh, 'emit');
    component.onRefresh();
    expect(spy).toHaveBeenCalled();
  });

  describe('getButtonClass logic', () => {
    it('should return active class when value matches selectedValue', () => {
      component.selectedValue = 'test';
      const result = component.getButtonClass(mockOption);
      expect(result).toContain('active-bg');
    });

    it('should return pilled inactive class when variant is pilled and not selected', () => {
      component.variant = 'pilled';
      component.selectedValue = 'other';
      const result = component.getButtonClass(mockOption);
      expect(result).toContain('bg-gray-100 text-gray-700');
    });

    it('should return custom inactive class when variant is default and not selected', () => {
      component.variant = 'default';
      component.selectedValue = 'other';
      const result = component.getButtonClass(mockOption);
      expect(result).toContain('inactive-bg');
    });
  });
});
