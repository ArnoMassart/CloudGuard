import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { RequestRole } from './request-role';
import { provideTranslocoTesting } from '../../testing/transloco-testing';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { UserService } from '../../services/user-service';

describe('RequestRole', () => {
  let component: RequestRole;
  let fixture: ComponentFixture<RequestRole>;

  beforeEach(async () => {
    localStorage.removeItem('currentLang');

    await TestBed.configureTestingModule({
      imports: [RequestRole],
      providers: [
        provideTranslocoTesting(),
        {
          provide: CustomAuthService,
          useValue: {
            logout: vi.fn(),
            isLoggedIn$: of(false),
          },
        },
        {
          provide: UserService,
          useValue: {
            requestAccess: vi.fn(() => of('')),
            getRequestSent: vi.fn(() => of(false)),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RequestRole);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
