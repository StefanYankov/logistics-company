import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ClerkRegistration} from './clerk-registration';
import {provideRouter} from '@angular/router';
import {
  CityAPIService,
  ClientAPIService,
  OfficeAPIService,
  PricingAPIService,
  ServiceCatalogAPIService,
  ShipmentAPIService
} from '../../../api';
import {delay, of, throwError} from 'rxjs';

describe('ClerkRegistration', () => {
  let component: ClerkRegistration;
  let fixture: ComponentFixture<ClerkRegistration>;

  let mockClientApi: any;
  let mockOfficeApi: any;
  let mockCityApi: any;
  let mockShipmentApi: any;
  let mockServiceCatalogApi: any;
  let mockPricingApi: any;

  beforeEach(async () => {
    mockClientApi = {
      searchClients: vi.fn().mockReturnValue(of({ content: [] }).pipe(delay(0))),
      quickRegisterClient: vi.fn().mockReturnValue(of({}))
    };
    mockOfficeApi = { getAllOffices: vi.fn().mockReturnValue(of({ content: [] })) };
    mockCityApi = { getAllCities: vi.fn().mockReturnValue(of({ content: [] })) };
    mockShipmentApi = { registerShipment: vi.fn() };
    mockServiceCatalogApi = { getAllServices: vi.fn().mockReturnValue(of([])) };
    mockPricingApi = { getActivePricingConfig: vi.fn().mockReturnValue(of({ basePrice: 5, pricePerKg: 1, addressSurcharge: 3 })) };

    await TestBed.configureTestingModule({
      imports: [ClerkRegistration],
      providers: [
        provideRouter([{ path: 'app/shipments', children: [] }]),
        { provide: ClientAPIService, useValue: mockClientApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: mockCityApi },
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: ServiceCatalogAPIService, useValue: mockServiceCatalogApi },
        { provide: PricingAPIService, useValue: mockPricingApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClerkRegistration);
    component = fixture.componentInstance;
  });

  // A helper function to properly initialize the component given its microtask/promise setup
  async function initializeComponent() {
    fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 50)); // Wait for all dropdown fetches to complete
  }

  it('should initialize and load dropdowns', async () => {
    await initializeComponent();
    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
  });


  it('should clear sender selection and invalidate the form control', async () => {
    await initializeComponent();

    component.registerForm.patchValue({ senderId: '123', selectedSenderDisplay: 'John Doe' });
    component.clearSender();

    expect(component.registerForm.get('senderId')?.value).toBe('');
    expect(component.registerForm.get('selectedSenderDisplay')?.value).toBe('');
    expect(component.registerForm.get('senderId')?.valid).toBe(false);
  });

  describe('Quick Registration Flow', () => {
    beforeEach(async () => {
      await initializeComponent();
    });

    it('should open quick register panel and pre-fill phone number if search term is a phone', async () => {
      component.registerForm.patchValue({ senderSearchTerm: '0888123456' });

      component.openQuickRegister();

      expect(component.showQuickRegister()).toBe(true);
      expect(component.quickRegisterForm.get('phoneNumber')?.value).toBe('0888123456');
    });

    it('should open quick register panel but NOT pre-fill if search term is a name', async () => {
      component.registerForm.patchValue({ senderSearchTerm: 'John' });

      component.openQuickRegister();

      expect(component.showQuickRegister()).toBe(true);
      expect(component.quickRegisterForm.get('phoneNumber')?.value).toBe('');
    });

    it('should cancel quick register', async () => {
      component.showQuickRegister.set(true);
      component.quickRegisterForm.patchValue({ firstName: 'Test' });
      component.quickRegisterError.set('Some error');

      component.cancelQuickRegister();

      expect(component.showQuickRegister()).toBe(false);
      expect(component.quickRegisterForm.get('firstName')?.value).toBeNull();
      expect(component.quickRegisterError()).toBeNull();
    });

    it('should successfully submit quick register and lock sender', async () => {
      const newClient = { id: 'new-uuid', firstName: 'Jane', lastName: 'Doe', phoneNumber: '0888111222' };
      mockClientApi.quickRegisterClient.mockReturnValue(of(newClient));

      component.showQuickRegister.set(true);
      component.quickRegisterForm.patchValue({
        firstName: 'Jane',
        lastName: 'Doe',
        phoneNumber: '0888111222'
      });

      component.onQuickRegisterSubmit();

      expect(mockClientApi.quickRegisterClient).toHaveBeenCalledWith({
        firstName: 'Jane',
        lastName: 'Doe',
        phoneNumber: '0888111222',
        email: undefined
      });

      expect(component.isQuickRegistering()).toBe(false);
      expect(component.showQuickRegister()).toBe(false);
      expect(component.registerForm.get('senderId')?.value).toBe('new-uuid');
      expect(component.registerForm.get('selectedSenderDisplay')?.value).toBe('Jane Doe (0888111222)');
    });

    it('should handle quick register server errors gracefully', async () => {
      mockClientApi.quickRegisterClient.mockReturnValue(throwError(() => ({
        status: 409,
        error: { errorCode: 'PHONE_DUPLICATE', detail: 'Phone already exists' }
      })));

      component.showQuickRegister.set(true);
      component.quickRegisterForm.patchValue({
        firstName: 'Jane',
        lastName: 'Doe',
        phoneNumber: '0888111222'
      });

      component.onQuickRegisterSubmit();

      expect(component.isQuickRegistering()).toBe(false);
      expect(component.showQuickRegister()).toBe(true);
      expect(component.quickRegisterError()).toBe('Phone already exists');
    });
  });
});
