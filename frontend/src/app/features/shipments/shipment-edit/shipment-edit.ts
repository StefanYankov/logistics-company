import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormControl, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {forkJoin, of} from 'rxjs';
import {catchError, startWith, switchMap, tap} from 'rxjs/operators';
import {
  CityAPIService,
  CityViewDto,
  OfficeAPIService,
  OfficeViewDto,
  ServiceCatalogAPIService,
  ServiceCatalogViewDto,
  ShipmentAPIService,
  ShipmentUpdateDto,
  StaffShipmentViewDto
} from '../../../api';
import {HttpErrorResponse} from '@angular/common/http';

// Define a typed interface for our form controls.
// This ensures that controls like 'type' and 'paidBy' are strongly typed to their respective enums,
// preventing type inference from locking them into a single literal value (e.g., 'PARCEL').
interface ShipmentEditForm {
  receiverName: FormControl<string | null>;
  receiverPhone: FormControl<string | null>;
  receiverEmail: FormControl<string | null>;
  destinationType: FormControl<string | null>;
  deliveryCityFilterId: FormControl<number | null>;
  deliveryOfficeId: FormControl<number | null>;
  deliveryCityId: FormControl<number | null>;
  deliveryStreet: FormControl<string | null>;
  deliveryDistrict: FormControl<string | null>;
  deliveryBuilding: FormControl<string | null>;
  deliveryEntrance: FormControl<string | null>;
  deliveryFloor: FormControl<string | null>;
  deliveryApartment: FormControl<string | null>;
  type: FormControl<ShipmentUpdateDto.TypeEnum | null>;
  weight: FormControl<number | null>;
  paidBy: FormControl<ShipmentUpdateDto.PaidByEnum | null>;
  isPaid: FormControl<boolean | null>;
}

@Component({
  selector: 'app-shipment-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './shipment-edit.html',
  styleUrl: './shipment-edit.css'
})
export class ShipmentEdit implements OnInit {
  private fb = inject(FormBuilder);
  private shipmentApi = inject(ShipmentAPIService);
  private officeApi = inject(OfficeAPIService);
  private cityApi = inject(CityAPIService);
  private serviceCatalogApi = inject(ServiceCatalogAPIService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  shipmentId = signal<string | null>(null);
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  allOffices: OfficeViewDto[] = [];
  cities = signal<CityViewDto[]>([]);
  availableServices = signal<ServiceCatalogViewDto[]>([]);
  filteredDeliveryOffices = signal<OfficeViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentUpdateDto.TypeEnum);
  paidByOptions = Object.values(ShipmentUpdateDto.PaidByEnum);

  selectedServiceIds = signal<number[]>([]);

  // Use the strongly-typed interface when creating the form group.
  editForm = this.fb.group<ShipmentEditForm>({
    receiverName: this.fb.control('', Validators.required),
    receiverPhone: this.fb.control('', [Validators.required, Validators.pattern(/^\+?[0-9]{8,15}$/)]),
    receiverEmail: this.fb.control('', Validators.email),
    destinationType: this.fb.control('OFFICE', Validators.required),
    deliveryCityFilterId: this.fb.control(null),
    deliveryOfficeId: this.fb.control(null),
    deliveryCityId: this.fb.control(null),
    deliveryStreet: this.fb.control(''),
    deliveryDistrict: this.fb.control(''),
    deliveryBuilding: this.fb.control(''),
    deliveryEntrance: this.fb.control(''),
    deliveryFloor: this.fb.control(''),
    deliveryApartment: this.fb.control(''),
    type: this.fb.control(ShipmentUpdateDto.TypeEnum.Parcel, Validators.required),
    weight: this.fb.control(0.1, [Validators.required, Validators.min(0.1)]),
    paidBy: this.fb.control(ShipmentUpdateDto.PaidByEnum.Sender, Validators.required),
    isPaid: this.fb.control(false)
  });

