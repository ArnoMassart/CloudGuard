import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MobileDevicesTopCard } from './mobile-devices-top-card';

describe('MobileDevicesTopCard', () => {
  let component: MobileDevicesTopCard;
  let fixture: ComponentFixture<MobileDevicesTopCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MobileDevicesTopCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MobileDevicesTopCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
