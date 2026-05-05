import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ShipmentAPIService, ClientAPIService, OfficeAPIService } from '../../../api';
import { ClientViewDto, OfficeViewDto, ShipmentCreationDto } from '../../../api';

@Component({
  selector: 'app-register-shipment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register-shipment.html',
  styleUrl: './register-shipment.css'
})
export class RegisterShipment implements OnInit {

  private fb = inject(FormBuilder);
  private shipmentApi = inject(ShipmentAPIService);
  private clientApi = inject(ClientAPIService);
  private officeApi = inject(OfficeAPIService);
  private router = inject(Router);

  isLoadingLookups = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  clients = signal<ClientViewDto[]>([]);
  offices = signal<OfficeViewDto[]>([]);

  // Expose the enum to the template for the dropdown options
  shipmentTypes = Object.values(ShipmentCreationDto.TypeEnum);

  // --- Form Definition ---
  registerForm = this.fb.group({
    senderId: ['', Validators.required],
    receiverId: ['', Validators.required],
    type: [ShipmentCreationDto.TypeEnum.Parcel, Validators.required],
    weight: [0.1, [Validators.required, Validators.min(0.1)]],
    deliveryOfficeId: [null as number | null, Validators.required]
  });

  ngOnInit() {
    this.loadDropdownData();
  }

  /**
   * Fetches the lists of Clients and Offices required to populate the form dropdowns.
   * Uses forkJoin to fetch both simultaneously for performance.
   */
  private loadDropdownData() {
    this.isLoadingLookups.set(true);

    // TODO: modify to use autocomplete/searchable dropdowns instead of loading all.
    const pageParams = { page: 0, size: 500 };

    forkJoin({
      clientsRes: this.clientApi.getAllClients(pageParams),
      officesRes: this.officeApi.getAllOffices(pageParams)
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
        this.isLoadingLookups.set(false);
      }
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      this.isSubmitting.set(true);
      this.errorMessage.set(null);

      const payload: ShipmentCreationDto = this.registerForm.value as any;

      this.shipmentApi.registerShipment(payload).pipe(
        tap(() => {
          this.isSubmitting.set(false);
          this.router.navigate(['/app']);
        }),
        catchError(err => {
          this.isSubmitting.set(false);
          if (err.status === 400 && err.error?.detail) {
            this.errorMessage.set(err.error.detail);
          } else {
            this.errorMessage.set('An unexpected error occurred while registering the shipment.');
          }
          return of(null);
        })
      ).subscribe();

    } else {
      this.registerForm.markAllAsTouched();
    }
  }
}
