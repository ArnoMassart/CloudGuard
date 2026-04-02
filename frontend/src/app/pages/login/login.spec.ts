import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '@auth0/auth0-angular';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { Login } from './login';

// Simpele mock voor eventuele vertalingen in de login pagina
const I18N_MOCK: Record<string, string> = {
  'login.title': 'Login',
};

class LoginTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  // Mock voor Auth0
  let authServiceMock: {
    loginWithRedirect: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      loginWithRedirect: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
          },
          loader: LoginTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('loginWithGoogle', () => {
    it('sets sessionStorage flag and calls Auth0 loginWithRedirect with correct parameters', () => {
      // Mock de native browser sessionStorage API
      const sessionStorageSpy = vi.spyOn(Storage.prototype, 'setItem');

      component.loginWithGoogle();

      // 1. Controleer of de flag netjes in de sessie is gezet
      expect(sessionStorageSpy).toHaveBeenCalledWith('auth0_redirect_pending', '1');

      // 2. Controleer of Auth0 wordt aangeroepen met exact de juiste configuratie
      expect(authServiceMock.loginWithRedirect).toHaveBeenCalledWith({
        appState: { target: '/callback' },
        authorizationParams: {
          connection: 'google-oauth2',
          prompt: 'select_account',
          // Omdat locatie afhankelijk is van je testrunner poort, bouwen we de string dynamisch op in de check
          redirect_uri: globalThis.location.origin + '/callback',
        },
      });

      // Opruimen zodat andere testen geen last hebben van onze spy
      sessionStorageSpy.mockRestore();
    });
  });
});
