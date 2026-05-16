import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {forkJoin, of} from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, startWith, switchMap, tap } from 'rxjs/operators';
import { ShipmentAPIService, ClientAPIService, OfficeAPIService, CityAPIService, ServiceCatalogAPIService } from '../../../api';
import { ClientViewDto, OfficeViewDto, CityViewDto, ShipmentCreationDto, ClientQuickRegistrationDto, ServiceCatalogViewDto } from '../../../api';

@Component({
  selector: 'app-clerk-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './clerk-registration.html',
  styleUrl: './clerk-registration.css'
})
export class ClerkRegistration implements OnInit {
  private fb = inject(FormBuilder);
  private shipmentApi = inject(ShipmentAPIService);
  private clientApi = inject(ClientAPIService);
  private officeApi = inject(OfficeAPIService);
  private cityApi = inject(CityAPIService);
  private serviceCatalogApi = inject(ServiceCatalogAPIService); // Inject new service
  private router = inject(Router);

  isLoadingLookups = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Search Results
  senderSearchResults = signal<ClientViewDto[]>([]);
  isSearchingSender = signal(false);

  // Quick Register State
  showQuickRegister = signal(false);
  isQuickRegistering = signal(false);
  quickRegisterError = signal<string | null>(null);

  // Source of truth
  allOffices: OfficeViewDto[] = [];
  cities = signal<CityViewDto[]>([]);
  availableServices = signal<ServiceCatalogViewDto[]>([]); // New signal for services

