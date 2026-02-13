import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LicensesBilling } from './licenses-billing';

describe('LicensesBilling', () => {
  let component: LicensesBilling;
  let fixture: ComponentFixture<LicensesBilling>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LicensesBilling]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LicensesBilling);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
