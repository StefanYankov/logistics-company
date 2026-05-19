import {ComponentFixture, TestBed} from '@angular/core/testing';
import {CourierDashboard} from './courier-dashboard';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {AuthService} from '../../../shared/auth.service';
import {ShipmentAPIService} from '../../../api';
import {of, throwError} from 'rxjs';
import {RouterTestingModule} from '@angular/router/testing';

describe('CourierDashboard', () => {
  let component: CourierDashboard;
  let fixture: ComponentFixture<CourierDashboard>;
  let mockAuthService: any;
  let mockShipmentApi: any;

  beforeEach(async () => {
    mockAuthService = {
      getDecodedToken: vi.fn()
    };

    mockShipmentApi = {
      getMyDeliveries: vi.fn(),
      getMyPickups: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [CourierDashboard, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ShipmentAPIService, useValue: mockShipmentApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CourierDashboard);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should fetch and display courier tasks for a COURIER', () => {
    const mockUserId = 'courier-uuid-123';
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_COURIER', userId: mockUserId });

    const mockDeliveries = { content: [{ id: 'delivery-1', trackingNumber: 'TRK-DELIVERY' }] };
    const mockPickups = { content: [{ id: 'pickup-1', trackingNumber: 'TRK-PICKUP' }] };

    mockShipmentApi.getMyDeliveries.mockReturnValue(of(mockDeliveries));
    mockShipmentApi.getMyPickups.mockReturnValue(of(mockPickups));

    fixture.detectChanges();

    expect(mockShipmentApi.getMyDeliveries).toHaveBeenCalled();
    expect(mockShipmentApi.getMyPickups).toHaveBeenCalled();

    expect(component.isLoading()).toBe(false);
    expect(component.deliveryTasks().length).toBe(1);
    expect(component.deliveryTasks()[0].trackingNumber).toBe('TRK-DELIVERY');
    expect(component.pickupTasks().length).toBe(1);
    expect(component.pickupTasks()[0].trackingNumber).toBe('TRK-PICKUP');
  });

  it('should show an error if the user is not a courier', () => {
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLIENT', userId: '123' });

    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('You are not authorized to view this page.');
    expect(mockShipmentApi.getMyDeliveries).not.toHaveBeenCalled();
  });

  it('should handle API errors gracefully', () => {
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_COURIER', userId: '123' });

    mockShipmentApi.getMyDeliveries.mockReturnValue(throwError(() => new Error('Network failed')));
    mockShipmentApi.getMyPickups.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load courier tasks. Please try again.');
  });
});
