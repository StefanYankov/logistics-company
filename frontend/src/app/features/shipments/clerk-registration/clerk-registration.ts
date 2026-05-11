import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, startWith, switchMap, tap } from 'rxjs/operators';
import { ShipmentAPIService, ClientAPIService, OfficeAPIService, CityAPIService } from '../../../api';
import { ClientViewDto, OfficeViewDto, CityViewDto, ShipmentCreationDto, ClientQuickRegistrationDto } from '../../../api';

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

  offices = signal<OfficeViewDto[]>([]);
  cities = signal<CityViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);
  paidByOptions = Object.values(ShipmentCreationDto.PaidByEnum);

  // Hardcoded for now. In reality, get this from the Clerk's profile.
  originOfficeId = 1;

  registerForm = this.fb.group({
    // Sender Configuration (Autocomplete)
    senderSearchTerm: [''],
    senderId: ['', Validators.required],
    selectedSenderDisplay: [''], // Just for showing the selected name

    // Receiver Configuration
    receiverType: ['REGISTERED', Validators.required],
    receiverId: [''],
    receiverName: [''],
    receiverPhone: [''],
    receiverEmail: [''],

    // Destination Configuration
    destinationType: ['OFFICE', Validators.required],
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
    email: ['', Validators.email] // Optional but must be valid if provided
  });

  ngOnInit() {
    this.setupDynamicValidations();
    this.setupSenderAutocomplete();

    // Production-grade microtask deferral
    queueMicrotask(() => this.loadDropdownData());
  }

  private setupSenderAutocomplete() {
    this.registerForm.get('senderSearchTerm')?.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => {
          this.isSearchingSender.set(true);
          this.showQuickRegister.set(false); // Hide quick register if they start typing again

          // Clear senderId to enforce re-selection
          this.registerForm.get('senderId')?.setValue('', { emitEvent: false });
          this.registerForm.get('senderId')?.markAsTouched();
          this.registerForm.get('senderId')?.updateValueAndValidity({ emitEvent: false });
      }),
      switchMap(term => {
        if (!term || term.length < 2) {
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

  private setupDynamicValidations() {
    // Receiver Validation
    const receiverTypeCtrl = this.registerForm.get('receiverType');
    receiverTypeCtrl?.valueChanges.pipe(startWith(receiverTypeCtrl.value)).subscribe(type => {
      this.syncReceiverValidators(type);
    });

    // Destination Validation
    const destTypeCtrl = this.registerForm.get('destinationType');
    destTypeCtrl?.valueChanges.pipe(startWith(destTypeCtrl.value)).subscribe(type => {
      this.syncDestinationValidators(type);
    });
  }

  private syncReceiverValidators(type: string | null) {
    const idCtrl = this.registerForm.get('receiverId');
    const nameCtrl = this.registerForm.get('receiverName');
    const phoneCtrl = this.registerForm.get('receiverPhone');

    if (type === 'REGISTERED') {
      idCtrl?.setValidators([Validators.required]);
      nameCtrl?.clearValidators();
      phoneCtrl?.clearValidators();
      nameCtrl?.setValue('', { emitEvent: false });
      phoneCtrl?.setValue('', { emitEvent: false });
    } else {
      idCtrl?.clearValidators();
      nameCtrl?.setValidators([Validators.required]);
      phoneCtrl?.setValidators([Validators.required]);
      idCtrl?.setValue('', { emitEvent: false });
    }
    [idCtrl, nameCtrl, phoneCtrl].forEach(c => c?.updateValueAndValidity({ emitEvent: false }));
  }

  private syncDestinationValidators(type: string | null) {
    const officeCtrl = this.registerForm.get('deliveryOfficeId');
    const cityCtrl = this.registerForm.get('deliveryCityId');
    const streetCtrl = this.registerForm.get('deliveryStreet');

    if (type === 'OFFICE') {
      officeCtrl?.setValidators([Validators.required]);
      cityCtrl?.clearValidators();
      streetCtrl?.clearValidators();
      cityCtrl?.setValue(null, { emitEvent: false });
      streetCtrl?.setValue('', { emitEvent: false });
    } else {
      officeCtrl?.clearValidators();
      cityCtrl?.setValidators([Validators.required]);
      streetCtrl?.setValidators([Validators.required]);
      officeCtrl?.setValue(null, { emitEvent: false });
    }
    [officeCtrl, cityCtrl, streetCtrl].forEach(c => c?.updateValueAndValidity({ emitEvent: false }));
  }

  private loadDropdownData() {
    this.isLoadingLookups.set(true);
    const pageParams = { page: 0, size: 500 };

    forkJoin({
      officesRes: this.officeApi.getAllOffices(pageParams),
      citiesRes: this.cityApi.getAllCities(pageParams)
    }).pipe(
      catchError(() => {
        this.errorMessage.set('Failed to load form data. Please try again.');
        this.isLoadingLookups.set(false);
        return of(null);
      })
    ).subscribe(result => {
      if (result) {
        this.offices.set(result.officesRes.content || []);
        this.cities.set(result.citiesRes.content || []);
        this.isLoadingLookups.set(false);
      }
    });
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
        paidBy: v.paidBy as ShipmentCreationDto.PaidByEnum
      };

      // We explicitly lock Origin to the Clerk's current office
      payload.originOfficeId = this.originOfficeId;

      // Receiver
      if (v.receiverType === 'REGISTERED') {
        payload.receiverId = v.receiverId!;
      } else {
        payload.receiverName = v.receiverName!;
        payload.receiverPhone = v.receiverPhone!;
        if (v.receiverEmail) payload.receiverEmail = v.receiverEmail;
      }

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
