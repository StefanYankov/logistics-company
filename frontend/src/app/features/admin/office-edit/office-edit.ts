import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormArray, FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {
  CityAPIService,
  CityViewDto,
  CompanyAPIService,
  CompanyViewDto,
  OfficeAPIService,
  OfficeDto,
  OfficeViewDto,
  OperatingHourDto
} from '../../../api';
import {forkJoin, of} from 'rxjs';
import {catchError, switchMap, tap} from 'rxjs/operators';

@Component({
  selector: 'app-office-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './office-edit.html',
  styleUrl: './office-edit.css'
})
export class OfficeEdit implements OnInit {
  private officeApi = inject(OfficeAPIService);
  private cityApi = inject(CityAPIService);
  private companyApi = inject(CompanyAPIService);
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  officeId = signal<number | null>(null);
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
    this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (!id) {
          this.errorMessage.set('No office ID provided.');
          this.isLoading.set(false);
          return of(null);
        }
        this.officeId.set(+id);
        return forkJoin({
          office: this.officeApi.getOfficeById(+id),
          cities: this.cityApi.getAllCities({ page: 0, size: 500 }),
          companies: this.companyApi.getAllCompanies({ page: 0, size: 100 })
        });
      })
    ).subscribe(result => {
      if (result) {
        this.cities.set(result.cities.content || []);
        this.companies.set(result.companies.content || []);
        this.populateForm(result.office);
        this.isLoading.set(false);
      }
    });
  }

  private populateForm(office: OfficeViewDto) {
    const city = this.cities().find(c => c.name === office.cityName && c.postcode === office.cityPostcode);

    this.officeForm.patchValue({
      companyId: office.companyId,
      address: {
        cityId: city?.id,
        street: office.fullAddress?.split(',')[0]
      }
    });
    this.operatingHours.clear();
    office.operatingHours?.forEach(hour => this.addOperatingHour(hour));
  }

  get operatingHours() {
    return this.officeForm.get('operatingHours') as FormArray;
  }

  addOperatingHour(hour?: any) {
    const hourForm = this.fb.group({
      dayOfWeek: [hour?.dayOfWeek || 'MONDAY', Validators.required],
      openTime: [hour?.openTime || '09:00', Validators.required],
      closeTime: [hour?.closeTime || '18:00', Validators.required],
      isClosed: [hour?.isClosed || false]
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
      } as any,
      operatingHours: new Set(formValue.operatingHours as OperatingHourDto[])
    };

    this.officeApi.updateOffice(this.officeId()!, payload).pipe(
      tap(() => {
        this.router.navigate(['/app/admin/offices']);
      }),
      catchError(err => {
        this.errorMessage.set(err.error?.detail || 'Failed to update office.');
        this.isSubmitting.set(false);
        return of(null);
      })
    ).subscribe();
  }
}
