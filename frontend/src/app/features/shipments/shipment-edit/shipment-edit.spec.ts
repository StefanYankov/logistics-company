import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ShipmentEdit} from './shipment-edit';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {ActivatedRoute, Router} from '@angular/router';
import {of, throwError} from 'rxjs';
import {
  CityAPIService,
  OfficeAPIService,
  ServiceCatalogAPIService,
  ShipmentAPIService,
  StaffShipmentViewDto
} from '../../../api';
import {FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

describe('ShipmentEdit', () => {
  let component: ShipmentEdit;
  let fixture: ComponentFixture<ShipmentEdit>;
  let mockShipmentApi: any;
  let mockOfficeApi: any;
  let mockCityApi: any;
  let mockServiceCatalogApi: any;
  let mockRouter: any;

  const mockShipment: StaffShipmentViewDto = {
    id: 'shipment-1',
    trackingNumber: 'TRK-123',
    status: 'REGISTERED',
    type: 'PARCEL',
    weight: 10.5,
    paidBy: 'SENDER',
    isPaid: false,
    receiverName: 'Jane Doe',
    receiverPhone: '0888123456',
    senderId: 'sender-1',
    senderName: 'John Doe',
    senderPhone: '0888654321',
    deliveryOfficeId: 1,
    deliveryOfficeName: 'Sofia Office',
    appliedAddons: ['Fragile']
  };

  const mockOffices = { content: [{ id: 1, fullAddress: 'Sofia Office', cityName: 'Sofia' }] };
  const mockCities = { content: [{ id: 1, name: 'Sofia', postcode: '1000' }] };
  const mockServices = [{ id: 1, name: 'Fragile', pricingType: 'FIXED_AMOUNT', pricingValue: 5.0 }];

  beforeEach(async () => {
    mockShipmentApi = {
      getStaffShipmentDetails: vi.fn().mockReturnValue(of(mockShipment)),
      updateShipment: vi.fn().mockReturnValue(of(mockShipment))
    };
    mockOfficeApi = {
      getAllOffices: vi.fn().mockReturnValue(of(mockOffices))
    };
    mockCityApi = {
      getAllCities: vi.fn().mockReturnValue(of(mockCities))
    };
    mockServiceCatalogApi = {
      getAllServices: vi.fn().mockReturnValue(of(mockServices))
    };
    mockRouter = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ShipmentEdit, ReactiveFormsModule, FormsModule, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: mockCityApi },
        { provide: ServiceCatalogAPIService, useValue: mockServiceCatalogApi },
        { provide: Router, useValue: mockRouter },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => {
                  if (key === 'id') {
                    return 'shipment-1';
                  }
                  return null;
                }
              }
            },
            paramMap: of({ get: (key: string) => (key === 'id' ? 'shipment-1' : null) })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShipmentEdit);
    component = fixture.componentInstance;
    fixture.detectChanges(); // Trigger ngOnInit
  });

  it('should create and load shipment data', () => {
    expect(component).toBeTruthy();
    expect(mockShipmentApi.getStaffShipmentDetails).toHaveBeenCalledWith('shipment-1');
    expect(component.isLoading()).toBe(false);
    expect(component.editForm.get('receiverName')?.value).toBe(mockShipment.receiverName);
    expect(component.selectedServiceIds()).toEqual([1]); // Fragile service ID
  });

  it('should toggle destination validators when type changes', () => {
    // Initially, it's OFFICE
    expect(component.editForm.get('deliveryOfficeId')?.hasValidator(Validators.required)).toBe(true);
    expect(component.editForm.get('deliveryStreet')?.hasValidator(Validators.required)).toBe(false);

    // Change to ADDRESS
    component.editForm.get('destinationType')?.setValue('ADDRESS');
    fixture.detectChanges();

    expect(component.editForm.get('deliveryOfficeId')?.hasValidator(Validators.required)).toBe(false);
    expect(component.editForm.get('deliveryStreet')?.hasValidator(Validators.required)).toBe(true);
    expect(component.editForm.get('deliveryCityId')?.hasValidator(Validators.required)).toBe(true);

    // Change back to OFFICE
    component.editForm.get('destinationType')?.setValue('OFFICE');
    fixture.detectChanges();

    expect(component.editForm.get('deliveryOfficeId')?.hasValidator(Validators.required)).toBe(true);
    expect(component.editForm.get('deliveryStreet')?.hasValidator(Validators.required)).toBe(false);
  });

  it('should submit updated shipment data', () => {
    component.editForm.patchValue({
      receiverName: 'New Name',
      weight: 12.0
    });
    component.selectedServiceIds.set([1, 2]); // Add another service

    component.onSubmit();

    expect(mockShipmentApi.updateShipment).toHaveBeenCalledWith('shipment-1', {
      type: mockShipment.type,
      weight: 12.0,
      paidBy: mockShipment.paidBy,
      isPaid: mockShipment.isPaid,
      receiverName: 'New Name',
      receiverPhone: mockShipment.receiverPhone,
      receiverEmail: undefined,
      selectedServiceIds: [1, 2],
      deliveryOfficeId: mockShipment.deliveryOfficeId,
      deliveryAddress: undefined
    });
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/app/shipments', 'shipment-1']);
  });

  it('should handle error during shipment update', () => {
    mockShipmentApi.updateShipment.mockReturnValue(throwError(() => ({ status: 400, error: { detail: 'Validation Error' } })));
    component.editForm.patchValue({ receiverName: 'New Name' });

    component.onSubmit();

    expect(component.isSubmitting()).toBe(false);
    expect(component.errorMessage()).toBe('Validation Error');
  });
});
