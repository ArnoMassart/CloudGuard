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
});
