import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NotificationFeedbackService } from '../../services/notification-feedback-service';

describe('NotificationFeedbackService', () => {
  let service: NotificationFeedbackService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NotificationFeedbackService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NotificationFeedbackService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('submitFeedback POSTs JSON with credentials', () => {
    service.submitFeedback('s', 't', 'hello').subscribe();

    const req = httpMock.expectOne((r) => r.url === '/api/notifications/feedback');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.body).toEqual({ source: 's', notificationType: 't', feedbackText: 'hello' });
    req.flush(null);
  });

  it('submitFeedback propagates HTTP errors to subscriber', () => {
    const nextSpy = vi.fn();
    const errorSpy = vi.fn();
    service.submitFeedback('a', 'b', 'c').subscribe({ next: nextSpy, error: errorSpy });

    const req = httpMock.expectOne((r) => r.url === '/api/notifications/feedback');
    req.flush('fail', { status: 500, statusText: 'Server Error' });

    expect(nextSpy).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalled();
  });
});
