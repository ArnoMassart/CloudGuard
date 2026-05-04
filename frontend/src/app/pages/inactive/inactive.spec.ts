import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Inactive } from './inactive';

describe('Inactive', () => {
  let component: Inactive;
  let fixture: ComponentFixture<Inactive>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inactive],
    }).compileComponents();

    fixture = TestBed.createComponent(Inactive);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
