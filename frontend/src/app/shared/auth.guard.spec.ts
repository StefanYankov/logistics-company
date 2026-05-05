import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let mockAuthService: any;
  let mockRouter: any;

  beforeEach(() => {
    // 1. Create spies for dependencies using vi.fn() instead of jasmine.createSpy()
    mockAuthService = {
      isLoggedIn: vi.fn()
    };
    mockRouter = {
      createUrlTree: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter }
      ]
    });
  });

  it('should allow navigation if user is logged in', () => {
    // Mock the service to simulate a logged-in user
    mockAuthService.isLoggedIn.mockReturnValue(true);

    // Call the guard with dummy Route and State objects
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(result).toBe(true);
  });

  it('should redirect to /login if user is not logged in', () => {
    // Mock the service to simulate a logged-out user
    mockAuthService.isLoggedIn.mockReturnValue(false);

    // Create a fake UrlTree to return from the router mock
    const fakeUrlTree: any = {};
    mockRouter.createUrlTree.mockReturnValue(fakeUrlTree);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(result).toBe(fakeUrlTree);
    expect(mockRouter.createUrlTree).toHaveBeenCalledWith(['/login']);
  });
});
