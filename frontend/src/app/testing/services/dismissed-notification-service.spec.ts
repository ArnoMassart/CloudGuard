import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DismissNotificationRequest } from '../../models/notification/DismissNotificationRequest';
import { DismissedNotificationService } from '../../services/dismissed-notification-service';

describe('DismissedNotificationService', () => {
  let service: DismissedNotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DismissedNotificationService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DismissedNotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('markAsDismissed POSTs body with credentials', () => {
    const request: DismissNotificationRequest = {
      source: 'security',
      notificationType: 'user-control',
      sourceLabel: 'Users',
      sourceRoute: '/users',
      title: 'T',
      description: 'D',
      severity: 'critical',
    };
    service.markAsDismissed(request).subscribe();

    const req = httpMock.expectOne((r) => r.url === '/api/notifications/dismissed');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.body).toEqual(request);
    req.flush(null);
  });

  it('unDismiss DELETEs with query params', () => {
    service.unDismiss('src', 'type-a').subscribe();

    const req = httpMock.expectOne(
      (r) =>
        r.url === '/api/notifications/dismissed' &&
        r.params.get('source') === 'src' &&
        r.params.get('notificationType') === 'type-a',
    );
    expect(req.request.method).toBe('DELETE');
    expect(req.request.withCredentials).toBe(true);
    req.flush(null);
  });

  it('markAsDismissed propagates HTTP errors', () => {
    const errSpy = vi.fn();
    const request: DismissNotificationRequest = {
      source: 's',
      notificationType: 't',
      sourceLabel: '',
      sourceRoute: '',
      title: '',
      description: '',
      severity: 'info',
    };
    service.markAsDismissed(request).subscribe({ error: errSpy });

    const req = httpMock.expectOne((r) => r.url === '/api/notifications/dismissed');
    req.flush('nope', { status: 400, statusText: 'Bad Request' });

    expect(errSpy).toHaveBeenCalled();
  });

  it('unDismiss propagates HTTP errors', () => {
    const errSpy = vi.fn();
    service.unDismiss('s', 't').subscribe({ error: errSpy });

    const req = httpMock.expectOne((r) => r.url === '/api/notifications/dismissed');
    req.error(new ProgressEvent('Network error'));

    expect(errSpy).toHaveBeenCalled();
  });
});
