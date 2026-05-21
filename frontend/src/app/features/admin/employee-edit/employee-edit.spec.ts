import {ComponentFixture, TestBed} from '@angular/core/testing';
import {EmployeeEdit} from './employee-edit';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {ActivatedRoute, provideRouter, Router} from '@angular/router';
import {of, throwError} from 'rxjs';
import {EmployeeManagementAPIService, EmployeeViewDto, OfficeAPIService} from '../../../api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

describe('EmployeeEdit', () => {
  let component: EmployeeEdit;
  let fixture: ComponentFixture<EmployeeEdit>;
  let mockEmployeeApi: any;
  let mockOfficeApi: any;
  let router: Router;

  const mockEmployee: EmployeeViewDto = {
    id: 'emp-1',
    username: 'testuser',
    email: 'test@user.com',
    firstName: 'Test',
    lastName: 'User',
    employeeNumber: 'EMP-123',
    hireDate: '2024-01-01',
    salary: 50000,
    applicationRole: 'CLERK',
    isActive: true,
    isEmailVerified: true,
    officeId: 1
  };

  beforeEach(async () => {
    mockEmployeeApi = {
      getEmployeeById: vi.fn().mockReturnValue(of(mockEmployee)),
      updateEmployeeBasicInfo: vi.fn().mockReturnValue(of(mockEmployee))
    };
    mockOfficeApi = {
      getAllOffices: vi.fn().mockReturnValue(of({ content: [] }))
    };

    await TestBed.configureTestingModule({
      imports: [EmployeeEdit, ReactiveFormsModule, FormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'app', children: [] }]),
        { provide: EmployeeManagementAPIService, useValue: mockEmployeeApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'emp-1' } },
            paramMap: of({ get: () => 'emp-1' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeEdit);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create and load employee data', () => {
    expect(component).toBeTruthy();
    expect(mockEmployeeApi.getEmployeeById).toHaveBeenCalledWith('emp-1');
    expect(component.isLoading()).toBe(false);
    expect(component.editForm.get('firstName')?.value).toBe(mockEmployee.firstName);
  });

  it('should submit updated employee data', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.editForm.patchValue({
      firstName: 'Updated',
      salary: 60000
    });

    fixture.detectChanges();
    component.onSubmit();

    expect(mockEmployeeApi.updateEmployeeBasicInfo).toHaveBeenCalledWith('emp-1', {
      email: mockEmployee.email,
      firstName: 'Updated',
      lastName: mockEmployee.lastName,
      salary: 60000,
      officeId: mockEmployee.officeId
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/app/admin/user-management']);
  });

  it('should handle error during employee update', () => {
    mockEmployeeApi.updateEmployeeBasicInfo.mockReturnValue(throwError(() => ({ status: 400, error: { detail: 'Validation Error' } })));

    component.editForm.patchValue({ firstName: 'Updated' });

    fixture.detectChanges();
    component.onSubmit();

    expect(component.isSubmitting()).toBe(false);
    expect(component.errorMessage()).toBe('Validation Error');
  });
});