  // Filtered lists for the UI
  filteredDeliveryOffices = signal<OfficeViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);
  paidByOptions = Object.values(ShipmentCreationDto.PaidByEnum);

  // Hardcoded for now. In reality, get this from the Clerk's profile.
  originOfficeId = 1;

  // Track selected addon IDs
  selectedServiceIds = signal<number[]>([]); // New signal for selected addon IDs

  registerForm = this.fb.group({
    // Sender Configuration (Autocomplete)
    senderSearchTerm: [''],
    senderId: ['', Validators.required],
    selectedSenderDisplay: [''], // Just for showing the selected name

    // Receiver Configuration (Simplified to just guest inputs, auto-matched by backend)
    receiverName: ['', Validators.required],
    receiverPhone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    receiverEmail: ['', Validators.email],

    // Destination Configuration
    destinationType: ['OFFICE', Validators.required],
    deliveryCityFilterId: [null as number | null],
    deliveryOfficeId: [null as number | null],
    deliveryCityId: [null as number | null],
    deliveryStreet: [''],
    deliveryDistrict: [''],
    deliveryBuilding: [''],
    deliveryEntrance: [''],
    deliveryFloor: [''],
    deliveryApartment: [''],

    // Package & Financials
    type: [ShipmentCreationDto.TypeEnum.Parcel, Validators.required],
    weight: [0.1, [Validators.required, Validators.min(0.1)]],
    paidBy: [ShipmentCreationDto.PaidByEnum.Sender, Validators.required]
  });

  // Quick Register Form
  quickRegisterForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    email: ['', Validators.email]
  });

  ngOnInit() {
    this.setupDynamicValidations();
    this.setupCascadingDropdowns();
    this.setupSenderAutocomplete();

    // Production-grade microtask deferral
    queueMicrotask(() => this.loadDropdownData());
  }

  private setupSenderAutocomplete() {
    const senderSearchTermCtrl = this.registerForm.get('senderSearchTerm');
    if (senderSearchTermCtrl) {
      senderSearchTermCtrl.valueChanges.pipe(
        debounceTime(500),
        distinctUntilChanged(),
        tap(() => {
          // Sync UI states that belong to the input event pass
          this.isSearchingSender.set(true);
          this.showQuickRegister.set(false);

          this.registerForm.get('senderId')?.setValue('', { emitEvent: false });
          this.registerForm.get('senderId')?.markAsTouched();
          this.registerForm.get('senderId')?.updateValueAndValidity({ emitEvent: false });
        }),
        switchMap(term => {
          if (!term || term.length < 3) {
            this.senderSearchResults.set([]);
            this.isSearchingSender.set(false);
            return of(null);
          }
          return this.clientApi.searchClients(term, { page: 0, size: 10 }).pipe(
            catchError(() => of({ content: [] }))
          );
        })
      ).subscribe(result => {
        // Safe Microtask Isolation: Defer the signal array assignment to the next macro-frame.
        // This ensures the input change pass completes cleanly before the template handles results.
        setTimeout(() => {
          this.isSearchingSender.set(false);
          if (result && result.content) {
            this.senderSearchResults.set(result.content);
          }
        }, 0);
      });
    }
  }

  selectSender(client: ClientViewDto) {
    this.registerForm.patchValue({
      senderId: client.id,
      selectedSenderDisplay: `${client.firstName} ${client.lastName} (${client.phoneNumber})`,
      senderSearchTerm: '' // Clear search
    }, { emitEvent: false });
    this.registerForm.get('senderId')?.updateValueAndValidity({ emitEvent: false });
    this.senderSearchResults.set([]); // Hide results
  }

  clearSender() {
    this.registerForm.patchValue({
      senderId: '',
      selectedSenderDisplay: ''
    }, { emitEvent: false });
    this.registerForm.get('senderId')?.updateValueAndValidity({ emitEvent: false });
  }

  openQuickRegister() {
    // Pre-fill phone number if they searched for one
    const searchTerm = this.registerForm.get('senderSearchTerm')?.value || '';
    if (/^\+?[0-9]+$/.test(searchTerm)) {
      this.quickRegisterForm.patchValue({ phoneNumber: searchTerm });
    }
    this.showQuickRegister.set(true);
  }

  cancelQuickRegister() {
    this.showQuickRegister.set(false);
    this.quickRegisterForm.reset();
    this.quickRegisterError.set(null);
  }

  onQuickRegisterSubmit() {
    if (this.quickRegisterForm.valid) {
      this.isQuickRegistering.set(true);
      this.quickRegisterError.set(null);

      const formVal = this.quickRegisterForm.value;
      const payload: ClientQuickRegistrationDto = {
        firstName: formVal.firstName!,
        lastName: formVal.lastName!,
        phoneNumber: formVal.phoneNumber!,
        email: formVal.email || undefined
      };

      this.clientApi.quickRegisterClient(payload).subscribe({
        next: (newClient) => {
          this.isQuickRegistering.set(false);
          // Automatically select the new client
          this.selectSender(newClient);
          this.cancelQuickRegister();
        },
        error: (err) => {
          this.isQuickRegistering.set(false);
          if (err.status === 409 && err.error?.errorCode) {
            this.quickRegisterError.set(err.error.detail || 'This user already exists.');
          } else if (err.status === 400 && err.error?.detail) {
            this.quickRegisterError.set(err.error.detail);
          } else {
            this.quickRegisterError.set('Failed to register customer. Please try again.');
          }
        }
      });
    } else {
      this.quickRegisterForm.markAllAsTouched();
    }
  }

  private setupCascadingDropdowns() {
    // Watch Delivery City Filter
    const deliveryCityFilterCtrl = this.registerForm.get('deliveryCityFilterId');
    if (deliveryCityFilterCtrl) {
      deliveryCityFilterCtrl.valueChanges.subscribe(cityId => {
        // Reset the selected office when the city changes
        this.registerForm.get('deliveryOfficeId')?.setValue(null, { emitEvent: false });

        if (!cityId) {
          this.filteredDeliveryOffices.set(this.allOffices);
          return;
        }

        const filtered = this.allOffices.filter(o => o.cityName === this.cities().find(c => c.id == cityId)?.name);
        this.filteredDeliveryOffices.set(filtered);
      });
    }
  }

  private setupDynamicValidations() {
    // Destination Validation
    const destTypeCtrl = this.registerForm.get('destinationType');
    if (destTypeCtrl) {
      destTypeCtrl.valueChanges.pipe(startWith(destTypeCtrl.value)).subscribe(type => {
        this.syncDestinationValidators(type);
      });
    }
  }

  private syncDestinationValidators(type: string | null) {
    const officeCtrl = this.registerForm.get('deliveryOfficeId');
    const cityCtrl = this.registerForm.get('deliveryCityId');
    const streetCtrl = this.registerForm.get('deliveryStreet');
    const cityFilterCtrl = this.registerForm.get('deliveryCityFilterId');

    if (type === 'OFFICE') {
      officeCtrl?.setValidators([Validators.required]);
      cityCtrl?.clearValidators();
      streetCtrl?.clearValidators();
      cityCtrl?.setValue(null, { emitEvent: false });
      streetCtrl?.setValue('', { emitEvent: false });
      cityFilterCtrl?.clearValidators(); // Clear validators for filter
    } else {
      officeCtrl?.clearValidators();
      cityCtrl?.setValidators([Validators.required]);
      streetCtrl?.setValidators([Validators.required]);
      officeCtrl?.setValue(null, { emitEvent: false });
      cityFilterCtrl?.setValue(null, { emitEvent: false }); // Clear filter value
    }
    [officeCtrl, cityCtrl, streetCtrl, cityFilterCtrl].forEach(c => c?.updateValueAndValidity({ emitEvent: false }));
  }

  private async loadDropdownData() {
    this.isLoadingLookups.set(true);
    const pageParams = { page: 0, size: 500 };

    forkJoin({
      officesRes: this.officeApi.getAllOffices(pageParams),
      citiesRes: this.cityApi.getAllCities(pageParams),
      servicesRes: this.serviceCatalogApi.getAllServices()
    }).pipe(
      catchError((err) => {
        console.error("Failed to load initial form data", err);
        this.errorMessage.set('Failed to load form data. Please try again.');
        this.isLoadingLookups.set(false);
        return of(null);
      })
    ).subscribe(async (result) => {
      if (result) {
        try {
          // Unpack network blobs into clean objects
          const officesData = await this.parseBlobResponse(result.officesRes);
          const citiesData = await this.parseBlobResponse(result.citiesRes);
          const servicesData = await this.parseBlobResponse(result.servicesRes);

          this.allOffices = officesData?.content || [];
          this.filteredDeliveryOffices.set(this.allOffices);
          this.cities.set(citiesData?.content || []);
          this.availableServices.set(Array.isArray(servicesData) ? servicesData : []);
        } catch (parseError) {
          console.error("Data deserialization error:", parseError);
          this.errorMessage.set('Data corruption encountered. Please refresh.');
        } finally {
          this.isLoadingLookups.set(false);
        }
      }
    });
  }

  // New method to toggle selected services
  toggleService(serviceId: number | undefined, event: Event) {
    if (!serviceId) return;
    const isChecked = (event.target as HTMLInputElement).checked;
    const current = this.selectedServiceIds();
    if (isChecked) {
      this.selectedServiceIds.set([...current, serviceId]);
    } else {
      this.selectedServiceIds.set(current.filter(id => id !== serviceId));
    }
  }

  /**
   * Helper utility to convert openapi generated Blob response streams to runtime JSON.
   */
  private async parseBlobResponse(response: any): Promise<any> {
    if (response instanceof Blob) {
      const text = await response.text();
      return text ? JSON.parse(text) : null;
    }
    return response;
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.isSubmitting.set(true);
      this.errorMessage.set(null);

      const v = this.registerForm.value;
      const payload: ShipmentCreationDto = {
        senderId: v.senderId!,
        type: v.type as ShipmentCreationDto.TypeEnum,
        weight: v.weight!,
        paidBy: v.paidBy as ShipmentCreationDto.PaidByEnum,
        receiverName: v.receiverName!,
        receiverPhone: v.receiverPhone!,
        receiverEmail: v.receiverEmail || undefined,
        selectedServiceIds: this.selectedServiceIds().length > 0 ? Array.from(this.selectedServiceIds()) as unknown as Set<number> : undefined
      };

      // We explicitly lock Origin to the Clerk's current office
      payload.originOfficeId = this.originOfficeId;

      // Destination - Omit the opposite to pass backend XOR validation
      if (v.destinationType === 'OFFICE') {
        payload.deliveryOfficeId = v.deliveryOfficeId!;
      } else {
        payload.deliveryAddress = {
          cityId: v.deliveryCityId!,
          street: v.deliveryStreet!,
          district: v.deliveryDistrict || undefined,
          building: v.deliveryBuilding || undefined,
          entrance: v.deliveryEntrance || undefined,
          floor: v.deliveryFloor || undefined,
          apartment: v.deliveryApartment || undefined
        };
      }

      this.shipmentApi.registerShipment(payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.router.navigate(['/app/shipments']).catch(console.error); // Clerks usually go to the list
        },
        error: (err) => {
          this.isSubmitting.set(false);
          if (err.status === 400 && err.error?.detail) {
            this.errorMessage.set(err.error.detail);
          } else {
            this.errorMessage.set('An unexpected error occurred while registering the shipment.');
          }
        }
      });
    } else {
      this.registerForm.markAllAsTouched();
    }
  }
}
