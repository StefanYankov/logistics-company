import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { ShipmentAPIService } from '../../../api';
import { ShipmentViewDto } from '../../../api';
import { ShipmentStatusUpdateDto } from '../../../api';

@Component({
  selector: 'app-shipment-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shipment-list.html',
  styleUrl: './shipment-list.css'
})
export class ShipmentList implements OnInit {
  private shipmentApi = inject(ShipmentAPIService);

  shipments = signal<ShipmentViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  currentPage = 0;
  pageSize = 50;
  totalElements = signal(0);

  ngOnInit(): void {
    this.loadShipments();
  }

  loadShipments(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const pageParams = { page: this.currentPage, size: this.pageSize };

    this.shipmentApi.getAllShipments(pageParams).pipe(
      tap(response => {
        this.shipments.set(response.content || []);
        this.totalElements.set(response.totalElements || 0);
        this.isLoading.set(false);
      }),
      catchError(_err => {
        this.errorMessage.set('Failed to load shipments.');
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe();
  }

  updateStatus(shipmentId: string, newStatus: ShipmentStatusUpdateDto.NewStatusEnum): void {
    if (!shipmentId) return;

    const payload: ShipmentStatusUpdateDto = { newStatus };

    this.shipmentApi.updateShipmentStatus(shipmentId, payload).pipe(
      tap(() => {
        this.loadShipments();
      }),
      catchError(err => {
        if (err.status === 400 && err.error?.detail) {
           alert(err.error.detail);
        } else {
           alert('Failed to update shipment status.');
        }
        return of(null);
      })
    ).subscribe();
  }
}
