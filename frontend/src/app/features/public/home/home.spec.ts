import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Home } from './home';
import { AuthService } from '../../../shared/auth.service';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let mockAuthService: any;
  let router: Router;

  beforeEach(async () => {
    mockAuthService = {
      isLoggedIn: vi.fn().mockReturnValue(false),
      getDecodedToken: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockImplementation(async () => true);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect to /app if logged in as CLIENT', () => {
    mockAuthService.isLoggedIn.mockReturnValue(true);
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLIENT' });

    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/app']);
  });

  it('should redirect to /app/shipments if logged in as staff', () => {
    mockAuthService.isLoggedIn.mockReturnValue(true);
    mockAuthService.getDecodedToken.mockReturnValue({ role: 'ROLE_CLERK' });

    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/app/shipments']);
  });

  it('should stay on public home page if not authenticated', () => {
    mockAuthService.isLoggedIn.mockReturnValue(false);

    fixture.detectChanges();

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
