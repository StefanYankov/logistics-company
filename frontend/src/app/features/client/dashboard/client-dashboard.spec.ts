import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ClientDashboard} from './client-dashboard';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {AuthService} from '../../../shared/auth.service';
import {ShipmentAPIService} from '../../../api';
import {of, throwError} from 'rxjs';
import {Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

describe('ClientDashboard', () => {
  let component: ClientDashboard;
  let fixture: ComponentFixture<ClientDashboard>;
  let mockAuthService: any;
  let mockShipmentApi: any;
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      getDecodedToken: vi.fn()
    };

    mockShipmentApi = {
      getShipmentsBySender: vi.fn(),
      getShipmentsByReceiver: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [ClientDashboard, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: ShipmentAPIService, useValue: mockShipmentApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientDashboard);
    component = fixture.componentInstance;
    router = TestBed.inject(Router); // Inject the real testing router
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect staff to /app/shipments', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLERK' });

    fixture.detectChanges();

    expect(navigateSpy).toHaveBeenCalledWith(['/app/shipments']);
  });

  it('should fetch and display sent and received shipments for a CLIENT', () => {
    const mockUserId = 'client-uuid-123';
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLIENT', userId: mockUserId });

    const mockSentResponse = { content: [{ id: 'sent-1', trackingNumber: 'TRK-SENT' }] };
    const mockReceivedResponse = { content: [{ id: 'received-1', trackingNumber: 'TRK-RECEIVED' }] };

    mockShipmentApi.getShipmentsBySender.mockReturnValue(of(mockSentResponse));
    mockShipmentApi.getShipmentsByReceiver.mockReturnValue(of(mockReceivedResponse));

    fixture.detectChanges();

    expect(mockShipmentApi.getShipmentsBySender).toHaveBeenCalledWith(mockUserId, { page: 0, size: 20 });

    expect(component.isLoading()).toBe(false);
    expect(component.sentShipments().length).toBe(1);
    expect(component.sentShipments()[0].trackingNumber).toBe('TRK-SENT');
    expect(component.receivedShipments().length).toBe(1);
  });

  it('should handle API errors gracefully', () => {
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLIENT', userId: '123' });

    mockShipmentApi.getShipmentsBySender.mockReturnValue(throwError(() => new Error('Network failed')));
    mockShipmentApi.getShipmentsByReceiver.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load shipments. Please try again.');
  });
});
