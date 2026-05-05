import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterShipment } from './register-shipment';
import { provideRouter } from '@angular/router';
import { ClientAPIService, OfficeAPIService, ShipmentAPIService } from '../../../api';
import { of, throwError } from 'rxjs';

describe('RegisterShipment', () => {
  let component: RegisterShipment;
  let fixture: ComponentFixture<RegisterShipment>;
  let mockClientApi: any;
  let mockOfficeApi: any;
  let mockShipmentApi: any;

  beforeEach(async () => {
    // 1. Create spies for all injected API services
    mockClientApi = { getAllClients: vi.fn() };
    mockOfficeApi = { getAllOffices: vi.fn() };
    mockShipmentApi = { registerShipment: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [RegisterShipment],
      providers: [
        // Provide a dummy router configured with the specific route the component tries to navigate to
        provideRouter([{ path: 'app', children: [] }]),
        // 2. Provide the MOCKED services instead of the real ones
        { provide: ClientAPIService, useValue: mockClientApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: ShipmentAPIService, useValue: mockShipmentApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterShipment);
    component = fixture.componentInstance;
  });

  it('should create and load lookups successfully', () => {
    // Arrange: Mock the API responses for the forkJoin in ngOnInit
    const mockClients = { content: [{ id: 'client-1', firstName: 'Test' }] };
    const mockOffices = { content: [{ id: 1, cityName: 'TestCity' }] };
    mockClientApi.getAllClients.mockReturnValue(of(mockClients));
    mockOfficeApi.getAllOffices.mockReturnValue(of(mockOffices));

    // Act: Trigger ngOnInit
    fixture.detectChanges();

    // Assert
    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
    expect(component.clients().length).toBe(1);
    expect(component.offices()[0].cityName).toBe('TestCity');
  });

  it('should handle errors during lookup loading', () => {
    // Arrange: Simulate one of the API calls failing
    mockClientApi.getAllClients.mockReturnValue(throwError(() => new Error('Network Error')));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.isLoadingLookups()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load form data. Please try again.');
  });

  it('should call registerShipment on valid submit', () => {
    // Arrange: First, satisfy the ngOnInit calls
    mockClientApi.getAllClients.mockReturnValue(of({ content: [] }));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    fixture.detectChanges(); // Run ngOnInit

    // Now, set up the form and the mock for the submit action
    component.registerForm.patchValue({
      senderId: 'client-1',
      receiverId: 'client-2',
      type: 'PARCEL' as any,
      weight: 5.5,
      deliveryOfficeId: 1
    });

    // Mock the submission call
    mockShipmentApi.registerShipment.mockReturnValue(of({ id: 'new-shipment-id' }));

    // Act
    component.onSubmit();

    // Assert
    expect(mockShipmentApi.registerShipment).toHaveBeenCalled();
    expect(component.isSubmitting()).toBe(false);
  });
});
