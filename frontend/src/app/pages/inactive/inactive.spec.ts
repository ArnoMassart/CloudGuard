import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Inactive } from './inactive';
import { provideTranslocoTesting } from '../../testing/transloco-testing';
import { CustomAuthService } from '../../auth/custom-auth-service';

describe('Inactive', () => {
  let component: Inactive;
  let fixture: ComponentFixture<Inactive>;

  beforeEach(async () => {
    localStorage.removeItem('currentLang');

    await TestBed.configureTestingModule({
      imports: [Inactive],
      providers: [
        provideTranslocoTesting(),
        {
          provide: CustomAuthService,
          useValue: {
            logout: vi.fn(),
            isLoggedIn$: of(false),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Inactive);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
