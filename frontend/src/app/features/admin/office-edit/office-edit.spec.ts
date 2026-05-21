import {ComponentFixture, TestBed} from '@angular/core/testing';
import {OfficeEdit} from './office-edit';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {ActivatedRoute, provideRouter, Router} from '@angular/router';
import {of} from 'rxjs';
import {
  CityAPIService,
  CityViewDto,
  CompanyAPIService,
  OfficeAPIService,
  OfficeViewDto,
  OperatingHourDto
} from '../../../api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

describe('OfficeEdit', () => {
  let component: OfficeEdit;
  let fixture: ComponentFixture<OfficeEdit>;
  let mockOfficeApi: any;
  let router: Router;

  const mockOffice: OfficeViewDto = {
    id: 1,
    companyId: 1,
    cityName: 'Sofia',
    cityPostcode: '1000',
    fullAddress: 'Test Street, Sofia, 1000',
    operatingHours: new Set<OperatingHourDto>()
  };

  const mockCities: CityViewDto[] = [
    { id: 1, name: 'Sofia', postcode: '1000' }
  ];

  beforeEach(async () => {
    mockOfficeApi = {
      getOfficeById: vi.fn().mockReturnValue(of(mockOffice)),
      updateOffice: vi.fn().mockReturnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [OfficeEdit, ReactiveFormsModule, FormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: { getAllCities: vi.fn().mockReturnValue(of({ content: mockCities })) } },
        { provide: CompanyAPIService, useValue: { getAllCompanies: vi.fn().mockReturnValue(of({ content: [] })) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => '1' } },
            paramMap: of({ get: () => '1' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OfficeEdit);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create and load office data', () => {
    expect(component).toBeTruthy();
    expect(mockOfficeApi.getOfficeById).toHaveBeenCalledWith(1);
    expect(component.officeForm.get('address.street')?.value).toBe('Test Street');
  });

  it('should submit updated office data', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    // Ensure form is valid before submitting
    component.officeForm.patchValue({
      companyId: 1,
      address: {
        cityId: 1,
        street: 'New Street'
      }
    });
    fixture.detectChanges();

    component.onSubmit();

    expect(mockOfficeApi.updateOffice).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/app/admin/offices']);
  });
});
