import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ShipmentDetails } from './shipment-details';
import { ShipmentAPIService } from '../../../api';
import { of } from 'rxjs';

describe('ShipmentDetails', () => {
  let component: ShipmentDetails;
  let fixture: ComponentFixture<ShipmentDetails>;
  let mockShipmentApiService: any;

  beforeEach(async () => {
    mockShipmentApiService = {
      getStaffShipmentDetails: vi.fn().mockReturnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [ShipmentDetails],
      providers: [
        provideRouter([]),
        { provide: ShipmentAPIService, useValue: mockShipmentApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShipmentDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
