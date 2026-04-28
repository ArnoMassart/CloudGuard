import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '@auth0/auth0-angular';
import { WorkspaceSetup } from './workspace-setup';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('WorkspaceSetup', () => {
  let component: WorkspaceSetup;
  let fixture: ComponentFixture<WorkspaceSetup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkspaceSetup],
      providers: [
        { provide: AuthService, useValue: { logout: () => undefined } },
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceSetup);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
