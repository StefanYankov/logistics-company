import {ComponentFixture, TestBed} from '@angular/core/testing';
import {CompanyDetails} from './company-details';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter, Router} from '@angular/router';
import {of, throwError} from 'rxjs';
import {CityAPIService, CityViewDto, CompanyAPIService, CompanyUpdateDto, CompanyViewDto} from '../../../api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

describe('CompanyDetails', () => {
  let component: CompanyDetails;
  let fixture: ComponentFixture<CompanyDetails>;
  let mockCompanyApi: any;
  let mockCityApi: any;
  let router: Router;

  const mockCompany: CompanyViewDto = {
    id: 1,
    name: 'LogisticsCo',
    registrationNumber: 'REG123',
    address: '123 Main St, Sofia 1000'
  };

  const mockCompanyUpdateDto: CompanyUpdateDto = {
    name: 'LogisticsCo',
    registrationNumber: 'REG123',
    addressDetails: {
      street: '123 Main St',
      cityId: 1
    }
  };

  const mockCities: CityViewDto[] = [
    { id: 1, name: 'Sofia', postcode: '1000' },
    { id: 2, name: 'Plovdiv', postcode: '4000' }
  ];

  beforeEach(async () => {
    mockCompanyApi = {
      getAllCompanies: vi.fn().mockReturnValue(of({ content: [mockCompany] })),
      getCompanyForUpdate: vi.fn().mockReturnValue(of(mockCompanyUpdateDto)),
      updateCompany: vi.fn().mockReturnValue(of(mockCompany))
    };
    mockCityApi = {
      getAllCities: vi.fn().mockReturnValue(of({ content: mockCities }))
    };

    await TestBed.configureTestingModule({
      imports: [CompanyDetails, ReactiveFormsModule, FormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: CompanyAPIService, useValue: mockCompanyApi },
        { provide: CityAPIService, useValue: mockCityApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompanyDetails);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create and load company data in view mode', () => {
    expect(component).toBeTruthy();
    expect(mockCompanyApi.getAllCompanies).toHaveBeenCalled();
    expect(mockCityApi.getAllCities).toHaveBeenCalled();
    expect(component.isLoading()).toBe(false);
    expect(component.company()?.name).toBe('LogisticsCo');
    expect(component.isEditMode()).toBe(false);
  });

  it('should switch to edit mode and populate form', () => {
    component.toggleEditMode(true);
    fixture.detectChanges();

    expect(mockCompanyApi.getCompanyForUpdate).toHaveBeenCalledWith(1);
    expect(component.isEditMode()).toBe(true);
    expect(component.companyForm.get('name')?.value).toBe('LogisticsCo');
    expect(component.companyForm.get('addressDetails.cityId')?.value).toBe(1);
  });

  it('should submit updated company data', () => {
    component.toggleEditMode(true);
    fixture.detectChanges();

    component.companyForm.patchValue({
      name: 'New LogisticsCo',
      registrationNumber: 'NEWREG123',
      addressDetails: {
        street: 'New Street',
        cityId: 2
      }
    });
    fixture.detectChanges();

    component.onSubmit();

    expect(mockCompanyApi.updateCompany).toHaveBeenCalledWith(1, {
      name: 'New LogisticsCo',
      registrationNumber: 'NEWREG123',
      addressDetails: {
        street: 'New Street',
        cityId: 2
      }
    });
    expect(component.isSubmitting()).toBe(false);
    expect(component.isEditMode()).toBe(false);
  });

  it('should handle error during company update', () => {
    mockCompanyApi.updateCompany.mockReturnValue(throwError(() => ({ status: 400, error: { detail: 'Validation Error' } })));

    component.toggleEditMode(true);
    fixture.detectChanges();

    component.companyForm.patchValue({
      name: 'New LogisticsCo',
      registrationNumber: 'NEWREG123',
      addressDetails: {
        street: 'New Street',
        cityId: 2
      }
    });
    fixture.detectChanges();

    component.onSubmit();

    expect(component.isSubmitting()).toBe(false);
    expect(component.errorMessage()).toBe('Validation Error');
  });

  it('should display message if no company data is found', () => {
    // Arrange
    mockCompanyApi.getAllCompanies.mockReturnValue(of({ content: [] }));
    component.company.set(null);

    // Act
    component.ngOnInit();
    fixture.detectChanges();

    // Assert
    expect(component.company()).toBeNull();
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.info-message')).toBeTruthy();
  });
});
