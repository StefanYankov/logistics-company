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
    mockClientApi = { getMyProfile: vi.fn() };
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
    mockClientApi.getMyProfile.mockReturnValue(of({ firstName: 'John', lastName: 'Doe', phoneNumber: '123' }));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();
    await Promise.resolve(); // resolve the queueMicrotask
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
    expect(component.loggedInUserId).toBe('client-123');
  });

  it('should toggle origin validation correctly', async () => {
    mockClientApi.getMyProfile.mockReturnValue(of({ firstName: 'John', lastName: 'Doe', phoneNumber: '123' }));
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

  it('should require receiver name and valid phone number', async () => {
    mockClientApi.getMyProfile.mockReturnValue(of({ firstName: 'John', lastName: 'Doe', phoneNumber: '123' }));
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    fixture.detectChanges();
    await Promise.resolve();

    const nameCtrl = component.registerForm.get('receiverName');
    const phoneCtrl = component.registerForm.get('receiverPhone');

    // Initially invalid
    expect(nameCtrl?.valid).toBe(false);
    expect(phoneCtrl?.valid).toBe(false);

    // Invalid phone pattern
    phoneCtrl?.setValue('invalid');
    expect(phoneCtrl?.valid).toBe(false);

    // Valid data
    nameCtrl?.setValue('Jane Doe');
    phoneCtrl?.setValue('+359888123456');
    expect(nameCtrl?.valid).toBe(true);
    expect(phoneCtrl?.valid).toBe(true);
  });
});
