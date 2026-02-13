import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MobileDevices } from './mobile-devices';

describe('MobileDevices', () => {
  let component: MobileDevices;
  let fixture: ComponentFixture<MobileDevices>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MobileDevices]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MobileDevices);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
