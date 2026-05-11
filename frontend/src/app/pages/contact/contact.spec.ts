import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { Contact } from './contact';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('Contact', () => {
  let fixture: ComponentFixture<Contact>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    vi.useFakeTimers();
    await TestBed.configureTestingModule({
      imports: [Contact],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(Contact);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    vi.useRealTimers();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('exposes chat URL for Intercom link', () => {
    expect(fixture.componentInstance.chatUrl).toBe('https://cloudmen.com/pages/contact#intercom');
  });

  it('defines topic options for the form', () => {
    expect(fixture.componentInstance.topics.length).toBeGreaterThanOrEqual(4);
    expect(fixture.componentInstance.topics.some((t) => t.value === 'support')).toBe(true);
  });

  it('onSubmit posts to backend contact endpoint when form is valid', () => {
    const cmp = fixture.componentInstance;
    cmp.contactForm.patchValue({
      topic: 'support',
      subject: 'Hello support',
      message: 'This is long enough message.',
    });

    cmp.onSubmit();

    const req = httpMock.expectOne('/api/contact/send');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush('ok');

    expect(cmp.isSuccess).toBe(true);
    expect(cmp.isSubmitting).toBe(false);
  });

  it('onSubmit does not send when form is invalid', () => {
    const cmp = fixture.componentInstance;
    cmp.contactForm.patchValue({
      topic: 'support',
      subject: 'ab',
      message: 'short',
    });

    cmp.onSubmit();

    httpMock.expectNone('/api/contact/send');
    expect(cmp.isSubmitting).toBe(false);
  });

  it('clears success flag after timeout', () => {
    const cmp = fixture.componentInstance;
    cmp.contactForm.patchValue({
      topic: 'support',
      subject: 'Hello support',
      message: 'This is long enough message.',
    });

    cmp.onSubmit();
    httpMock.expectOne('/api/contact/send').flush('ok');

    expect(cmp.isSuccess).toBe(true);

    vi.advanceTimersByTime(5000);
    expect(cmp.isSuccess).toBe(false);
  });
});
