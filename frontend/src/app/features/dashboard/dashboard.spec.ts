import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Dashboard } from './dashboard';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../../shared/auth.service';
import { ShipmentAPIService } from '../../api';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

describe('Dashboard', () => {
  let component: Dashboard;
  let fixture: ComponentFixture<Dashboard>;
  let mockAuthService: any;
  let mockShipmentApi: any;
  let mockRouter: any;

  beforeEach(async () => {
    // 1. Create simple mock objects for our services using Vitest's vi.fn()
    mockAuthService = {
      getDecodedToken: vi.fn()
    };

    mockShipmentApi = {
      getShipmentsBySender: vi.fn(),
      getShipmentsByReceiver: vi.fn()
    };

    mockRouter = {
      navigate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        // Provide the real HttpClient for testing, but override our specific services with mocks
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: Router, useValue: mockRouter }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect staff to /app/shipments', () => {
    // Arrange: Simulate a CLERK logging in
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLERK' });

    // Act: Trigger ngOnInit
    fixture.detectChanges();

    // Assert: We should not make any API calls and loading should be false
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/app/shipments']);
  });

  it('should fetch and display sent and received shipments for a CLIENT', () => {
    // Arrange: Simulate a CLIENT logging in
    const mockUserId = 'client-uuid-123';
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLIENT', userId: mockUserId });

    // Simulate the backend returning a Page object with an array of shipments
    const mockSentResponse = { content: [{ id: 'sent-1', trackingNumber: 'TRK-SENT' }] };
    const mockReceivedResponse = { content: [{ id: 'received-1', trackingNumber: 'TRK-RECEIVED' }] };

    // When the component calls the API, return our fake data as an Observable (of)
    mockShipmentApi.getShipmentsBySender.mockReturnValue(of(mockSentResponse));
    mockShipmentApi.getShipmentsByReceiver.mockReturnValue(of(mockReceivedResponse));

    // Act: Trigger ngOnInit
    fixture.detectChanges();

    // Assert: Check that the API was called with the correct user ID
    expect(mockShipmentApi.getShipmentsBySender).toHaveBeenCalledWith(mockUserId, { page: 0, size: 20 });

    // Assert: Check that the data was correctly loaded into the Angular Signals
    expect(component.isLoading()).toBe(false);
    expect(component.sentShipments().length).toBe(1);
    expect(component.sentShipments()[0].trackingNumber).toBe('TRK-SENT');
    expect(component.receivedShipments().length).toBe(1);
  });

  it('should handle API errors gracefully', () => {
    // Arrange: Simulate a CLIENT logging in
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLIENT', userId: '123' });

    // Simulate a network failure or 500 error from the backend
    mockShipmentApi.getShipmentsBySender.mockReturnValue(throwError(() => new Error('Network failed')));
    mockShipmentApi.getShipmentsByReceiver.mockReturnValue(of({ content: [] }));

    // Act: Trigger ngOnInit
    fixture.detectChanges();

    // Assert: Check that the component handles the error without crashing
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load shipments. Please try again.');
  });
});
