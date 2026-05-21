import {Component, inject, OnInit, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {CityAPIService, CityViewDto, CompanyAPIService, CompanyUpdateDto, CompanyViewDto} from '../../../api';
import {forkJoin, of} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';

@Component({
  selector: 'app-company-details',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './company-details.html',
  styleUrl: './company-details.css',
})
export class CompanyDetails implements OnInit {
  private companyApi = inject(CompanyAPIService);
  private cityApi = inject(CityAPIService);
  private fb = inject(FormBuilder);

  company = signal<CompanyViewDto | null>(null);
  cities = signal<CityViewDto[]>([]);
  isLoading = signal(true);
  isEditMode = signal(false);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  companyForm = this.fb.group({
    name: ['', Validators.required],
    registrationNumber: ['', Validators.required],
    addressDetails: this.fb.group({
      street: ['', Validators.required],
      cityId: [null as number | null, Validators.required]
    })
  });

  ngOnInit() {
    this.loadInitialData();
  }

  public loadInitialData() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    forkJoin({
      companyPage: this.companyApi.getAllCompanies({ page: 0, size: 1 }),
      cities: this.cityApi.getAllCities({ page: 0, size: 500 })
    }).pipe(
      tap(result => {
        if (result.companyPage.content && result.companyPage.content.length > 0) {
          this.company.set(result.companyPage.content[0]);
        }
        this.cities.set(result.cities.content || []);
        this.isLoading.set(false);
      }),
      catchError(_err => {
        this.errorMessage.set('Failed to load company data.');
        this.isLoading.set(false);
        return of(null);
      })
    ).subscribe();
  }

  private populateForm(updateDto: CompanyUpdateDto) {
    this.companyForm.patchValue({
      name: updateDto.name,
      registrationNumber: updateDto.registrationNumber,
      addressDetails: {
        street: updateDto.addressDetails?.street,
        cityId: updateDto.addressDetails?.cityId
      }
    });
  }

  toggleEditMode(edit: boolean) {
    if (edit) {
      const companyId = this.company()?.id;
      if (!companyId) return;

      this.companyApi.getCompanyForUpdate(companyId).subscribe(updateDto => {
        this.populateForm(updateDto);
        this.isEditMode.set(true);
      });
    } else {
      this.isEditMode.set(false);
    }
  }

  onSubmit() {
    if (this.companyForm.invalid) {
      this.companyForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const companyId = this.company()?.id;
    if (!companyId) {
      this.errorMessage.set("Cannot update: Company ID is missing.");
      this.isSubmitting.set(false);
      return;
    }

    const payload = this.companyForm.value as CompanyUpdateDto;

    this.companyApi.updateCompany(companyId, payload).subscribe({
      next: (updatedCompany) => {
        this.company.set(updatedCompany);
        this.isSubmitting.set(false);
        this.isEditMode.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.detail || 'Failed to update company.');
        this.isSubmitting.set(false);
      }
    });
  }
}
