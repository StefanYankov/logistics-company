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

  allOffices: OfficeViewDto[] = [];
  cities = signal<CityViewDto[]>([]);
  availableServices = signal<ServiceCatalogViewDto[]>([]);

  filteredOriginOffices = signal<OfficeViewDto[]>([]);
  filteredDeliveryOffices = signal<OfficeViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);
  paidByOptions = Object.values(ShipmentCreationDto.PaidByEnum);

  loggedInUserId: string = '';

  selectedServiceIds = signal<Set<number>>(new Set());

  senderProfileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]]
  });

  registerForm = this.fb.group({
    receiverName: ['', Validators.required],
    receiverPhone: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]],
    receiverEmail: ['', Validators.email],

    // Origin Configuration
    originType: ['OFFICE', Validators.required],
    originCityFilterId: [null as number | null],
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

  ngOnInit() {
    const token = this.authService.getDecodedToken();
    if (!token || !token.userId) {
      this.errorMessage.set('Authentication error. Please log in again.');
      return;
    }
    this.loggedInUserId = token.userId;

    this.setupDynamicValidations();
    this.setupCascadingDropdowns();

    queueMicrotask(() => this.loadDropdownData());
  }

  private setupCascadingDropdowns() {
    this.registerForm.get('originCityFilterId')?.valueChanges.subscribe(cityId => {
      this.registerForm.get('originOfficeId')?.setValue(null, { emitEvent: false });

      if (!cityId) {
        this.filteredOriginOffices.set(this.allOffices);
        return;
      }

      const filtered = this.allOffices.filter(o => o.cityName === this.cities().find(c => c.id == cityId)?.name);
      this.filteredOriginOffices.set(filtered);
    });

    this.registerForm.get('deliveryCityFilterId')?.valueChanges.subscribe(cityId => {
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
    ).subscribe((result) => {
      if (result) {
        this.senderProfileForm.patchValue({
          firstName: result.profileRes.firstName,
          lastName: result.profileRes.lastName,
          phoneNumber: result.profileRes.phoneNumber
        });

        this.allOffices = result.officesRes.content || [];
        this.filteredOriginOffices.set(this.allOffices);
        this.filteredDeliveryOffices.set(this.allOffices);
        this.cities.set(result.citiesRes.content || []);

        const services = Array.isArray(result.servicesRes) ? result.servicesRes : [];
        this.availableServices.set(services);

        this.isLoadingLookups.set(false);
      }
    });
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
                senderId: this.loggedInUserId,
                type: v.type as ShipmentCreationDto.TypeEnum,
                weight: v.weight!,
                paidBy: v.paidBy as ShipmentCreationDto.PaidByEnum,
                receiverName: v.receiverName!,
                receiverPhone: v.receiverPhone!,
                receiverEmail: v.receiverEmail || undefined,
                selectedServiceIds: this.selectedServiceIds().size > 0 ? Array.from(this.selectedServiceIds()) as unknown as Set<number> : undefined
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
