import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestRole } from './request-role';

describe('RequestRole', () => {
  let component: RequestRole;
  let fixture: ComponentFixture<RequestRole>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequestRole],
    }).compileComponents();

    fixture = TestBed.createComponent(RequestRole);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
