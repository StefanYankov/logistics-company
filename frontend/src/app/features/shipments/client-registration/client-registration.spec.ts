import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClientRegistration } from './client-registration';
import { provideRouter } from '@angular/router';
import { ClientAPIService, OfficeAPIService, CityAPIService, ShipmentAPIService, ServiceCatalogAPIService } from '../../../api';
import { AuthService } from '../../../shared/auth.service';
import { of } from 'rxjs';

describe('ClientRegistration', () => {
  let component: ClientRegistration;
  let fixture: ComponentFixture<ClientRegistration>;

  let mockClientApi: any;
  let mockOfficeApi: any;
  let mockCityApi: any;
  let mockShipmentApi: any;
  let mockServiceCatalogApi: any;
  let mockAuthService: any;

  const createMockBlobResponse = (data: any): Blob => {
    return new Blob([JSON.stringify(data)], { type: 'application/json' });
  };

  beforeEach(async () => {
    mockClientApi = { getMyProfile: vi.fn(), updateMyProfile: vi.fn() };
    mockOfficeApi = { getAllOffices: vi.fn() };
    mockCityApi = { getAllCities: vi.fn() };
    mockShipmentApi = { registerShipment: vi.fn() };
    mockServiceCatalogApi = { getAllServices: vi.fn() };
    mockAuthService = { getDecodedToken: vi.fn().mockReturnValue({ userId: 'client-123', role: 'ROLE_CLIENT' }) };

    await TestBed.configureTestingModule({
      imports: [ClientRegistration],
      providers: [
        provideRouter([{ path: 'app', children: [] }]),
        { provide: ClientAPIService, useValue: mockClientApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: mockCityApi },
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: ServiceCatalogAPIService, useValue: mockServiceCatalogApi },
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClientRegistration);
    component = fixture.componentInstance;
  });

  const setupDefaultMockResponses = () => {
    mockClientApi.getMyProfile.mockReturnValue(of({ firstName: 'Stefan', lastName: 'Yankov', phoneNumber: '+359888111222' }));
    mockOfficeApi.getAllOffices.mockReturnValue(of(createMockBlobResponse({ content: [] })));
    mockCityApi.getAllCities.mockReturnValue(of(createMockBlobResponse({ content: [] })));
    mockServiceCatalogApi.getAllServices.mockReturnValue(of(createMockBlobResponse([])));
  };

  it('should initialize profile and unpack backend blobs into signals', async () => {
    setupDefaultMockResponses();

    // 1. Trigger initial change detection pass (kicks off ngOnInit)
    fixture.detectChanges();

    // 2. Allow a macroscopic frame tick for the async Blob.text() promises to resolve
    await new Promise(resolve => setTimeout(resolve, 0));

    // 3. Inform the Zoneless engine to run change detection on the newly set Signals
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
    expect(component.loggedInUserId).toBe('client-123');
    expect(component.senderProfileForm.get('firstName')?.value).toBe('Stefan');
  });
  it('should toggle origin validation cleanly without side-effect event loops', async () => {
    setupDefaultMockResponses();

    fixture.detectChanges();
    await fixture.whenStable();

    // Default configuration mode check (OFFICE)
    expect(component.registerForm.get('originOfficeId')?.validator).toBeTruthy();
    expect(component.registerForm.get('originCityId')?.validator).toBeNull();

    // Swap configuration target to raw address mapping
    component.registerForm.patchValue({ originType: 'ADDRESS' });
    fixture.detectChanges();

    expect(component.registerForm.get('originOfficeId')?.value).toBeNull();
    expect(component.registerForm.get('originOfficeId')?.validator).toBeNull();
    expect(component.registerForm.get('originCityId')?.validator).toBeTruthy();
  });

  it('should invalidate receiver details fields when formatting regex criteria fail', async () => {
    setupDefaultMockResponses();

    fixture.detectChanges();
    await fixture.whenStable();

    const nameCtrl = component.registerForm.get('receiverName');
    const phoneCtrl = component.registerForm.get('receiverPhone');

    expect(nameCtrl?.valid).toBe(false);
    expect(phoneCtrl?.valid).toBe(false);

    // Apply broken numerical character profile sequence values
    phoneCtrl?.setValue('bad-phone-format');
    fixture.detectChanges();
    expect(phoneCtrl?.valid).toBe(false);

    // Provide target standardized validation data metrics
    nameCtrl?.setValue('Jane Doe');
    phoneCtrl?.setValue('+359888123456');
    fixture.detectChanges();

    expect(nameCtrl?.valid).toBe(true);
    expect(phoneCtrl?.valid).toBe(true);
  });
});
