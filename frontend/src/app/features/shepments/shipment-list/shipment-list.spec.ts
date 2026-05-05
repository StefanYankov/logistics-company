import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ShipmentList } from './shipment-list';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ShipmentAPIService } from '../../../api';
import { BASE_PATH } from '../../../api';
import { of, throwError } from 'rxjs';

describe('ShipmentList', () => {
  let component: ShipmentList;
  let fixture: ComponentFixture<ShipmentList>;
  let mockShipmentApi: any;

  beforeEach(async () => {
    TestBed.resetTestingModule();

    mockShipmentApi = {
      getAllShipments: vi.fn(),
      updateShipmentStatus: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ShipmentList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: BASE_PATH, useValue: '' }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShipmentList);
    component = fixture.componentInstance;
  });

  it('should create and load shipments', () => {
    // Arrange
    const mockResponse = {
      content: [{ id: '1', trackingNumber: 'TRK-1', status: 'REGISTERED' }],
      totalElements: 1
    };
    mockShipmentApi.getAllShipments.mockReturnValue(of(mockResponse));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component).toBeTruthy();
    expect(component.isLoading()).toBe(false);
    expect(component.shipments().length).toBe(1);
    expect(component.totalElements()).toBe(1);
  });

  it('should handle error when loading shipments fails', () => {
    // Arrange
    mockShipmentApi.getAllShipments.mockReturnValue(throwError(() => new Error('Error')));

    // Act
    fixture.detectChanges();

    // Assert
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load shipments.');
  });

  it('should call updateShipmentStatus and reload data', () => {
    // Arrange
    mockShipmentApi.getAllShipments.mockReturnValue(of({ content: [] }));
    fixture.detectChanges();

    // Arrange
    mockShipmentApi.updateShipmentStatus.mockReturnValue(of({}));

    // Act
    component.updateStatus('shipment-1', 'IN_TRANSIT' as any);

    // Assert
    expect(mockShipmentApi.updateShipmentStatus).toHaveBeenCalledWith('shipment-1', { newStatus: 'IN_TRANSIT' });
    expect(mockShipmentApi.getAllShipments).toHaveBeenCalledTimes(2);
  });
});
