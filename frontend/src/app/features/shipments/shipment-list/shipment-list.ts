import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {catchError, tap} from 'rxjs/operators';
import {of} from 'rxjs';
import {FormsModule} from '@angular/forms';

import {
  EmployeeManagementAPIService,
  EmployeeViewDto,
  Pageable,
  ShipmentAPIService,
  ShipmentStatusUpdateDto,
  StaffShipmentViewDto
} from '../../../api';

@Component({
  selector: 'app-shipment-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './shipment-list.html',
  styleUrl: './shipment-list.css'
})
export class ShipmentList implements OnInit {
  private shipmentApi = inject(ShipmentAPIService);
  private employeeApi = inject(EmployeeManagementAPIService);
  public router = inject(Router);

  shipments = signal<StaffShipmentViewDto[]>([]);
  availableCouriers = signal<EmployeeViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  currentPage = 0;
  pageSize = 50;
  totalElements = signal(0);

  // State for Assign Pickup modal
  showAssignPickupModal = signal<string | null>(null);
  selectedCourierForAssignment = signal<string | null>(null);

  ngOnInit(): void {
    this.loadShipments();
    this.loadCouriers();
  }

  loadShipments(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const pageParams = {page: this.currentPage, size: this.pageSize};

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

  loadCouriers(): void {
    const pageable: Pageable = { page: 0, size: 500 };
    this.employeeApi.getAllEmployees(pageable).pipe(
      tap(response => {
        this.availableCouriers.set(response.content?.filter(emp => emp.applicationRole === EmployeeViewDto.ApplicationRoleEnum.Courier) || []);
      }),
      catchError(err => {
        console.error('Failed to load couriers:', err);
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

    const payload: ShipmentStatusUpdateDto = {newStatus};

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

  // Assign Pickup Modal Logic
  openAssignPickupModal(shipmentId: string): void {
    this.showAssignPickupModal.set(shipmentId);
    this.selectedCourierForAssignment.set(null);
  }

  closeAssignPickupModal(): void {
    this.showAssignPickupModal.set(null);
    this.selectedCourierForAssignment.set(null);
  }

  onCourierSelected(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.selectedCourierForAssignment.set(selectElement.value);
  }

  confirmAssignPickup(): void {
    const shipmentId = this.showAssignPickupModal();
    const courierId = this.selectedCourierForAssignment();

    if (!shipmentId || !courierId) {
      alert('Please select a courier to assign.');
      return;
    }

    this.shipmentApi.assignPickup(shipmentId, courierId).pipe(
      tap(() => {
        alert('Pickup assigned successfully!');
        this.closeAssignPickupModal();
        this.loadShipments();
      }),
      catchError(err => {
        console.error('Failed to assign pickup:', err);
        if (err.status === 400 && err.error?.detail) {
          alert(err.error.detail);
        } else {
          alert('Failed to assign pickup. Please try again.');
        }
        return of(null);
      })
    ).subscribe();
  }
}
