import {ComponentFixture, TestBed} from '@angular/core/testing';
import {EmployeeCreate} from './employee-create';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {provideRouter, Router} from '@angular/router';
import {of, throwError} from 'rxjs';
import {EmployeeManagementAPIService, OfficeAPIService} from '../../../api';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

describe('EmployeeCreate', () => {
  let component: EmployeeCreate;
  let fixture: ComponentFixture<EmployeeCreate>;
  let mockEmployeeApi: any;
  let mockOfficeApi: any;
  let router: Router;

  beforeEach(async () => {
    mockEmployeeApi = {
      createEmployee: vi.fn().mockReturnValue(of({}))
    };
    mockOfficeApi = {
      getAllOffices: vi.fn().mockReturnValue(of({ content: [] }))
    };

    await TestBed.configureTestingModule({
      imports: [EmployeeCreate, ReactiveFormsModule, FormsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'app', children: [] }]),
        { provide: EmployeeManagementAPIService, useValue: mockEmployeeApi },
        { provide: OfficeAPIService, useValue: mockOfficeApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeCreate);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should submit new employee data', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.createForm.patchValue({
      username: 'newuser',
      email: 'new@user.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      hireDate: '2024-01-01',
      salary: 50000,
      applicationRole: 'COURIER'
    });

    fixture.detectChanges();
    component.onSubmit();

    expect(mockEmployeeApi.createEmployee).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/app/admin/user-management']);
  });

  it('should handle error during employee creation', () => {
    mockEmployeeApi.createEmployee.mockReturnValue(throwError(() => ({ status: 409, error: { detail: 'Duplicate user' } })));

    component.createForm.patchValue({
      username: 'newuser',
      email: 'new@user.com',
      password: 'password123',
      firstName: 'New',
      lastName: 'User',
      hireDate: '2024-01-01',
      salary: 50000,
      applicationRole: 'COURIER'
    });

    fixture.detectChanges();
    component.onSubmit();

    expect(component.isSubmitting()).toBe(false);
    expect(component.errorMessage()).toBe('Duplicate user');
  });
});
