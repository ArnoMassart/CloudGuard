import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SecurityGauge } from './security-gauge';

describe('SecurityGauge', () => {
  let component: SecurityGauge;
  let fixture: ComponentFixture<SecurityGauge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SecurityGauge],
    }).compileComponents();

    fixture = TestBed.createComponent(SecurityGauge);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
