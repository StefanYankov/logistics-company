import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {forkJoin, of} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {ShipmentAPIService, StaffShipmentViewDto} from '../../../api';
import {AuthService} from '../../../shared/auth.service';

@Component({
  selector: 'app-courier-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './courier-dashboard.html',
  styleUrl: './courier-dashboard.css'
})
export class CourierDashboard implements OnInit {
  private shipmentApi = inject(ShipmentAPIService);
  private authService = inject(AuthService);

  deliveryTasks = signal<StaffShipmentViewDto[]>([]);
  pickupTasks = signal<StaffShipmentViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit() {
    this.loadCourierTasks();
  }

  private loadCourierTasks() {
    const decodedToken = this.authService.getDecodedToken();
    if (!decodedToken || decodedToken.role !== 'ROLE_COURIER') {
      this.errorMessage.set('You are not authorized to view this page.');
      this.isLoading.set(false);
      return;
    }

    const pageParams = { page: 0, size: 100 };

    forkJoin({
      deliveries: this.shipmentApi.getMyDeliveries(pageParams),
      pickups: this.shipmentApi.getMyPickups(pageParams)
    }).pipe(
      catchError(_err => {
        this.errorMessage.set('Failed to load courier tasks. Please try again.');
        return of(null);
      })
    ).subscribe(result => {
      if (result) {
        this.deliveryTasks.set(result.deliveries.content || []);
        this.pickupTasks.set(result.pickups.content || []);
      }
      this.isLoading.set(false);
    });
  }
}
