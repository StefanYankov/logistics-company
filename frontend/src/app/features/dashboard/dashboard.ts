import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ShipmentAPIService } from '../../api';
import { ShipmentViewDto } from '../../api';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private shipmentApi = inject(ShipmentAPIService);

  // Modern Signals for state management
  sentShipments = signal<ShipmentViewDto[]>([]);
  receivedShipments = signal<ShipmentViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit() {
    this.loadClientShipments();
  }

  private loadClientShipments() {
    this.isLoading.set(true);

    setTimeout(() => {
      this.isLoading.set(false);
    }, 500);
  }
}
