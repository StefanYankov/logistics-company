import {ComponentFixture, TestBed} from '@angular/core/testing';
import {OfficeCreate} from './office-create';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter, Router} from '@angular/router';
import {of} from 'rxjs';
import {CityAPIService, CompanyAPIService, OfficeAPIService} from '../../../api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

describe('OfficeCreate', () => {
  let component: OfficeCreate;
  let fixture: ComponentFixture<OfficeCreate>;
  let mockOfficeApi: any;
  let router: Router;

  beforeEach(async () => {
    mockOfficeApi = {
      createOffice: vi.fn().mockReturnValue(of({}))
    };

    await TestBed.configureTestingModule({
      imports: [OfficeCreate, ReactiveFormsModule, FormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        { provide: CityAPIService, useValue: { getAllCities: vi.fn().mockReturnValue(of({ content: [] })) } },
        { provide: CompanyAPIService, useValue: { getAllCompanies: vi.fn().mockReturnValue(of({ content: [] })) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OfficeCreate);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should submit new office data', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.officeForm.patchValue({
      companyId: 1,
      address: {
        cityId: 1,
        street: 'Test Street'
      }
    });
    fixture.detectChanges();

    component.onSubmit();

    expect(mockOfficeApi.createOffice).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/app/admin/offices']);
  });
});
