import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {OfficeAPIService, OfficeViewDto} from '../../../api';
import {catchError, of} from 'rxjs';

@Component({
  selector: 'app-office-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './office-list.html',
  styleUrl: './office-list.css'
})
export class OfficeList implements OnInit {
  private officeApi = inject(OfficeAPIService);

  offices = signal<OfficeViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit() {
    this.loadOffices();
  }

  loadOffices() {
    this.isLoading.set(true);
    this.officeApi.getAllOffices({ page: 0, size: 100 }).pipe(
      catchError(err => {
        this.errorMessage.set('Failed to load offices.');
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe(response => {
      if (response) {
        this.offices.set(response.content || []);
      }
      this.isLoading.set(false);
    });
  }

  deleteOffice(officeId: number | undefined) {
    if (!officeId) return;

    if (confirm('Are you sure you want to delete this office?')) {
      this.officeApi.deleteOffice(officeId).subscribe({
        next: () => {
          this.loadOffices();
        },
        error: (err) => {
          this.errorMessage.set(err.error?.detail || 'Failed to delete office.');
        }
      });
    }
  }
}
