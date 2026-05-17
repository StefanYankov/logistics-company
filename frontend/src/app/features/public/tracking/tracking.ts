import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ShipmentAPIService } from '../../../api';
import { ShipmentViewDto } from '../../../api';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-tracking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tracking.html',
  styleUrl: './tracking.css'
})
export class Tracking implements OnInit {
  private route = inject(ActivatedRoute);
  private shipmentApi = inject(ShipmentAPIService);

  shipment = signal<ShipmentViewDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const trackingNumber = this.route.snapshot.paramMap.get('trackingNumber');

    if (trackingNumber) {
      this.trackShipment(trackingNumber);
    } else {
      this.isLoading.set(false);
      this.errorMessage.set('No tracking number provided.');
    }
  }

  private trackShipment(trackingNumber: string): void {
    this.shipmentApi.getShipmentByTrackingNumber(trackingNumber).pipe(
      tap((response: ShipmentViewDto) => {
        this.shipment.set(response);
        this.isLoading.set(false);
      }),
      catchError(err => {
        if (err.status === 404) {
          this.errorMessage.set(`No shipment found for tracking number: ${trackingNumber}`);
        } else {
          this.errorMessage.set('An unexpected error occurred.');
        }
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe();
  }
}
