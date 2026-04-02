import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FilterChips } from './filter-chips';
import { By } from '@angular/platform-browser';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('FilterChips Integration', () => {
  let component: FilterChips;
  let fixture: ComponentFixture<FilterChips>;

  const mockOptions = [
    { value: 'all', label: 'all', count: 10, activeClass: 'act', inactiveClass: 'in' },
    { value: 'high', label: 'high', count: 2, activeClass: 'act', inactiveClass: 'in' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterChips],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterChips);
    component = fixture.componentInstance;
    component.options = mockOptions;
    fixture.detectChanges();
  });

  it('should render the correct amount of filter chips', () => {
    const buttons = fixture.debugElement.queryAll(By.css('button'));
    // Er zijn 2 opties, dus we verwachten 2 buttons (zolang showRefresh false is)
    expect(buttons.length).toBe(2);
  });

  it('should display the label and counts correctly', () => {
    // GEBRUIK DIT: setInput zorgt voor een correcte afhandeling van de verandering
    fixture.componentRef.setInput('label', 'Zoeken:');

    // Nu pas detectChanges aanroepen
    fixture.detectChanges();

    const labelEl = fixture.debugElement.query(By.css('span')).nativeElement;
    expect(labelEl.textContent).toContain('Zoeken:');

    const firstChip = fixture.debugElement.query(By.css('button')).nativeElement;
    expect(firstChip.textContent).toContain('All (10)');
  });

  it('should call selectFilter when a chip is clicked', () => {
    const spy = vi.spyOn(component, 'selectFilter');
    const secondChip = fixture.debugElement.queryAll(By.css('button'))[1];

    secondChip.nativeElement.click();

    expect(spy).toHaveBeenCalledWith('high');
  });

  describe('Refresh Button', () => {
    it('should not show refresh button by default', () => {
      expect(fixture.debugElement.query(By.css('.animate-spin'))).toBeFalsy();
    });

    it('should show and handle refresh button when showRefresh is true', () => {
      fixture.componentRef.setInput('showRefresh', true);
      fixture.detectChanges();

      const refreshSpy = vi.spyOn(component.refresh, 'emit');
      // De refresh button is nu de laatste button in de DOM
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      const refreshBtn = buttons[buttons.length - 1];

      refreshBtn.nativeElement.click();
      expect(refreshSpy).toHaveBeenCalled();
    });

    it('should disable the refresh button and show spinner when isRefreshing is true', () => {
      fixture.componentRef.setInput('showRefresh', true);
      fixture.componentRef.setInput('isRefreshing', true);
      fixture.detectChanges();

      const refreshBtn = fixture.debugElement.queryAll(By.css('button')).at(-1);
      expect(refreshBtn?.nativeElement.disabled).toBe(true);

      const spinner = fixture.debugElement.query(By.css('.animate-spin'));
      expect(spinner).toBeTruthy();
    });
  });
});
