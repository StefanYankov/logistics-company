import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Register } from './register';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { BASE_PATH } from '../../../api';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BASE_PATH, useValue: '' }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate empty form', () => {
    expect(component.registerForm.valid).toBe(false);
  });

  it('should validate form with correct data', () => {
    component.registerForm.patchValue({
      username: 'testuser',
      email: 'test@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe',
      phoneNumber: '0888123456'
    });
    expect(component.registerForm.valid).toBe(true);
  });

  it('should make HTTP POST request on valid submit', () => {
    component.registerForm.patchValue({
      username: 'testuser',
      email: 'test@example.com',
      password: 'password123',
      firstName: 'John',
      lastName: 'Doe',
      phoneNumber: '0888123456'
    });

    component.onSubmit();

    const req = httpMock.expectOne('/api/clients/register');
    expect(req.request.method).toBe('POST');

    // Return a standard JS object. The generator's default config handles JSON properly in recent versions.
    req.flush({ id: 'some-uuid' });

    expect(component.isSuccess).toBe(true);
    expect(component.isLoading).toBe(false);
  });
});
