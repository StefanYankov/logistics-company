import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, startWith } from 'rxjs/operators';
import { ShipmentAPIService, ClientAPIService, OfficeAPIService, CityAPIService } from '../../../api';
import { ClientViewDto, OfficeViewDto, CityViewDto, ShipmentCreationDto } from '../../../api';
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
  private authService = inject(AuthService);
  private router = inject(Router);

  isLoadingLookups = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  clients = signal<ClientViewDto[]>([]);
  offices = signal<OfficeViewDto[]>([]);
  cities = signal<CityViewDto[]>([]);

  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);
  paidByOptions = Object.values(ShipmentCreationDto.PaidByEnum);

  loggedInUserId: string = '';

  registerForm = this.fb.group({
    // Receiver Configuration
    receiverType: ['REGISTERED', Validators.required],
    receiverId: [''],
    receiverName: [''],
    receiverPhone: [''],
    receiverEmail: [''],

    // Origin Configuration
    originType: ['OFFICE', Validators.required],
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

    // Production-grade microtask deferral
    queueMicrotask(() => this.loadDropdownData());
  }

  private setupDynamicValidations() {
    // Receiver Validation
    const receiverTypeCtrl = this.registerForm.get('receiverType');
    receiverTypeCtrl?.valueChanges.pipe(startWith(receiverTypeCtrl.value)).subscribe(type => {
      this.syncReceiverValidators(type);
    });

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

  private syncOriginValidators(type: string | null) {
    const officeCtrl = this.registerForm.get('originOfficeId');
    const cityCtrl = this.registerForm.get('originCityId');
    const streetCtrl = this.registerForm.get('originStreet');

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
      clientsRes: this.clientApi.getAllClients(pageParams),
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
        this.clients.set(result.clientsRes.content || []);
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
        senderId: this.loggedInUserId, // Implicitly set
        type: v.type as ShipmentCreationDto.TypeEnum,
        weight: v.weight!,
        paidBy: v.paidBy as ShipmentCreationDto.PaidByEnum
      };

      // Receiver
      if (v.receiverType === 'REGISTERED') {
        payload.receiverId = v.receiverId!;
      } else {
        payload.receiverName = v.receiverName!;
        payload.receiverPhone = v.receiverPhone!;
        if (v.receiverEmail) payload.receiverEmail = v.receiverEmail;
      }

      // Origin - Omit the opposite to pass backend XOR validation
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
          this.router.navigate(['/app']).catch(console.error);
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
