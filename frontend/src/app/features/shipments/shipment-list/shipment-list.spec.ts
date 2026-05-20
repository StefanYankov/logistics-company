import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ShipmentList} from './shipment-list';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {BASE_PATH, EmployeeManagementAPIService, ShipmentAPIService} from '../../../api';
import {of, throwError} from 'rxjs';
import {provideRouter} from '@angular/router';

describe('ShipmentList', () => {
  let component: ShipmentList;
  let fixture: ComponentFixture<ShipmentList>;
  let mockShipmentApi: any;
  let mockEmployeeApi: any;

  beforeEach(async () => {
    TestBed.resetTestingModule();

    mockShipmentApi = {
      getAllShipments: vi.fn(),
      updateShipmentStatus: vi.fn(),
      assignPickup: vi.fn()
    };

    mockEmployeeApi = {
      getAllEmployees: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ShipmentList],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: EmployeeManagementAPIService, useValue: mockEmployeeApi },
        { provide: BASE_PATH, useValue: '' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShipmentList);
    component = fixture.componentInstance;

    // Mock window.confirm to always return true during testing
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should create and load shipments and couriers', () => {
    // Arrange
    const mockShipmentsResponse = {
      content: [{ id: '1', trackingNumber: 'TRK-1', status: 'REGISTERED', appliedAddons: ['Fragile', 'SMS Notification'] }],
      totalElements: 1
    };
    const mockEmployeesResponse = {
      content: [{ id: 'courier-1', firstName: 'John', lastName: 'Doe', applicationRole: 'COURIER' }]
    };
    mockShipmentApi.getAllShipments.mockReturnValue(of(mockShipmentsResponse));
    mockEmployeeApi.getAllEmployees.mockReturnValue(of(mockEmployeesResponse));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component).toBeTruthy();
    expect(component.isLoading()).toBe(false);
    expect(component.shipments().length).toBe(1);
    expect(component.totalElements()).toBe(1);
    expect(component.availableCouriers().length).toBe(1);
  });

  it('should handle error when loading shipments fails', () => {
    // Arrange
    mockShipmentApi.getAllShipments.mockReturnValue(throwError(() => new Error('Error')));
    mockEmployeeApi.getAllEmployees.mockReturnValue(of({ content: [] }));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load shipments.');
  });

  it('should call updateShipmentStatus and reload data', () => {
    // Arrange
    mockShipmentApi.getAllShipments.mockReturnValue(of({ content: [] }));
    mockEmployeeApi.getAllEmployees.mockReturnValue(of({ content: [] }));
    fixture.detectChanges();

    // Arrange
    mockShipmentApi.updateShipmentStatus.mockReturnValue(of({}));

    // Act
    component.updateStatus('shipment-1', 'IN_TRANSIT' as any);

    // Assert
    expect(mockShipmentApi.updateShipmentStatus).toHaveBeenCalledWith('shipment-1', { newStatus: 'IN_TRANSIT' });
    expect(mockShipmentApi.getAllShipments).toHaveBeenCalledTimes(2);
  });

  it('should open and close the assign pickup modal', () => {
    // Arrange
    mockShipmentApi.getAllShipments.mockReturnValue(of({ content: [] }));
    mockEmployeeApi.getAllEmployees.mockReturnValue(of({ content: [] }));
    fixture.detectChanges();

    // Act
    component.openAssignPickupModal('shipment-1');

    // Assert
    expect(component.showAssignPickupModal()).toBe('shipment-1');

    // Act
    component.closeAssignPickupModal();

    // Assert
    expect(component.showAssignPickupModal()).toBe(null);
  });

  it('should call assignPickup and reload data on confirmation', () => {
    // Arrange
    mockShipmentApi.getAllShipments.mockReturnValue(of({ content: [] }));
    mockEmployeeApi.getAllEmployees.mockReturnValue(of({ content: [] }));
    fixture.detectChanges();

    mockShipmentApi.assignPickup.mockReturnValue(of({}));

    // Act
    component.openAssignPickupModal('shipment-1');
    component.selectedCourierForAssignment.set('courier-1');
    component.confirmAssignPickup();

    // Assert
    expect(mockShipmentApi.assignPickup).toHaveBeenCalledWith('shipment-1', 'courier-1');
    expect(mockShipmentApi.getAllShipments).toHaveBeenCalledTimes(2);
  });
});
