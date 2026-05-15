import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of, EMPTY, Observable } from 'rxjs';
import { catchError, startWith, concatMap, tap } from 'rxjs/operators';
import { ShipmentAPIService, OfficeAPIService, CityAPIService, ClientAPIService, ServiceCatalogAPIService } from '../../../api';
import { OfficeViewDto, CityViewDto, ShipmentCreationDto, ClientUpdateDto, ServiceCatalogViewDto } from '../../../api';
import { AuthService } from '../../../shared/auth.service';

@Component({
  selector: 'app-client-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './client-registration.html',
  styleUrl: './client-registration.css'
})
export class ClientRegistration implements OnInit {
  private fb = inject(FormBuilder);
  private shipmentApi = inject(ShipmentAPIService);
  private clientApi = inject(ClientAPIService);
  private officeApi = inject(OfficeAPIService);
  private cityApi = inject(CityAPIService);
  private serviceCatalogApi = inject(ServiceCatalogAPIService);
  private authService = inject(AuthService);
  private router = inject(Router);

  isLoadingLookups = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  // Source of truth
  allOffices: OfficeViewDto[] = [];
  cities = signal<CityViewDto[]>([]);
  availableServices = signal<ServiceCatalogViewDto[]>([]);

  // Filtered lists for the UI
  filteredOriginOffices = signal<OfficeViewDto[]>([]);
  filteredDeliveryOffices = signal<OfficeViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);
  paidByOptions = Object.values(ShipmentCreationDto.PaidByEnum);

  loggedInUserId: string = '';

  // Track selected addon IDs
  selectedServiceIds = signal<Set<number>>(new Set());

  senderProfileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]]
  });

  registerForm = this.fb.group({
    // Receiver Configuration
    receiverName: ['', Validators.required],
    receiverPhone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    receiverEmail: ['', Validators.email],

    // Origin Configuration
    originType: ['OFFICE', Validators.required],
    originCityFilterId: [null as number | null], // New filter control
    originOfficeId: [null as number | null],
    originCityId: [null as number | null],
    originStreet: [''],
    originDistrict: [''],
    originBuilding: [''],
    originEntrance: [''],
    originFloor: [''],
    originApartment: [''],

    // Destination Configuration
    destinationType: ['OFFICE', Validators.required],
    deliveryCityFilterId: [null as number | null], // New filter control
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

  ngOnInit() {
    const token = this.authService.getDecodedToken();
    if (!token || !token.userId) {
      this.errorMessage.set('Authentication error. Please log in again.');
      return;
    }
    this.loggedInUserId = token.userId;

    this.setupDynamicValidations();
    this.setupCascadingDropdowns(); // New setup

    // Production-grade microtask deferral
    queueMicrotask(() => this.loadDropdownData());
  }

  private setupCascadingDropdowns() {
    // Watch Origin City Filter
    this.registerForm.get('originCityFilterId')?.valueChanges.subscribe(cityId => {
      // Reset the selected office when the city changes
      this.registerForm.get('originOfficeId')?.setValue(null, { emitEvent: false });

      if (!cityId) {
        // If no city selected, show all offices
        this.filteredOriginOffices.set(this.allOffices);
        return;
      }

      // We assume cityId comes in as a string from the HTML select, so we use loose equality or parseInt
      const filtered = this.allOffices.filter(o => o.cityName === this.cities().find(c => c.id == cityId)?.name);
      this.filteredOriginOffices.set(filtered);
    });

    // Watch Delivery City Filter
    this.registerForm.get('deliveryCityFilterId')?.valueChanges.subscribe(cityId => {
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

  private setupDynamicValidations() {
    // Origin Validation
    const originTypeCtrl = this.registerForm.get('originType');
    originTypeCtrl?.valueChanges.pipe(startWith(originTypeCtrl.value)).subscribe(type => {
      this.syncOriginValidators(type);
    });

    // Destination Validation
    const destTypeCtrl = this.registerForm.get('destinationType');
    destTypeCtrl?.valueChanges.pipe(startWith(destTypeCtrl.value)).subscribe(type => {
      this.syncDestinationValidators(type);
    });
  }

  private syncOriginValidators(type: string | null) {
    const officeCtrl = this.registerForm.get('originOfficeId');
    const cityCtrl = this.registerForm.get('originCityId');
    const streetCtrl = this.registerForm.get('originStreet');
    const cityFilterCtrl = this.registerForm.get('originCityFilterId'); // New control

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

  private syncDestinationValidators(type: string | null) {
    const officeCtrl = this.registerForm.get('deliveryOfficeId');
    const cityCtrl = this.registerForm.get('deliveryCityId');
    const streetCtrl = this.registerForm.get('deliveryStreet');
    const cityFilterCtrl = this.registerForm.get('deliveryCityFilterId'); // New control

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
      profileRes: this.clientApi.getMyProfile(),
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
    ).subscribe(async (result) => { // Mark handler as async
      if (result) {
        try {
          // Pre-fill profile (already parses correctly as object)
          this.senderProfileForm.patchValue({
            firstName: result.profileRes.firstName,
            lastName: result.profileRes.lastName,
            phoneNumber: result.profileRes.phoneNumber
          });

          // Unpack Blobs safely into structured data
          const officesData = await this.parseBlobResponse(result.officesRes);
          const citiesData = await this.parseBlobResponse(result.citiesRes);
          const servicesData = await this.parseBlobResponse(result.servicesRes);

          this.allOffices = officesData?.content || [];

          // Apply changes to signals outside the constructor flow
          this.filteredOriginOffices.set(this.allOffices);
          this.filteredDeliveryOffices.set(this.allOffices);
          this.cities.set(citiesData?.content || []);
          this.availableServices.set(Array.isArray(servicesData) ? servicesData : []);

          console.log("DEBUG: availableServices signal successfully set to:", this.availableServices());
        } catch (parseError) {
          console.error("Failed to process API Blob response payloads:", parseError);
          this.errorMessage.set('Data corruption error. Please reload.');
        } finally {
          this.isLoadingLookups.set(false);
        }
      }
    });
  }

  /**
   * Helper utility to convert openapi generated Blob response streams to runtime JSON.
   */
  private async parseBlobResponse(response: any): Promise<any> {
    if (response instanceof Blob) {
      const text = await response.text();
      return text ? JSON.parse(text) : null;
    }
    return response; // Fallback if the client layer later returns objects directly
  }

  toggleService(serviceId: number | undefined, event: Event) {
    if (!serviceId) return;
    const isChecked = (event.target as HTMLInputElement).checked;

    this.selectedServiceIds.update(set => {
      const newSet = new Set(set);
      if (isChecked) {
        newSet.add(serviceId);
      } else {
        newSet.delete(serviceId);
      }
      return newSet;
    });
  }

  onSubmit() {
    if (this.registerForm.valid && this.senderProfileForm.valid) {
      this.isSubmitting.set(true);
      this.errorMessage.set(null);

      const profileUpdate$: Observable<any> = this.senderProfileForm.dirty
          ? this.clientApi.updateMyProfile(this.senderProfileForm.value as ClientUpdateDto)
          : of(null);

      profileUpdate$.pipe(
          concatMap(() => {
              const v = this.registerForm.value;
              const payload: ShipmentCreationDto = {
                senderId: this.loggedInUserId, // Implicitly set
                type: v.type as ShipmentCreationDto.TypeEnum,
                weight: v.weight!,
                paidBy: v.paidBy as ShipmentCreationDto.PaidByEnum,
                receiverName: v.receiverName!,
                receiverPhone: v.receiverPhone!,
                receiverEmail: v.receiverEmail || undefined,
                selectedServiceIds: this.selectedServiceIds().size > 0 ? this.selectedServiceIds() : undefined
              };

              // Origin
              if (v.originType === 'OFFICE') {
                payload.originOfficeId = v.originOfficeId!;
              } else {
                payload.originAddress = {
                  cityId: v.originCityId!,
                  street: v.originStreet!,
                  district: v.originDistrict || undefined,
                  building: v.originBuilding || undefined,
                  entrance: v.originEntrance || undefined,
                  floor: v.originFloor || undefined,
                  apartment: v.originApartment || undefined
                };
              }

              // Destination
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

              return this.shipmentApi.registerShipment(payload);
          }),
          tap(() => {
            this.isSubmitting.set(false);
            this.router.navigate(['/app']).catch(console.error);
          }),
          catchError((err) => {
            this.isSubmitting.set(false);
            if (err.status === 409 && err.error?.errorCode === 'E3005') {
                 // E3005 is our new PHONE_DUPLICATE error code
                 this.errorMessage.set('The phone number you provided is already registered to another account.');
            } else if (err.status === 400 && err.error?.detail) {
                 this.errorMessage.set(err.error.detail);
            } else {
                 this.errorMessage.set('An unexpected error occurred while processing your request.');
            }
            return EMPTY;
          })
      ).subscribe();

    } else {
      this.registerForm.markAllAsTouched();
      this.senderProfileForm.markAllAsTouched();
    }
  }
}
