import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedDrives } from './shared-drives';

describe('SharedDrives', () => {
  let component: SharedDrives;
  let fixture: ComponentFixture<SharedDrives>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedDrives]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SharedDrives);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
