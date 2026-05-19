import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {catchError, tap} from 'rxjs/operators';
import {of} from 'rxjs';
import {ShipmentAPIService, ShipmentStatusUpdateDto, StaffShipmentViewDto} from '../../../api';

@Component({
  selector: 'app-shipment-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './shipment-list.html',
  styleUrl: './shipment-list.css'
})
export class ShipmentList implements OnInit {
  private shipmentApi = inject(ShipmentAPIService);
  public router = inject(Router);

  shipments = signal<StaffShipmentViewDto[]>([]);
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

    if (newStatus === ShipmentStatusUpdateDto.NewStatusEnum.InTransit) {
      const confirmTransit = window.confirm('Are you sure you want to mark this shipment as "In Transit"? This action indicates the package has left the office.');
      if (!confirmTransit) {
        return;
      }
    }

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
