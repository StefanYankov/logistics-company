import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AuthenticatedHeader} from './authenticated-header';
import {AuthService} from '../../auth.service';
import {RouterTestingModule} from '@angular/router/testing';

describe('AuthenticatedHeader', () => {
  let component: AuthenticatedHeader;
  let fixture: ComponentFixture<AuthenticatedHeader>;
  let mockAuthService: any;

  beforeEach(async () => {
    mockAuthService = {
      isLoggedIn: vi.fn(),
      hasRole: vi.fn(),
      logout: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AuthenticatedHeader, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: mockAuthService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuthenticatedHeader);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display client links if user is a client', () => {
    mockAuthService.isLoggedIn.mockReturnValue(true);
    mockAuthService.hasRole.mockImplementation((role: string) => role === 'ROLE_CLIENT');

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('a[routerLink="/app"]')).not.toBeNull();
    expect(compiled.querySelector('a[routerLink="/app/send-package"]')).not.toBeNull();
    expect(compiled.querySelector('.btn-logout')).not.toBeNull();
  });

  it('should display staff links if user is a clerk', () => {
    mockAuthService.isLoggedIn.mockReturnValue(true);
    mockAuthService.hasRole.mockImplementation((role: string) => role === 'ROLE_CLERK');

    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('a[routerLink="/app/shipments"]')).not.toBeNull();
    expect(compiled.querySelector('a[routerLink="/app/register-shipment"]')).not.toBeNull();
    expect(compiled.querySelector('.btn-logout')).not.toBeNull();
  });

  it('should call authService.logout when logout button is clicked', () => {
    mockAuthService.isLoggedIn.mockReturnValue(true);
    fixture.detectChanges();

    const logoutButton = fixture.nativeElement.querySelector('.btn-logout');
    logoutButton.click();

    expect(mockAuthService.logout).toHaveBeenCalled();
  });
});
