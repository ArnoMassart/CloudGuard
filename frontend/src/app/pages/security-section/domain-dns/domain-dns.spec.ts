import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DomainDns } from './domain-dns';

describe('DomainDns', () => {
  let component: DomainDns;
  let fixture: ComponentFixture<DomainDns>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DomainDns]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DomainDns);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
