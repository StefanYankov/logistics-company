import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ClerkRegistration } from './clerk-registration';
import { provideRouter } from '@angular/router';
import { ClientAPIService, OfficeAPIService, CityAPIService, ShipmentAPIService } from '../../../api';
import { of, throwError } from 'rxjs';

describe('ClerkRegistration', () => {
  let component: ClerkRegistration;
  let fixture: ComponentFixture<ClerkRegistration>;

  let mockClientApi: any;
  let mockOfficeApi: any;
  let mockCityApi: any;
  let mockShipmentApi: any;

  beforeEach(async () => {
    mockClientApi = {
        getAllClients: vi.fn(),
        searchClients: vi.fn(),
        quickRegisterClient: vi.fn()
    };
    mockOfficeApi = { getAllOffices: vi.fn() };
    mockCityApi = { getAllCities: vi.fn() };
    mockShipmentApi = { registerShipment: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ClerkRegistration],
      providers: [
        provideRouter([{ path: 'app/shipments', children: [] }]),
        { provide: ClientAPIService, useValue: mockClientApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: mockCityApi },
        { provide: ShipmentAPIService, useValue: mockShipmentApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ClerkRegistration);
    component = fixture.componentInstance;
  });

  it('should initialize and load dropdowns', async () => {
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    fixture.detectChanges(); // triggers ngOnInit
    await Promise.resolve(); // resolve the queueMicrotask
    fixture.detectChanges(); // reflect changes in DOM

    expect(component).toBeTruthy();
    expect(component.isLoadingLookups()).toBe(false);
  });

  it('should search for clients when typing in the sender input', async () => {
    mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
    mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));

    const mockSearchResults = { content: [{ id: '1', firstName: 'John', lastName: 'Doe', phoneNumber: '123' }] };
    mockClientApi.searchClients.mockReturnValue(of(mockSearchResults));

    fixture.detectChanges();
    await Promise.resolve(); // clear microtasks

    // Simulate user typing
    component.registerForm.patchValue({ senderSearchTerm: 'John' });

    // Wait for the 300ms debounceTime to pass
    await new Promise(resolve => setTimeout(resolve, 350));
    fixture.detectChanges(); // Tell Angular to redraw the HTML now that the Signals have updated!

    expect(mockClientApi.searchClients).toHaveBeenCalledWith('John', { page: 0, size: 10 });
    expect(component.senderSearchResults().length).toBe(1);
    expect(component.showQuickRegister()).toBe(false); // Should hide quick register when typing
  });

  it('should clear sender selection and invalidate the form control', async () => {
      mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
      mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));
      fixture.detectChanges();
      await Promise.resolve();

      component.registerForm.patchValue({ senderId: '123', selectedSenderDisplay: 'John Doe' });

      component.clearSender();

      expect(component.registerForm.get('senderId')?.value).toBe('');
      expect(component.registerForm.get('selectedSenderDisplay')?.value).toBe('');
      expect(component.registerForm.get('senderId')?.valid).toBe(false);
  });

  describe('Quick Registration Flow', () => {
      beforeEach(async () => {
          mockOfficeApi.getAllOffices.mockReturnValue(of({ content: [] }));
          mockCityApi.getAllCities.mockReturnValue(of({ content: [] }));
          fixture.detectChanges();
          await Promise.resolve();
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
              email: undefined // null/empty gracefully mapped to undefined
          });

          // Verify UI updates
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
          expect(component.showQuickRegister()).toBe(true); // Should stay open
          expect(component.quickRegisterError()).toBe('Phone already exists');
      });
  });
});
