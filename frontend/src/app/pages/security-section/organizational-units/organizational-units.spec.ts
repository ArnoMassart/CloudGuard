import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrganizationalUnits } from './organizational-units';

describe('OrganizationalUnits', () => {
  let component: OrganizationalUnits;
  let fixture: ComponentFixture<OrganizationalUnits>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationalUnits]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrganizationalUnits);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
