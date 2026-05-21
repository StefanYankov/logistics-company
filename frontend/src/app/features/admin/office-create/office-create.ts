import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormArray, FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {
  AddressDetailsDto,
  CityAPIService,
  CityViewDto,
  CompanyAPIService,
  CompanyViewDto,
  OfficeAPIService,
  OfficeDto
} from '../../../api';
import {forkJoin, of} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';

@Component({
  selector: 'app-office-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './office-create.html',
  styleUrl: './office-create.css'
})
export class OfficeCreate implements OnInit {
  private officeApi = inject(OfficeAPIService);
  private cityApi = inject(CityAPIService);
  private companyApi = inject(CompanyAPIService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  cities = signal<CityViewDto[]>([]);
  companies = signal<CompanyViewDto[]>([]);
  isLoading = signal(true);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  officeForm = this.fb.group({
    companyId: [null as number | null, Validators.required],
    address: this.fb.group({
      cityId: [null as number | null, Validators.required],
      street: ['', Validators.required]
    }),
    operatingHours: this.fb.array([])
  });

  ngOnInit() {
    this.addOperatingHour();
    forkJoin({
      cities: this.cityApi.getAllCities({ page: 0, size: 500 }),
      companies: this.companyApi.getAllCompanies({ page: 0, size: 100 })
    }).subscribe(result => {
      this.cities.set(result.cities.content || []);
      this.companies.set(result.companies.content || []);
      this.isLoading.set(false);
    });
  }

  get operatingHours() {
    return this.officeForm.get('operatingHours') as FormArray;
  }

  addOperatingHour() {
    const hourForm = this.fb.group({
      dayOfWeek: ['MONDAY', Validators.required],
      openTime: ['09:00', Validators.required],
      closeTime: ['18:00', Validators.required],
      isClosed: [false]
    });
    this.operatingHours.push(hourForm);
  }

  removeOperatingHour(index: number) {
    this.operatingHours.removeAt(index);
  }

  onSubmit() {
    if (this.officeForm.invalid) {
      this.officeForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const formValue = this.officeForm.getRawValue();

    if (!formValue.address || !formValue.address.cityId) {
        this.errorMessage.set('City is a required field.');
        this.isSubmitting.set(false);
        return;
    }

    const payload: OfficeDto = {
      companyId: formValue.companyId!,
      address: {
          ...formValue.address,
          cityId: formValue.address.cityId
      } as AddressDetailsDto,
      operatingHours: formValue.operatingHours as any
    };

    this.officeApi.createOffice(payload).pipe(
      tap(() => {
        this.router.navigate(['/app/admin/offices']);
      }),
      catchError(err => {
        this.errorMessage.set(err.error?.detail || 'Failed to create office.');
        this.isSubmitting.set(false);
        return of(null);
      })
    ).subscribe();
  }
}
