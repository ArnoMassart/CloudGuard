import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { PaginationBar } from './pagination-bar';

// Mock vertalingen
const I18N_MOCK = {
  previous: 'Vorige',
  next: 'Volgende',
  page: 'Pagina',
};

class PaginationTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('PaginationBar', () => {
  let component: PaginationBar;
  let fixture: ComponentFixture<PaginationBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginationBar],
      providers: [
        provideTransloco({
          config: { availableLangs: ['en'], defaultLang: 'en' },
          loader: PaginationTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginationBar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start at page 1 with empty history', () => {
    expect(component.currentPage()).toBe(1);
    expect((component as any).tokenHistory).toEqual([]);
  });

  describe('nextPage', () => {
    it('should navigate to next page and emit token if available', () => {
      // Mock de output emit
      const emitSpy = vi.spyOn(component.loadData, 'emit');

      // Zet de input signal (Angular 17+ signal input)
      fixture.componentRef.setInput('nextPageToken', 'token-1');

      component.nextPage();

      expect(component.currentPage()).toBe(2);
      expect((component as any).tokenHistory).toEqual(['token-1']);
      expect(emitSpy).toHaveBeenCalledWith('token-1');
    });

    it('should not navigate if nextPageToken is null', () => {
      const emitSpy = vi.spyOn(component.loadData, 'emit');
      fixture.componentRef.setInput('nextPageToken', null);

      component.nextPage();

      expect(component.currentPage()).toBe(1);
      expect(emitSpy).not.toHaveBeenCalled();
    });
  });

  describe('prevPage', () => {
    it('should navigate back and emit the previous token from history', () => {
      const emitSpy = vi.spyOn(component.loadData, 'emit');

      // Setup: we zijn op pagina 3
      // Pagina 1 (start) -> Pagina 2 (token-A) -> Pagina 3 (token-B)
      fixture.componentRef.setInput('nextPageToken', 'token-A');
      component.nextPage();
      fixture.componentRef.setInput('nextPageToken', 'token-B');
      component.nextPage();

      expect(component.currentPage()).toBe(3);
      emitSpy.mockClear();

      // Act: Terug naar pagina 2
      component.prevPage();

      expect(component.currentPage()).toBe(2);
      // Het token van pagina 2 was 'token-A' (de laatste in de resterende historie)
      expect(emitSpy).toHaveBeenCalledWith('token-A');
    });

    it('should emit undefined when going back to page 1', () => {
      const emitSpy = vi.spyOn(component.loadData, 'emit');

      fixture.componentRef.setInput('nextPageToken', 'token-A');
      component.nextPage(); // naar pagina 2
      emitSpy.mockClear();

      component.prevPage(); // terug naar pagina 1

      expect(component.currentPage()).toBe(1);
      expect(emitSpy).toHaveBeenCalledWith(undefined);
    });

    it('should do nothing if already on page 1', () => {
      const emitSpy = vi.spyOn(component.loadData, 'emit');

      component.prevPage();

      expect(component.currentPage()).toBe(1);
      expect(emitSpy).not.toHaveBeenCalled();
    });
  });

  describe('reset', () => {
    it('should reset currentPage and tokenHistory', () => {
      // Setup staat
      fixture.componentRef.setInput('nextPageToken', 'token-A');
      component.nextPage();
      expect(component.currentPage()).toBe(2);

      // Act
      component.reset();

      expect(component.currentPage()).toBe(1);
      expect((component as any).tokenHistory).toEqual([]);
    });
  });
});
