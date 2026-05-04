import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let mockRouter: any;

  beforeEach(() => {
    // 1. Create a simple mock object for the Router using vi.fn()
    mockRouter = {
      navigate: vi.fn()
    };

    // 2. Configure the testing module
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(), // Mocks the HttpClient
        { provide: Router, useValue: mockRouter }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // 3. Clear localStorage before each test for state isolation
    localStorage.clear();
  });

  afterEach(() => {
    // 4. Verify that no unmatched HTTP requests are outstanding
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should store JWT on successful login', () => {
    const mockCredentials = { username: 'admin', password: 'password' };
    const mockResponse = { token: 'mock-jwt-token' };

    service.login(mockCredentials).subscribe();

    // Intercept the outgoing request
    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    // Simulate a successful server response
    req.flush(mockResponse);

    expect(localStorage.getItem('logistics_jwt')).toBe('mock-jwt-token');
  });

  it('should clear JWT and navigate to login on logout', () => {
    localStorage.setItem('logistics_jwt', 'existing-token');

    service.logout();

    expect(localStorage.getItem('logistics_jwt')).toBeNull();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return true for isLoggedIn when token exists', () => {
    localStorage.setItem('logistics_jwt', 'some-token');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('should return false for isLoggedIn when token does not exist', () => {
    expect(service.isLoggedIn()).toBe(false);
  });
});
