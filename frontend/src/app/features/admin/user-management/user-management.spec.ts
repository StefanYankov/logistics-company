import {ComponentFixture, TestBed} from '@angular/core/testing';
import {UserManagement} from './user-management';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {of} from 'rxjs';
import {ClientAPIService, EmployeeManagementAPIService, EmployeeViewDto} from '../../../api';
import {provideRouter} from '@angular/router';

describe('UserManagement', () => {
  let component: UserManagement;
  let fixture: ComponentFixture<UserManagement>;
  let mockEmployeeApi: any;
  let mockClientApi: any;

  const mockEmployee: EmployeeViewDto = {
    id: 'emp-1',
    firstName: 'Test',
    lastName: 'Employee',
    isActive: true
  };

  beforeEach(async () => {
    mockEmployeeApi = {
      getAllEmployees: vi.fn().mockReturnValue(of({ content: [mockEmployee] })),
      deactivateEmployee: vi.fn().mockReturnValue(of(null)),
      activateEmployee: vi.fn().mockReturnValue(of(null)),
      forcePasswordReset: vi.fn().mockReturnValue(of(null))
    };
    mockClientApi = {
      getAllClients: vi.fn().mockReturnValue(of({ content: [] }))
    };

    await TestBed.configureTestingModule({
      imports: [UserManagement],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: EmployeeManagementAPIService, useValue: mockEmployeeApi },
        { provide: ClientAPIService, useValue: mockClientApi }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagement);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load users', () => {
    expect(component).toBeTruthy();
    expect(mockEmployeeApi.getAllEmployees).toHaveBeenCalled();
    expect(mockClientApi.getAllClients).toHaveBeenCalled();
    expect(component.employees().length).toBe(1);
  });

  it('should call forcePasswordReset when admin provides a valid new password', () => {
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('newSecurePassword123');

    component.openResetPasswordModal(mockEmployee);

    expect(promptSpy).toHaveBeenCalled();
    expect(mockEmployeeApi.forcePasswordReset).toHaveBeenCalledWith('emp-1', { newPassword: 'newSecurePassword123' });
  });

  it('should not call forcePasswordReset if password is too short', () => {
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('short');
    const alertSpy = vi.spyOn(window, 'alert');

    component.openResetPasswordModal(mockEmployee);

    expect(promptSpy).toHaveBeenCalled();
    expect(alertSpy).toHaveBeenCalledWith('Password must be at least 8 characters long.');
    expect(mockEmployeeApi.forcePasswordReset).not.toHaveBeenCalled();
  });
});
