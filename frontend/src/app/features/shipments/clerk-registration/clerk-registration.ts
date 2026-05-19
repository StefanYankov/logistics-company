import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {forkJoin, of} from 'rxjs';
import {catchError, debounceTime, distinctUntilChanged, startWith, switchMap, tap} from 'rxjs/operators';
import {
  CityAPIService,
  CityViewDto,
  ClientAPIService,
  ClientQuickRegistrationDto,
  ClientViewDto,
  OfficeAPIService,
  OfficeViewDto,
  ServiceCatalogAPIService,
  ServiceCatalogViewDto,
  ShipmentAPIService,
  ShipmentCreationDto
} from '../../../api';

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
  private serviceCatalogApi = inject(ServiceCatalogAPIService);
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
  availableServices = signal<ServiceCatalogViewDto[]>([]);

  // Filtered lists for the UI
  filteredDeliveryOffices = signal<OfficeViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);
  paidByOptions = Object.values(ShipmentCreationDto.PaidByEnum);

  // Hardcoded for now. In reality, get this from the Clerk's profile.
  originOfficeId = 1;

  // Track selected addon IDs
  selectedServiceIds = signal<number[]>([]);

  registerForm = this.fb.group({
    senderSearchTerm: [''],
    senderId: ['', Validators.required],
    selectedSenderDisplay: [''],

    receiverName: ['', Validators.required],
    receiverPhone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    receiverEmail: ['', Validators.email],

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

    type: [ShipmentCreationDto.TypeEnum.Parcel, Validators.required],
    weight: [0.1, [Validators.required, Validators.min(0.1)]],
    paidBy: [ShipmentCreationDto.PaidByEnum.Sender, Validators.required]
  });

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

    queueMicrotask(() => this.loadDropdownData());
  }

  private setupSenderAutocomplete() {
    const senderSearchTermCtrl = this.registerForm.get('senderSearchTerm');
    if (senderSearchTermCtrl) {
      senderSearchTermCtrl.valueChanges.pipe(
        debounceTime(500),
        distinctUntilChanged(),
        tap(() => {
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
        this.isSearchingSender.set(false);
        if (result && result.content) {
          this.senderSearchResults.set(result.content);
        }
      });
    }
  }

  selectSender(client: ClientViewDto) {
    this.registerForm.patchValue({
      senderId: client.id,
      selectedSenderDisplay: `${client.firstName} ${client.lastName} (${client.phoneNumber})`,
      senderSearchTerm: ''
    }, { emitEvent: false });
    this.registerForm.get('senderId')?.updateValueAndValidity({ emitEvent: false });
    this.senderSearchResults.set([]);
  }

  clearSender() {
    this.registerForm.patchValue({
      senderId: '',
      selectedSenderDisplay: '',
      senderSearchTerm: ''
    }, { emitEvent: false });
    this.registerForm.get('senderId')?.updateValueAndValidity({ emitEvent: false });
  }

  openQuickRegister() {
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
    const deliveryCityFilterCtrl = this.registerForm.get('deliveryCityFilterId');
    if (deliveryCityFilterCtrl) {
      deliveryCityFilterCtrl.valueChanges.subscribe(cityId => {
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
      cityFilterCtrl?.clearValidators();
    } else {
      officeCtrl?.clearValidators();
      cityCtrl?.setValidators([Validators.required]);
      streetCtrl?.setValidators([Validators.required]);
      officeCtrl?.setValue(null, { emitEvent: false });
      cityFilterCtrl?.setValue(null, { emitEvent: false });
    }
    [officeCtrl, cityCtrl, streetCtrl, cityFilterCtrl].forEach(c => c?.updateValueAndValidity({ emitEvent: false }));
  }

  private loadDropdownData() {
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
    ).subscribe((result) => {
      if (result) {
        this.allOffices = result.officesRes.content || [];
        this.filteredDeliveryOffices.set(this.allOffices);
        this.cities.set(result.citiesRes.content || []);
        this.availableServices.set(Array.isArray(result.servicesRes) ? result.servicesRes : []);
        this.isLoadingLookups.set(false);
      }
    });
  }

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
        // TypeScript sees this as Set<number> from the generated DTO,
        // but we cast it as `any` because `JSON.stringify` cannot serialize Sets natively,
        // and Jackson in Java expects a regular JSON array to deserialize into a Set.
        selectedServiceIds: this.selectedServiceIds().length > 0 ? (this.selectedServiceIds() as any) : undefined
      };

      payload.originOfficeId = this.originOfficeId;

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
          this.router.navigate(['/app/shipments']).catch(console.error);
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
