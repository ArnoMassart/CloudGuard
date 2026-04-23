import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkspaceSetup } from './workspace-setup';

describe('WorkspaceSetup', () => {
  let component: WorkspaceSetup;
  let fixture: ComponentFixture<WorkspaceSetup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkspaceSetup],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceSetup);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
