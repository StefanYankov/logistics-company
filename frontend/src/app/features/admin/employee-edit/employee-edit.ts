import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {forkJoin, of} from 'rxjs';
import {catchError, switchMap, tap} from 'rxjs/operators';
import {
  EmployeeManagementAPIService,
  EmployeeUpdateDto,
  EmployeeViewDto,
  OfficeAPIService,
  OfficeViewDto
} from '../../../api';

@Component({
  selector: 'app-employee-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './employee-edit.html',
  styleUrl: './employee-edit.css'
})
export class EmployeeEdit implements OnInit {
  private fb = inject(FormBuilder);
  private employeeApi = inject(EmployeeManagementAPIService);
  private officeApi = inject(OfficeAPIService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  employeeId = signal<string | null>(null);
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  offices = signal<OfficeViewDto[]>([]);

  editForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    salary: [0, [Validators.required, Validators.min(0)]],
    officeId: [null as number | null]
  });

  ngOnInit() {
    this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (!id) {
          this.errorMessage.set('No employee ID provided.');
          this.isLoading.set(false);
          return of(null);
        }
        this.employeeId.set(id);
        return this.loadInitialData(id);
      })
    ).subscribe();
  }

  private loadInitialData(employeeId: string) {
    return forkJoin({
      employee: this.employeeApi.getEmployeeById(employeeId),
      offices: this.officeApi.getAllOffices({ page: 0, size: 500 })
    }).pipe(
      tap(result => {
        this.populateForm(result.employee);
        this.offices.set(result.offices.content || []);
        this.isLoading.set(false);
      }),
      catchError(_err => {
        this.errorMessage.set('Failed to load employee data for editing.');
        this.isLoading.set(false);
        return of(null);
      })
    );
  }

  private populateForm(employee: EmployeeViewDto) {
    this.editForm.patchValue({
      email: employee.email,
      firstName: employee.firstName,
      lastName: employee.lastName,
      salary: employee.salary,
      officeId: employee.officeId
    });
  }

  onSubmit() {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const payload = this.editForm.value as EmployeeUpdateDto;

    this.employeeApi.updateEmployeeBasicInfo(this.employeeId()!, payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/app/admin/user-management']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.detail || 'Failed to update employee.');
      }
    });
  }
}
