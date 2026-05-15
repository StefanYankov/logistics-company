import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClerkRegistration } from './clerk-registration';
import { provideRouter } from '@angular/router';
import { ClientAPIService, OfficeAPIService, CityAPIService, ShipmentAPIService, ServiceCatalogAPIService } from '../../../api';
import { of, throwError } from 'rxjs';

describe('ClerkRegistration', () => {
  let component: ClerkRegistration;
  let fixture: ComponentFixture<ClerkRegistration>;

  let mockClientApi: any;
  let mockOfficeApi: any;
  let mockCityApi: any;
  let mockShipmentApi: any;
  let mockServiceCatalogApi: any;

  beforeEach(async () => {
    mockClientApi = {
      searchClients: vi.fn().mockReturnValue(of({ content: [] })),
      quickRegisterClient: vi.fn().mockReturnValue(of({}))
    };
    mockOfficeApi = { getAllOffices: vi.fn().mockReturnValue(of({ content: [] })) };
    mockCityApi = { getAllCities: vi.fn().mockReturnValue(of({ content: [] })) };
    mockShipmentApi = { registerShipment: vi.fn() };
    mockServiceCatalogApi = { getAllServices: vi.fn().mockReturnValue(of([])) };

    await TestBed.configureTestingModule({
      imports: [ClerkRegistration],
      providers: [
        provideRouter([{ path: 'app/shipments', children: [] }]),
        { provide: ClientAPIService, useValue: mockClientApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: mockCityApi },
        { provide: ShipmentAPIService, useValue: mockShipmentApi },
        { provide: ServiceCatalogAPIService, useValue: mockServiceCatalogApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClerkRegistration);
    component = fixture.componentInstance;
  });

  it('should initialize and load dropdowns', async () => {
// 1. Kicks off ngOnInit and schedules loadDropdownData() inside a macro-task
    fixture.detectChanges();

    // 2. Clear the macro-task execution frame so loadDropdownData begins running
    await new Promise(resolve => setTimeout(resolve, 0));

    // 3. Clear the subsequent microtask queue so the async Blob parsing promises resolve
    await new Promise(resolve => setTimeout(resolve, 0));

    // 4. Update layout metrics now that signals have stabilized
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
  });

  it('should search for clients when typing in the sender input', async () => {
    const mockSearchResults = { content: [{ id: '1', firstName: 'John', lastName: 'Doe', phoneNumber: '123' }] };
    mockClientApi.searchClients.mockReturnValue(of(mockSearchResults));

    fixture.detectChanges();
    await fixture.whenStable();

    component.registerForm.patchValue({ senderSearchTerm: 'John' });
    fixture.detectChanges();

    await new Promise(resolve => setTimeout(resolve, 550));
    fixture.detectChanges();

    expect(mockClientApi.searchClients).toHaveBeenCalledWith('John', { page: 0, size: 10 });
    expect(component.senderSearchResults().length).toBe(1);
  });

  it('should clear sender selection and invalidate the form control', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    component.registerForm.patchValue({ senderId: '123', selectedSenderDisplay: 'John Doe' });
    fixture.detectChanges();

    component.clearSender();
    fixture.detectChanges();

    expect(component.registerForm.get('senderId')?.value).toBe('');
    expect(component.registerForm.get('selectedSenderDisplay')?.value).toBe('');
    expect(component.registerForm.get('senderId')?.valid).toBe(false);
  });

  describe('Quick Registration Flow', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('should open quick register panel and pre-fill phone number if search term is a phone', () => {
      component.registerForm.patchValue({ senderSearchTerm: '0888123456' });
      component.openQuickRegister();
      expect(component.showQuickRegister()).toBe(true);
      expect(component.quickRegisterForm.get('phoneNumber')?.value).toBe('0888123456');
    });

    it('should open quick register panel but NOT pre-fill if search term is a name', () => {
      component.registerForm.patchValue({ senderSearchTerm: 'John' });
      component.openQuickRegister();
      expect(component.showQuickRegister()).toBe(true);
      expect(component.quickRegisterForm.get('phoneNumber')?.value).toBe('');
    });

    it('should cancel quick register', () => {
      component.showQuickRegister.set(true);
      component.quickRegisterForm.patchValue({ firstName: 'Test' });
      component.quickRegisterError.set('Some error');
      component.cancelQuickRegister();

      expect(component.showQuickRegister()).toBe(false);
      expect(component.quickRegisterForm.get('firstName')?.value).toBeNull();
      expect(component.quickRegisterError()).toBeNull();
    });

    it('should successfully submit quick register and lock sender', () => {
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

    it('should handle quick register server errors gracefully', () => {
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
