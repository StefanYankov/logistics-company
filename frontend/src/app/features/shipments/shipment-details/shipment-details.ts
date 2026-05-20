import {Component, computed, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, RouterModule} from '@angular/router';
import {ShipmentAPIService, StaffShipmentViewDto} from '../../../api';
import {catchError, tap} from 'rxjs/operators';
import {of} from 'rxjs';
import {AuthService} from '../../../shared/auth.service';

@Component({
  selector: 'app-shipment-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './shipment-details.html',
  styleUrl: './shipment-details.css'
})
export class ShipmentDetails implements OnInit {
  private route = inject(ActivatedRoute);
  private shipmentApi = inject(ShipmentAPIService);
  private authService = inject(AuthService);

  shipment = signal<StaffShipmentViewDto | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  // Computed signal to determine if the edit button should be shown
  canEdit = computed(() => {
    const s = this.shipment();
    if (!s) return false;

    const user = this.authService.getDecodedToken();
    if (!user) return false;

    const isRegistered = s.status === 'REGISTERED';
    const isStaff = user.role === 'ROLE_ADMIN' || user.role === 'ROLE_CLERK';
    const isOwner = user.role === 'ROLE_CLIENT' && user.userId === s.senderId;

    return isRegistered && (isStaff || isOwner);
  });

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
