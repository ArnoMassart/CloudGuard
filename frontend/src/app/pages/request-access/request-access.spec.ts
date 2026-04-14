import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestAccess } from './request-access';

describe('RequestAccess', () => {
  let component: RequestAccess;
  let fixture: ComponentFixture<RequestAccess>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequestAccess],
    }).compileComponents();

    fixture = TestBed.createComponent(RequestAccess);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
