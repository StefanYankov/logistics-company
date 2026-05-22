import {ComponentFixture, TestBed} from '@angular/core/testing';

import {ShipmentFilterReport} from './shipment-filter-report';

describe('ShipmentFilterReport', () => {
  let component: ShipmentFilterReport;
  let fixture: ComponentFixture<ShipmentFilterReport>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShipmentFilterReport],
    }).compileComponents();

    fixture = TestBed.createComponent(ShipmentFilterReport);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
