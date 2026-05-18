import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ShipmentAPIService } from '../../../api';
import { StaffShipmentViewDto } from '../../../api';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-shipment-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shipment-details.html',
  styleUrl: './shipment-details.css'
})
export class ShipmentDetails implements OnInit {
  private route = inject(ActivatedRoute);
  private shipmentApi = inject(ShipmentAPIService);

  shipment = signal<StaffShipmentViewDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const shipmentId = this.route.snapshot.paramMap.get('id');

    if (shipmentId) {
      this.loadShipmentDetails(shipmentId);
    } else {
      this.isLoading.set(false);
      this.errorMessage.set('No shipment ID provided.');
    }
  }

  private loadShipmentDetails(id: string): void {
    this.shipmentApi.getStaffShipmentDetails(id).pipe(
      tap((response: StaffShipmentViewDto) => {
        this.shipment.set(response);
        this.isLoading.set(false);
      }),
      catchError(err => {
        if (err.status === 404) {
          this.errorMessage.set(`No shipment found for ID: ${id}`);
        } else {
          this.errorMessage.set('An unexpected error occurred.');
        }
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe();
  }
}
