import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeliveryPartnerHome } from './delivery-partner-home';

describe('DeliveryPartnerHome', () => {
  let component: DeliveryPartnerHome;
  let fixture: ComponentFixture<DeliveryPartnerHome>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeliveryPartnerHome]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeliveryPartnerHome);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