  ngOnInit() {
    this.setupDynamicValidations();
    this.setupCascadingDropdowns();

    this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (!id) {
          this.errorMessage.set('No shipment ID provided.');
          this.isLoading.set(false);
          return of(null);
        }
        this.shipmentId.set(id);
        return this.loadInitialData(id);
      })
    ).subscribe();
  }

  private loadInitialData(shipmentId: string) {
    const pageParams = { page: 0, size: 500 };
    return forkJoin({
      shipment: this.shipmentApi.getStaffShipmentDetails(shipmentId),
      offices: this.officeApi.getAllOffices(pageParams),
      cities: this.cityApi.getAllCities(pageParams),
      services: this.serviceCatalogApi.getAllServices()
    }).pipe(
      tap(result => {
        // Set available services first, so they are available in populateForm
        this.availableServices.set(Array.isArray(result.services) ? result.services : []);
        this.populateForm(result.shipment);
        this.allOffices = result.offices.content || [];
        this.filteredDeliveryOffices.set(this.allOffices);
        this.cities.set(result.cities.content || []);
        this.isLoading.set(false);
      }),
      catchError((err: HttpErrorResponse) => {
        this.errorMessage.set(err.error?.detail || 'Failed to load shipment data for editing.');
        this.isLoading.set(false);
        return of(null);
      })
    );
  }

  private populateForm(shipment: StaffShipmentViewDto) {
    // With the form now strongly typed, we can patch the values directly.
    // TypeScript knows that `shipment.type` (e.g., 'PARCEL') is a valid value for a control of type `FormControl<ShipmentUpdateDto.TypeEnum | null>`.
    this.editForm.patchValue({
      receiverName: shipment.receiverName,
      receiverPhone: shipment.receiverPhone,
      type: shipment.type,
      weight: shipment.weight,
      paidBy: shipment.paidBy,
      isPaid: shipment.isPaid
    });

    if (shipment.deliveryOfficeId) {
      this.editForm.patchValue({
        destinationType: 'OFFICE',
        deliveryOfficeId: shipment.deliveryOfficeId
      });
    } else if (shipment.deliveryAddressString) {
      this.editForm.patchValue({ destinationType: 'ADDRESS' });
    }

    const serviceIds = shipment.appliedAddons?.map(addonName => {
        const service = this.availableServices().find(s => s.name === addonName);
        return service?.id;
    }).filter((id): id is number => id !== undefined) || [];
    this.selectedServiceIds.set(serviceIds);
  }

  private setupCascadingDropdowns() {
    this.editForm.get('deliveryCityFilterId')?.valueChanges.subscribe(cityId => {
      this.editForm.get('deliveryOfficeId')?.setValue(null, { emitEvent: false });
      if (!cityId) {
        this.filteredDeliveryOffices.set(this.allOffices);
        return;
      }
      const filtered = this.allOffices.filter(o => o.cityName === this.cities().find(c => c.id == cityId)?.name);
      this.filteredDeliveryOffices.set(filtered);
    });
  }

  private setupDynamicValidations() {
    this.editForm.get('destinationType')?.valueChanges.pipe(startWith(this.editForm.get('destinationType')?.value)).subscribe(type => {
      this.syncDestinationValidators(type as string | null);
    });
  }

  private syncDestinationValidators(type: string | null) {
    const officeCtrl = this.editForm.get('deliveryOfficeId');
    const cityCtrl = this.editForm.get('deliveryCityId');
    const streetCtrl = this.editForm.get('deliveryStreet');

    if (type === 'OFFICE') {
      officeCtrl?.setValidators([Validators.required]);
      cityCtrl?.clearValidators();
      streetCtrl?.clearValidators();
    } else {
      officeCtrl?.clearValidators();
      cityCtrl?.setValidators([Validators.required]);
      streetCtrl?.setValidators([Validators.required]);
    }
    [officeCtrl, cityCtrl, streetCtrl].forEach(c => c?.updateValueAndValidity());
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
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const v = this.editForm.value;
    const payload: ShipmentUpdateDto = {
      type: v.type as ShipmentUpdateDto.TypeEnum,
      weight: v.weight!,
      paidBy: v.paidBy as ShipmentUpdateDto.PaidByEnum,
      isPaid: v.isPaid!,
      receiverName: v.receiverName!,
      receiverPhone: v.receiverPhone!,
      receiverEmail: v.receiverEmail || undefined,
      selectedServiceIds: this.selectedServiceIds() as any
    };

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

    this.shipmentApi.updateShipment(this.shipmentId()!, payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/app/shipments', this.shipmentId()!]);
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.detail || 'Failed to update shipment.');
      }
    });
  }
}
