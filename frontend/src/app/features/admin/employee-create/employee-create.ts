import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, FormControl, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {EmployeeCreationDto, EmployeeManagementAPIService, OfficeAPIService, OfficeViewDto} from '../../../api';
import {catchError, tap} from 'rxjs/operators';
import {of} from 'rxjs';

interface EmployeeCreateForm {
  username: FormControl<string | null>;
  email: FormControl<string | null>;
  password: FormControl<string | null>;
  firstName: FormControl<string | null>;
  lastName: FormControl<string | null>;
  hireDate: FormControl<string | null>;
  salary: FormControl<number | null>;
  applicationRole: FormControl<EmployeeCreationDto.ApplicationRoleEnum | null>;
  officeId: FormControl<number | null>;
}

@Component({
  selector: 'app-employee-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './employee-create.html',
  styleUrl: './employee-create.css'
})
export class EmployeeCreate implements OnInit {
  private fb = inject(FormBuilder);
  private employeeApi = inject(EmployeeManagementAPIService);
  private officeApi = inject(OfficeAPIService);
  private router = inject(Router);

  offices = signal<OfficeViewDto[]>([]);
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  roles = Object.values(EmployeeCreationDto.ApplicationRoleEnum);

  createForm = this.fb.group<EmployeeCreateForm>({
    username: this.fb.control('', Validators.required),
    email: this.fb.control('', [Validators.required, Validators.email]),
    password: this.fb.control('', [Validators.required, Validators.minLength(8)]),
    firstName: this.fb.control('', Validators.required),
    lastName: this.fb.control('', Validators.required),
    hireDate: this.fb.control(new Date().toISOString().split('T')[0], Validators.required),
    salary: this.fb.control(0, [Validators.required, Validators.min(0)]),
    applicationRole: this.fb.control(EmployeeCreationDto.ApplicationRoleEnum.Clerk, Validators.required),
    officeId: this.fb.control(null)
  });

  ngOnInit() {
    this.officeApi.getAllOffices({ page: 0, size: 500 }).subscribe(response => {
      this.offices.set(response.content || []);
      this.isLoading.set(false);
    });

    this.createForm.get('applicationRole')?.valueChanges.subscribe(role => {
      const officeIdCtrl = this.createForm.get('officeId');
      if (role === EmployeeCreationDto.ApplicationRoleEnum.Clerk) {
        officeIdCtrl?.setValidators([Validators.required]);
      } else {
        officeIdCtrl?.clearValidators();
      }
      officeIdCtrl?.updateValueAndValidity();
    });
  }

  onSubmit() {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const payload = this.createForm.value as EmployeeCreationDto;

    this.employeeApi.createEmployee(payload).pipe(
      tap(() => {
        this.router.navigate(['/app/admin/user-management']);
      }),
      catchError(err => {
        this.errorMessage.set(err.error?.detail || 'Failed to create employee.');
        this.isSubmitting.set(false);
        return of(null);
      })
    ).subscribe();
  }
}
