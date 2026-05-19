import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';
import {forkJoin, of} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {ShipmentAPIService, StaffShipmentViewDto} from '../../../api';
import {AuthService} from '../../../shared/auth.service';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './client-dashboard.html',
  styleUrl: './client-dashboard.css'
})
export class ClientDashboard implements OnInit {
  private shipmentApi = inject(ShipmentAPIService);
  private authService = inject(AuthService);
  private router = inject(Router);

  sentShipments = signal<StaffShipmentViewDto[]>([]);
  receivedShipments = signal<StaffShipmentViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit() {
    this.loadClientShipments();
  }

  private loadClientShipments() {
    const decodedToken = this.authService.getDecodedToken();

    if (decodedToken && (decodedToken.role === 'ROLE_CLERK' || decodedToken.role === 'ROLE_COURIER' || decodedToken.role === 'ROLE_ADMIN')) {
       this.router.navigate(['/app/shipments']);
       return;
    }

    if (!decodedToken || decodedToken.role !== 'ROLE_CLIENT') {
       this.isLoading.set(false);
       return;
    }

    const userId = decodedToken.userId;
    const pageParams = { page: 0, size: 20 };

    forkJoin({
      sent: this.shipmentApi.getShipmentsBySender(userId, pageParams),
      received: this.shipmentApi.getShipmentsByReceiver(userId, pageParams)
    }).pipe(
      catchError(_err => {
        this.errorMessage.set('Failed to load shipments. Please try again.');
        return of(null);
      })
    ).subscribe(result => {
      if (result) {
         this.sentShipments.set(result.sent.content || []);
         this.receivedShipments.set(result.received.content || []);
      }
      this.isLoading.set(false);
    });
  }
}
