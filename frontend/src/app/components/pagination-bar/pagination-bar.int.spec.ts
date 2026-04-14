import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginationBar } from './pagination-bar';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { By } from '@angular/platform-browser';

// Mock vertalingen voor de pipe
const I18N_MOCK = { next: 'Next', previous: 'Previous', page: 'Page' };
class TestLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('PaginationBar Integration', () => {
  let component: PaginationBar;
  let fixture: ComponentFixture<PaginationBar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginationBar],
      providers: [
        provideTransloco({
          config: { availableLangs: ['en'], defaultLang: 'en' },
          loader: TestLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PaginationBar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should display the correct initial page number in the UI', () => {
    const pageText = fixture.debugElement.query(By.css('.text-sm')).nativeElement.textContent;
    // We verwachten dat "1" ergens in de tekst staat (bijv. "Page 1")
    expect(pageText).toContain('1');
  });

  describe('Full Navigation Flow', () => {
    it('should navigate to page 2 when the next button is clicked', () => {
      // 1. Setup: geef een token mee via de Signal Input
      fixture.componentRef.setInput('nextPageToken', 'token-val-1');
      fixture.detectChanges();

      // Spioneer op de output
      const emitSpy = vi.spyOn(component.loadData, 'emit');

      // 2. Act: Zoek de "Next" knop (meestal de tweede knop of op basis van icon/tekst)
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      const nextButton = buttons[buttons.length - 1]; // Laatste knop is meestal 'Next'

      nextButton.nativeElement.click();
      fixture.detectChanges();

      // 3. Assert: Is de UI geüpdatet?
      const pageText = fixture.debugElement.query(By.css('.text-sm')).nativeElement.textContent;
      expect(pageText).toContain('2');

      // Is de data geëmit naar de parent?
      expect(emitSpy).toHaveBeenCalledWith('token-val-1');
    });

    it('should navigate back to page 1 and emit undefined when Previous is clicked', () => {
      const emitSpy = vi.spyOn(component.loadData, 'emit');

      // Ga eerst naar pagina 2
      fixture.componentRef.setInput('nextPageToken', 'token-val-1');
      component.nextPage();
      fixture.detectChanges();
      expect(component.currentPage()).toBe(2);

      // Klik nu op de 'Previous' knop (meestal de eerste knop)
      const prevButton = fixture.debugElement.queryAll(By.css('button'))[0];
      prevButton.nativeElement.click();
      fixture.detectChanges();

      // Assert
      expect(component.currentPage()).toBe(1);
      const pageText = fixture.debugElement.query(By.css('.text-sm')).nativeElement.textContent;
      expect(pageText).toContain('1');

      // Terug naar pagina 1 betekent dat we de initiële data laden (zonder token)
      expect(emitSpy).toHaveBeenCalledWith(undefined);
    });
  });

  describe('Loading & Disabled States', () => {
    it('should disable buttons when isLoading is true', () => {
      fixture.componentRef.setInput('isLoading', true);
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));
      buttons.forEach((btn) => {
        expect(btn.nativeElement.disabled).toBe(true);
      });
    });

    it('should disable Next button if no nextPageToken is present', () => {
      fixture.componentRef.setInput('nextPageToken', null);
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));
      const nextButton = buttons[buttons.length - 1];

      expect(nextButton.nativeElement.disabled).toBe(true);
    });
  });

  it('should reset state completely when reset() is called', () => {
    // Breng component in een bepaalde staat
    fixture.componentRef.setInput('nextPageToken', 'abc');
    component.nextPage();
    fixture.detectChanges();

    expect(component.currentPage()).toBe(2);

    // Reset
    component.reset();
    fixture.detectChanges();

    expect(component.currentPage()).toBe(1);
    const pageText = fixture.debugElement.query(By.css('.text-sm')).nativeElement.textContent;
    expect(pageText).toContain('1');
  });
});
