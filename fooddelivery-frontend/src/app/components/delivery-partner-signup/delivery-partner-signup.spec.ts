import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeliveryPartnerSignup } from './delivery-partner-signup';

describe('DeliveryPartnerSignup', () => {
  let component: DeliveryPartnerSignup;
  let fixture: ComponentFixture<DeliveryPartnerSignup>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeliveryPartnerSignup]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeliveryPartnerSignup);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
