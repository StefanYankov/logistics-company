import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {forkJoin, of} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {
  AdminPasswordResetDto,
  ClientAPIService,
  ClientViewDto,
  EmployeeManagementAPIService,
  EmployeeViewDto,
  Pageable
} from '../../../api';
import {RouterModule} from '@angular/router';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './user-management.html',
  styleUrl: './user-management.css',
})
export class UserManagement implements OnInit {
  private employeeApi = inject(EmployeeManagementAPIService);
  private clientApi = inject(ClientAPIService);

  employees = signal<EmployeeViewDto[]>([]);
  clients = signal<ClientViewDto[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  activeTab = signal<'staff' | 'clients'>('staff');

  ngOnInit(): void {
    this.loadAllUsers();
  }

  loadAllUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const pageable: Pageable = { page: 0, size: 100, sort: [] };

    forkJoin({
      employees: this.employeeApi.getAllEmployees(pageable),
      clients: this.clientApi.getAllClients(pageable)
    }).pipe(
      tap(response => {
        console.log('API Response:', response);
        this.employees.set(response.employees.content || []);
        this.clients.set(response.clients.content || []);
        this.isLoading.set(false);
      }),
      catchError(_err => {
        this.errorMessage.set('Failed to load user data.');
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe();
  }

  selectTab(tab: 'staff' | 'clients'): void {
    this.activeTab.set(tab);
  }

  toggleEmployeeStatus(employee: EmployeeViewDto): void {
    if (!employee.id) return;

    const action = employee.isActive
      ? this.employeeApi.deactivateEmployee(employee.id)
      : this.employeeApi.activateEmployee(employee.id);

    action.subscribe({
      next: () => this.loadAllUsers(),
      error: () => alert('Failed to update employee status.')
    });
  }

  toggleClientStatus(client: ClientViewDto): void {
    if (!client.id) return;

    const action = client.isActive
      ? this.clientApi.deactivateClient(client.id)
      : this.clientApi.activateClient(client.id);

    action.subscribe({
      next: () => this.loadAllUsers(),
      error: () => alert('Failed to update client status.')
    });
  }

  openResetPasswordModal(employee: EmployeeViewDto): void {
    if (!employee.id) return;

    const newPassword = prompt(`Enter new temporary password for ${employee.firstName} ${employee.lastName}:`);

    if (newPassword && newPassword.length >= 8) {
      const payload: AdminPasswordResetDto = { newPassword };
      this.employeeApi.forcePasswordReset(employee.id, payload).subscribe({
        next: () => alert('Password has been reset successfully.'),
        error: () => alert('Failed to reset password.')
      });
    } else if (newPassword) {
      alert('Password must be at least 8 characters long.');
    }
  }
}
