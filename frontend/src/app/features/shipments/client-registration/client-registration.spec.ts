import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientRegistration } from './client-registration';
import { provideRouter } from '@angular/router';
import { ClientAPIService, OfficeAPIService, CityAPIService, ShipmentAPIService } from '../../../api';
import { AuthService } from '../../../shared/auth.service';
import { of } from 'rxjs';

describe('ClientRegistration', () => {
  let component: ClientRegistration;
  let fixture: ComponentFixture<ClientRegistration>;

  let mockClientApi: any;
  let mockOfficeApi: any;
  let mockCityApi: any;
  let mockShipmentApi: any;
  let mockAuthService: any;

  beforeEach(async () => {
    mockClientApi = { getAllClients: vi.fn() };
    mockOfficeApi = { getAllOffices: vi.fn() };
    mockCityApi = { getAllCities: vi.fn() };
    mockShipmentApi = { registerShipment: vi.fn() };
    mockAuthService = { getDecodedToken: vi.fn().mockReturnValue({ userId: 'client-123', role: 'ROLE_CLIENT' }) };

    await TestBed.configureTestingModule({
      imports: [ClientRegistration],
      providers: [
        provideRouter([{ path: 'app', children: [] }]),
        { provide: ClientAPIService, useValue: mockClientApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: mockCityApi },
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientRegistration);
    component = fixture.componentInstance;
  });

  it('should initialize and load dropdowns', async () => {
    mockClientApi.getAllClients.mockReturnValue(of({ content: [] }));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();
    await Promise.resolve();
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
    expect(component.loggedInUserId).toBe('client-123');
  });

  it('should toggle receiver validation correctly', async () => {
    mockClientApi.getAllClients.mockReturnValue(of({ content: [] }));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();
    await Promise.resolve();

    // Default is REGISTERED
    expect(component.registerForm.get('receiverId')?.validator).toBeTruthy();
    expect(component.registerForm.get('receiverName')?.validator).toBeNull();

    // Switch to GUEST
    component.registerForm.patchValue({ receiverType: 'GUEST' });
    expect(component.registerForm.get('receiverId')?.validator).toBeNull();
    expect(component.registerForm.get('receiverName')?.validator).toBeTruthy();
  });

  it('should toggle origin validation correctly', async () => {
    mockClientApi.getAllClients.mockReturnValue(of({ content: [] }));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();
    await Promise.resolve();

    // Default is OFFICE
    expect(component.registerForm.get('originOfficeId')?.validator).toBeTruthy();
    expect(component.registerForm.get('originCityId')?.validator).toBeNull();

    // Switch to ADDRESS
    component.registerForm.patchValue({ originType: 'ADDRESS' });
    expect(component.registerForm.get('originOfficeId')?.validator).toBeNull();
    expect(component.registerForm.get('originCityId')?.validator).toBeTruthy();
  });
});
